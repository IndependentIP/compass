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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.store.Directory;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.lucene.engine.LuceneSearchEngine;
import org.compass.core.lucene.engine.manager.LuceneSearchEngineIndexManager;
import org.compass.core.spi.InternalCompassSession;

/**
 * Provides information about the segments within a Lucene index and about the
 * lucene index itself.
 * <p/>
 * Can not be instantiated directly, but has a factory method (<code>getSegmentsInfos</code>)
 * which returns the index info for the given index path.
 * </p>
 *
 * @author kimchy
 */
public class LuceneSubIndexInfo {

    /**
     * A Lucene single segment information
     */
    public static class LuceneSegmentInfo {
        private String name;

        private int docCount;

        public LuceneSegmentInfo(String name, int docCount) {
            this.name = name;
            this.docCount = docCount;
        }

        /**
         * Returns the name of the segment
         */
        public String name() {
            return this.name;
        }

        /**
         * Returns the number of documents within the segment
         */
        public int docCount() {
            return docCount;
        }
    }

    private ArrayList segmentInfos;

    private long version;

    private String subIndex;

    protected LuceneSubIndexInfo(String subIndex, long version, ArrayList segmentInfos) {
        this.subIndex = subIndex;
        this.version = version;
        this.segmentInfos = segmentInfos;
    }

    /**
     * Returns the version of the index.
     *
     * @return The version of the index
     */
    public long version() {
        return this.version;
    }

    /**
     * Retruns the number of segments.
     *
     * @return The number of segments
     */
    public int size() {
        return segmentInfos.size();
    }

    /**
     * The segment info that maps to the given index.
     *
     * @param segmentIndex The segment index
     * @return The segment info structure
     */
    public LuceneSegmentInfo info(int segmentIndex) {
        return (LuceneSegmentInfo) segmentInfos.get(segmentIndex);
    }

    /**
     * The index parh of the given index.
     *
     * @return The index path
     */
    public String getSubIndex() {
        return this.subIndex;
    }

    /**
     * Returns low level Lucene index sub index information. Note, this method
     * can be called outside of a transactional context.
     *
     * @param subIndex The sub index to get the info for
     * @param session  The compass session that will be used for transactional support
     * @return The sub index info
     * @throws IOException Failed to read the segments from the directory
     */
    public static LuceneSubIndexInfo getIndexInfo(final String subIndex, final CompassSession session) throws IOException {
        LuceneSearchEngine searchEngine = (LuceneSearchEngine) ((InternalCompassSession) session).getSearchEngine();
        final LuceneSearchEngineIndexManager indexManager = (LuceneSearchEngineIndexManager) searchEngine.getSearchEngineFactory().getIndexManager();
        CompassTransaction tx = null;
        try {
            tx = session.beginTransaction();
            LuceneSubIndexInfo info = getIndexInfo(subIndex, indexManager);
            tx.commit();
            return info;
        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception e1) {
                    // do nothing
                }
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Returns low level Lucene index sub index information. Note, this method must
     * be called within a transactional context.
     *
     * @param subIndex     The sub index to get the info for
     * @param indexManager An index manager used to get compass index informations
     * @return The sub index info
     * @throws IOException Failed to read the segments from the directory
     */
    public static LuceneSubIndexInfo getIndexInfo(String subIndex, LuceneSearchEngineIndexManager indexManager)
            throws IOException {
        final Directory directory = indexManager.getStore().getDirectoryBySubIndex(subIndex, false);
        try {
            final SegmentInfos segmentInfos = new SegmentInfos();
            segmentInfos.read(directory);

            ArrayList segmentInfosList = new ArrayList();
            for (int i = 0; i < segmentInfos.size(); i++) {
                SegmentInfo segmentInfo = segmentInfos.info(i);
                LuceneSegmentInfo luceneSegmentInfo = new LuceneSegmentInfo(segmentInfo.name, segmentInfo.docCount);
                segmentInfosList.add(luceneSegmentInfo);
            }
            return new LuceneSubIndexInfo(subIndex, segmentInfos.getVersion(), segmentInfosList);
        } catch (FileNotFoundException e) {
            // the segments file was not found, return null.
            return null;
        }
    }
}
