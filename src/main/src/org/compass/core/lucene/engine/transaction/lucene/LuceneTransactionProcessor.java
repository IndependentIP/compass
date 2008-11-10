/*
 * Copyright 2004-2008 the original author or authors.
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

package org.compass.core.lucene.engine.transaction.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.lucene.LuceneResource;
import org.compass.core.lucene.engine.LuceneSearchEngine;
import org.compass.core.lucene.engine.transaction.support.AbstractSearchTransactionProcessor;
import org.compass.core.lucene.engine.transaction.support.ResourceEnhancer;
import org.compass.core.spi.InternalResource;
import org.compass.core.spi.ResourceKey;
import org.compass.core.transaction.context.TransactionalCallable;
import org.compass.core.util.StringUtils;

/**
 * Lucene based transaction, allows to perfom dirty operations direct over the index
 * using Lucene support for "transactions". Reads and search will be performed on the
 * index itself without taking into account the "transactional" operations.
 *
 * @author kimchy
 */
public class LuceneTransactionProcessor extends AbstractSearchTransactionProcessor {

    private static final Log log = LogFactory.getLog(LuceneTransactionProcessor.class);

    private Map<String, IndexWriter> indexWriterBySubIndex = new HashMap<String, IndexWriter>();

    public LuceneTransactionProcessor(LuceneSearchEngine searchEngine) {
        super(searchEngine);
    }

    public void begin() throws SearchEngineException {
        // nothing to do here
    }

    public void rollback() throws SearchEngineException {
        SearchEngineException exception = null;
        for (Map.Entry<String, IndexWriter> entry : indexWriterBySubIndex.entrySet()) {
            try {
                entry.getValue().rollback();
            } catch (AlreadyClosedException e) {
                if (log.isTraceEnabled()) {
                    log.trace("Failed to abort transaction for sub index [" + entry.getKey() + "] since it is alreayd closed");
                }
            } catch (IOException e) {
                Directory dir = indexManager.getStore().openDirectory(entry.getKey());
                try {
                    if (IndexWriter.isLocked(dir)) {
                        IndexWriter.unlock(dir);
                    }
                } catch (Exception e1) {
                    log.warn("Failed to check for locks or unlock failed commit for sub index [" + entry.getKey() + "]", e);
                }
                exception = new SearchEngineException("Failed to rollback transaction for sub index [" + entry.getKey() + "]", e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    public void prepare() throws SearchEngineException {
        if (indexManager.supportsConcurrentOperations()) {
            ArrayList<Callable<Object>> prepareCallables = new ArrayList<Callable<Object>>();
            for (Map.Entry<String, IndexWriter> entry : indexWriterBySubIndex.entrySet()) {
                prepareCallables.add(new TransactionalCallable(indexManager.getTransactionContext(), new PrepareCommitCallable(entry.getKey(), entry.getValue())));
            }
            indexManager.getExecutorManager().invokeAllWithLimitBailOnException(prepareCallables, 1);
        } else {
            for (Map.Entry<String, IndexWriter> entry : indexWriterBySubIndex.entrySet()) {
                try {
                    new PrepareCommitCallable(entry.getKey(), entry.getValue()).call();
                } catch (SearchEngineException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SearchEngineException("Failed to commit transaction for sub index [" + entry.getKey() + "]", e);
                }
            }
        }
    }

    public void commit(boolean onePhase) throws SearchEngineException {
        if (indexWriterBySubIndex.isEmpty()) {
            return;
        }
        // here, we issue doPrepare since if only one of the sub indexes failed with it, then
        // it should fail.
        if (onePhase) {
            prepare();
        }
        if (indexManager.supportsConcurrentOperations()) {
            ArrayList<Callable<Object>> prepareCallables = new ArrayList<Callable<Object>>();
            for (Map.Entry<String, IndexWriter> entry : indexWriterBySubIndex.entrySet()) {
                prepareCallables.add(new TransactionalCallable(indexManager.getTransactionContext(), new CommitCallable(entry.getKey(), entry.getValue())));
            }
            indexManager.getExecutorManager().invokeAllWithLimitBailOnException(prepareCallables, 1);
        } else {
            for (Map.Entry<String, IndexWriter> entry : indexWriterBySubIndex.entrySet()) {
                try {
                    new CommitCallable(entry.getKey(), entry.getValue()).call();
                } catch (SearchEngineException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SearchEngineException("Failed to commit transaction for sub index [" + entry.getKey() + "]", e);
                }
            }
        }
    }

    public void flush() throws SearchEngineException {
        // flush() deprecated in Lucene 2.4
//        if (indexWriterBySubIndex.isEmpty()) {
//            return;
//        }
//        ArrayList<Callable<Object>> prepareCallables = new ArrayList<Callable<Object>>();
//        for (Map.Entry<String, IndexWriter> entry : indexWriterBySubIndex.entrySet()) {
//            prepareCallables.add(new TransactionalCallable(indexManager.getTransactionContext(), new PrepareCommitCallable(entry.getKey(), entry.getValue())));
//        }
//        indexManager.getExecutorManager().invokeAllWithLimitBailOnException(prepareCallables, 1);
    }

    public void create(InternalResource resource) throws SearchEngineException {
        try {
            IndexWriter indexWriter = getOrCreateIndexWriter(resource.getSubIndex());
            Analyzer analyzer = ResourceEnhancer.enahanceResource(resource, searchEngine.getSearchEngineFactory());
            indexWriter.addDocument(((LuceneResource) resource).getDocument(), analyzer);
        } catch (IOException e) {
            throw new SearchEngineException("Failed to create resource for alias [" + resource.getAlias()
                    + "] and resource " + resource, e);
        }
    }

    public void delete(ResourceKey resourceKey) throws SearchEngineException {
        try {
            IndexWriter indexWriter = getOrCreateIndexWriter(resourceKey.getSubIndex());
            indexWriter.deleteDocuments(new Term(resourceKey.getUIDPath(), resourceKey.buildUID()));
        } catch (IOException e) {
            throw new SearchEngineException("Failed to delete alias [" + resourceKey.getAlias() + "] and ids ["
                    + StringUtils.arrayToCommaDelimitedString(resourceKey.getIds()) + "]", e);
        }
    }

    public void update(InternalResource resource) throws SearchEngineException {
        try {
            IndexWriter indexWriter = getOrCreateIndexWriter(resource.getSubIndex());
            Analyzer analyzer = ResourceEnhancer.enahanceResource(resource, searchEngine.getSearchEngineFactory());
            indexWriter.updateDocument(new Term(resource.getResourceKey().getUIDPath(), resource.getResourceKey().buildUID()), ((LuceneResource) resource).getDocument(), analyzer);
        } catch (IOException e) {
            throw new SearchEngineException("Failed to update resource for alias [" + resource.getAlias()
                    + "] and resource " + resource, e);
        }
    }

    protected IndexWriter getOrCreateIndexWriter(String subIndex) throws IOException {
        IndexWriter indexWriter = indexWriterBySubIndex.get(subIndex);
        if (indexWriter != null) {
            return indexWriter;
        }
        indexWriter = indexManager.openIndexWriter(searchEngine.getSettings(), subIndex, false);
        indexWriterBySubIndex.put(subIndex, indexWriter);
        return indexWriter;
    }

    private class PrepareCommitCallable implements Callable {

        private String subIndex;

        private IndexWriter indexWriter;

        private PrepareCommitCallable(String subIndex, IndexWriter indexWriter) {
            this.subIndex = subIndex;
            this.indexWriter = indexWriter;
        }

        public Object call() throws Exception {
            indexWriter.prepareCommit();
            return null;
        }
    }

    private class CommitCallable implements Callable {

        private String subIndex;

        private IndexWriter indexWriter;

        private CommitCallable(String subIndex, IndexWriter indexWriter) {
            this.subIndex = subIndex;
            this.indexWriter = indexWriter;
        }

        public Object call() throws Exception {
            try {
                indexWriter.commit();
                indexWriter.close();
            } catch (IOException e) {
                Directory dir = indexManager.getStore().openDirectory(subIndex);
                try {
                    if (IndexWriter.isLocked(dir)) {
                        IndexWriter.unlock(dir);
                    }
                } catch (Exception e1) {
                    log.warn("Failed to check for locks or unlock failed commit for sub index [" + subIndex + "]", e);
                }
                throw new SearchEngineException("Failed commit transaction sub index [" + subIndex + "]", e);
            }
            if (isClearCacheOnCommit()) {
                if (log.isTraceEnabled()) {
                    log.trace("Clearing cache after commit for sub index [" + subIndex + "]");
                }
                indexManager.clearCache(subIndex);
            }
            return null;
        }
    }
}
