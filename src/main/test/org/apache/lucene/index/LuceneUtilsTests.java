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

import junit.framework.TestCase;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * @author kimchy
 */
public class LuceneUtilsTests extends TestCase {

    private String indexFilePath = "target/test-index";

    private Directory directory;

    protected void setUp() throws Exception {
        File indexFile = new File(indexFilePath);
        org.compass.core.lucene.util.LuceneUtils.deleteDir(indexFile);
        directory = FSDirectory.getDirectory(indexFile, true);
    }

    protected void tearDown() throws Exception {
        File indexFile = new File(indexFilePath);
        org.compass.core.lucene.util.LuceneUtils.deleteDir(indexFile);
    }

    public void testIsCompound() throws IOException {
        assertTrue(LuceneUtils.isCompound(directory));
        createIndex();
        assertTrue(LuceneUtils.isCompound(directory));
        addDocument(true);
        assertTrue(LuceneUtils.isCompound(directory));
    }

    public void testIsCompound2() throws IOException {
        assertTrue(LuceneUtils.isCompound(directory));
        createIndex();
        assertTrue(LuceneUtils.isCompound(directory));
        addDocument(false);
        assertFalse(LuceneUtils.isCompound(directory));
    }

    public void testIsUnCompound() throws IOException {
        assertTrue(LuceneUtils.isUnCompound(directory));
        createIndex();
        assertTrue(LuceneUtils.isUnCompound(directory));
        addDocument(false);
        assertTrue(LuceneUtils.isUnCompound(directory));
    }

    public void testIsUnCompound2() throws IOException {
        assertTrue(LuceneUtils.isUnCompound(directory));
        createIndex();
        assertTrue(LuceneUtils.isUnCompound(directory));
        addDocument(true);
        assertFalse(LuceneUtils.isUnCompound(directory));
    }

    public void testCompoundDirectory() throws IOException {
        createIndex();

        addDocument(false);
        assertDocumentExists();
        assertFalse(LuceneUtils.isCompound(directory));
        assertTrue(LuceneUtils.isUnCompound(directory));

        LuceneUtils.compoundDirectory(directory, 1000);
        assertDocumentExists();
        assertTrue(LuceneUtils.isCompound(directory));
        assertFalse(LuceneUtils.isUnCompound(directory));
    }

    public void testUnCompoundDirectory() throws IOException {
        createIndex();

        addDocument(true);
        assertDocumentExists();
        assertTrue(LuceneUtils.isCompound(directory));
        assertFalse(LuceneUtils.isUnCompound(directory));

        LuceneUtils.unCompoundDirectory(directory, 1000);
        assertDocumentExists();
        assertTrue(LuceneUtils.isUnCompound(directory));
    }

    private void createIndex() throws IOException {
        IndexWriter indexWriter = new IndexWriter(directory, new SimpleAnalyzer(), true);
        indexWriter.close();
    }

    private void addDocument(boolean useCompound) throws IOException {
        IndexWriter indexWriter = new IndexWriter(directory, new SimpleAnalyzer(), false);
        indexWriter.setUseCompoundFile(useCompound);
        Document doc = new Document();
        doc.add(new Field("test", "test", Field.Store.YES, Field.Index.TOKENIZED));
        indexWriter.addDocument(doc);
        indexWriter.close();
    }

    private void assertDocumentExists() throws IOException {
        IndexReader indexReader = IndexReader.open(directory);
        assertEquals(1, indexReader.numDocs());
        Document doc = indexReader.document(0);
        assertEquals("test", doc.get("test"));
        indexReader.close();
    }
}
