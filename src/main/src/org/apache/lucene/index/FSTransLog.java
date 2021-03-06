/*
 * Copyright 2004-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.index;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.lucene.store.BufferedIndexInput;
import org.apache.lucene.store.ConfigurableBufferedIndexOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.compass.core.CompassException;
import org.compass.core.config.CompassSettings;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.lucene.LuceneEnvironment;
import org.compass.core.lucene.util.LuceneUtils;

/**
 * <p>A transaction log that saves most of the transactional data log into disk.
 *
 * <p>Can be provided with a transaction log path (using
 * {@link org.compass.core.lucene.LuceneEnvironment.Transaction.TransLog#PATH} setting), where
 * different directories will be created in (in the form of PATH/compass/translog/[tran id]). All
 * the trans data is stored into a single file called <code>transdata</code>.
 *
 * <p>Two parameters can be set on this transaction log as well, the first is read buffer size,
 * which controls the size of the buffer for read operations. This can affect the memory consumption
 * of this transaction log, and defaults to 64 (bytes). The write buffer is the memory buffer used
 * to write data into the file system, and defaults to 2048 (bytes). It is pooled so does not affect
 * memory consumption that much.
 *
 * @author kimchy
 */
public class FSTransLog implements TransLog {

    private TransDirectory dir;

    // TODO need to find a better way to generate trans id
    private static Random transId = new Random();

    private static final String DEFAULT_LOCATION = System.getProperty("java.io.tmpdir") + "/compass/translog";

    public FSTransLog() {
    }

    public void configure(CompassSettings settings) throws CompassException {
        String location = settings.getSetting(LuceneEnvironment.Transaction.TransLog.PATH, DEFAULT_LOCATION);
        File fileLocation = new File(location);
        if (!fileLocation.exists()) {
            synchronized (FSTransLog.class) {
                fileLocation.mkdirs();
            }
        }
        int readBufferSize = settings.getSettingAsInt(LuceneEnvironment.Transaction.TransLog.READ_BUFFER_SIZE, 64);
        int writeBufferSize = settings.getSettingAsInt(LuceneEnvironment.Transaction.TransLog.WRITE_BUFFER_SIZE, 2048);
        try {
            location = location + "/" + transId.nextLong();
            dir = new TransDirectory(location, writeBufferSize, readBufferSize);
        } catch (IOException e) {
            throw new SearchEngineException("Failed to create tran log location [" + location + "]", e);
        }
    }

    public Directory getDirectory() {
        return this.dir;
    }

    public boolean shouldUpdateTransSegments() {
        return false;
    }

    public void close() throws IOException {
        dir.close();
        LuceneUtils.deleteDir(dir.getFile());
        dir = null;
    }

    public void onDocumentAdded() throws IOException {
        dir.flush();
    }

    /**
     * Transactional log directory implementation, writes all the data into memory until
     * {@link #flush()} is called (it is called in the {@link FSTransLog#onDocumentAdded()} callback).
     * Whe flush is called, flushes all the data into a single file, and performs all reads from this
     * single file. Stores in memory the meta data information regarding the file contents (file names
     * and positions).
     */
    class TransDirectory extends Directory {

        private RandomAccessFile raf;

        private HashMap files;

        private File dir;

        private ArrayList ramBasedFiles = new ArrayList();

        private WriteByteBufferPool bufferPool = new WriteByteBufferPool();

        private int writeBufferSize;

        private int readBufferSize;

        public TransDirectory(String path, int writeBufferSize, int readBufferSize) throws IOException {
            dir = new File(path);
            this.writeBufferSize = writeBufferSize;
            this.readBufferSize = readBufferSize;
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("Cannot create trans directory [" + dir.getAbsolutePath() + "]");
                }
            }
            raf = new RandomAccessFile(new File(dir, "tansdata"), "rw");
            files = new HashMap();
        }

        public File getFile() {
            return this.dir;
        }

        public String[] list() throws IOException {
            String[] list = new String[files.size()];
            int count = 0;
            for (Iterator it = files.values().iterator(); it.hasNext();) {
                FileEntry entry = (FileEntry) it.next();
                if (!entry.deleted) {
                    list[count++] = entry.name;
                }
            }
            return list;
        }

        public boolean fileExists(String name) throws IOException {
            return files.containsKey(name);
        }

        public long fileModified(String name) throws IOException {
            return ((FileEntry) files.get(name)).lastModified;
        }

        public void touchFile(String name) throws IOException {
            ((FileEntry) files.get(name)).lastModified = System.currentTimeMillis();
        }

        public void deleteFile(String name) throws IOException {
            ((FileEntry) files.get(name)).deleted = true;
        }

        public void renameFile(String from, String to) throws IOException {
            FileEntry fileEnty = (FileEntry) files.get(from);
            fileEnty.name = to;
            files.put(to, fileEnty);
        }

        public long fileLength(String name) throws IOException {
            return ((FileEntry) files.get(name)).length;
        }

        public IndexOutput createOutput(String name) throws IOException {
            FileEntry fileEntry = new FileEntry();
            fileEntry.name = name;
            files.put(name, fileEntry);
            ramBasedFiles.add(fileEntry);
            return new RamTransIndexOutput(fileEntry);
        }

        public IndexInput openInput(String name) throws IOException {
            FileEntry fileEntry = (FileEntry) files.get(name);
            return new TransIndexInput(raf, fileEntry);
        }

        public void flush() throws IOException {
            raf.seek(raf.length());
            for (int feIndex = 0; feIndex < ramBasedFiles.size(); feIndex++) {
                FileEntry fileEntry = (FileEntry) ramBasedFiles.get(feIndex);
                fileEntry.startPosition = raf.length();
                if (fileEntry.buffers.size() == 0) {
                    continue;
                }
                if (fileEntry.buffers.size() == 1) {
                    raf.write((byte[]) fileEntry.buffers.get(0), 0, (int) fileEntry.length);
                } else {
                    int tempSize = fileEntry.buffers.size() - 1;
                    int i;
                    for (i = 0; i < tempSize; i++) {
                        raf.write((byte[]) fileEntry.buffers.get(i), 0, writeBufferSize);
                    }
                    int leftOver = (int) (fileEntry.length % writeBufferSize);
                    if (leftOver == 0) {
                        raf.write((byte[]) fileEntry.buffers.get(i), 0, writeBufferSize);
                    } else {
                        raf.write((byte[]) fileEntry.buffers.get(i), 0, leftOver);
                    }
                }
                bufferPool.put(fileEntry.buffers);
                fileEntry.buffers = null;
            }
            ramBasedFiles.clear();
        }

        public Lock makeLock(String name) {
            throw new UnsupportedOperationException("Lokcing is not supported for trans directory");
        }

        public void close() throws IOException {
            raf.close();
        }

        /**
         * A file entry representation of a single file entry within the compound data
         * file.
         */
        class FileEntry {
            String name;
            long lastModified;
            boolean deleted;
            long startPosition;
            long length;
            ArrayList buffers;
        }

        /**
         * A very simple pool of write buffers used for write operations. If no buffer
         * is allocated int the pool, a new one is returned. If one is found, it is
         * removed from the pool and returned to the caller.
         */
        class WriteByteBufferPool {

            private LinkedList pool = new LinkedList();

            public byte[] get() {
                if (pool.size() == 0) {
                    return new byte[writeBufferSize];
                }
                return (byte[]) pool.removeFirst();
            }

            public void put(byte[] data) {
                pool.add(data);
            }

            public void put(List data) {
                pool.addAll(data);
            }
        }

        /**
         * A memory based index output that writes all the data into memory.
         * Can later be flushed by {@link org.apache.lucene.index.FSTransLog.TransDirectory#flush()}.
         */
        class RamTransIndexOutput extends ConfigurableBufferedIndexOutput {

            private long pointer = 0;

            private FileEntry fileEntry;

            public RamTransIndexOutput(FileEntry fileEntry) {
                this.fileEntry = fileEntry;
                this.fileEntry.buffers = new ArrayList();
                initBuffer(writeBufferSize);
            }

            public void flushBuffer(byte[] src, int offset, int len) {
                byte[] buffer;
                int bufferPos = offset;
                while (bufferPos != len) {
                    int bufferNumber = (int) (pointer / writeBufferSize);
                    int bufferOffset = (int) (pointer % writeBufferSize);
                    int bytesInBuffer = writeBufferSize - bufferOffset;
                    int remainInSrcBuffer = len - bufferPos;
                    int bytesToCopy = bytesInBuffer >= remainInSrcBuffer ? remainInSrcBuffer : bytesInBuffer;

                    if (bufferNumber == fileEntry.buffers.size()) {
                        buffer = bufferPool.get();
                        fileEntry.buffers.add(buffer);
                    } else {
                        buffer = (byte[]) fileEntry.buffers.get(bufferNumber);
                    }

                    System.arraycopy(src, bufferPos, buffer, bufferOffset, bytesToCopy);
                    bufferPos += bytesToCopy;
                    pointer += bytesToCopy;
                }

                if (pointer > fileEntry.length)
                    fileEntry.length = pointer;

                fileEntry.lastModified = System.currentTimeMillis();
            }

            public void close() throws IOException {
                super.close();
            }

            public void seek(long pos) throws IOException {
                super.seek(pos);
                pointer = pos;
            }

            public long length() {
                return fileEntry.length;
            }
        }

        /**
         * An index input implementation that can read a
         * {@link org.apache.lucene.index.FSTransLog.TransDirectory.FileEntry} information
         * from the compound transactional data file.
         */
        class TransIndexInput extends BufferedIndexInput {

            private RandomAccessFile raf;

            private FileEntry fileEntry;

            public TransIndexInput(RandomAccessFile raf, FileEntry fileEntry) throws IOException {
                super(readBufferSize);
                this.raf = raf;
                this.fileEntry = fileEntry;
            }

            protected void readInternal(byte[] b, int offset, int length) throws IOException {
                raf.seek(fileEntry.startPosition + getFilePointer());
                int total = 0;
                do {
                    int i = raf.read(b, offset + total, length - total);
                    if (i == -1)
                        throw new IOException("Read past EOF [" + fileEntry.name + "] SIZE [" + fileEntry.length + "]");
                    total += i;
                } while (total < length);
            }

            protected void seekInternal(long pos) throws IOException {
            }

            public void close() throws IOException {
            }

            public long length() {
                return fileEntry.length;
            }
        }
    }
}
