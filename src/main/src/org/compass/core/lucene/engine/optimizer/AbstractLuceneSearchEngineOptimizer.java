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

package org.compass.core.lucene.engine.optimizer;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.LuceneSubIndexInfo;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.lucene.engine.LuceneSearchEngineFactory;
import org.compass.core.lucene.engine.manager.LuceneSearchEngineIndexManager;

/**
 * @author kimchy
 */
public abstract class AbstractLuceneSearchEngineOptimizer implements LuceneSearchEngineOptimizer {

    protected final Log log = LogFactory.getLog(getClass());

    private LuceneSearchEngineFactory searchEngineFactory;

    private boolean isRunning = false;

    public void start() throws SearchEngineException {
        if (isRunning) {
            throw new IllegalStateException("Optimizer is already running");
        }
        if (log.isDebugEnabled()) {
            log.debug("Starting Optimizer");
        }
        doStart();
        isRunning = true;
    }

    protected void doStart() throws SearchEngineException {

    }

    public void stop() throws SearchEngineException {
        if (!isRunning) {
            throw new IllegalStateException("Optimizer is is not running");
        }
        if (log.isDebugEnabled()) {
            log.debug("Stopping Optimizer");
        }
        doStop();
        isRunning = false;
    }

    protected void doStop() throws SearchEngineException {

    }

    public boolean isRunning() {
        return isRunning;
    }

    public void optimize() throws SearchEngineException {
        LuceneSearchEngineIndexManager indexManager = searchEngineFactory.getLuceneIndexManager();
        String[] subIndexes = indexManager.getStore().getSubIndexes();
        for (int i = 0; i < subIndexes.length; i++) {
            // here we go indirectly since it might be wrapped in a transaction
            searchEngineFactory.getOptimizer().optimize(subIndexes[i]);
        }
    }

    public void optimize(String subIndex) throws SearchEngineException {
        if (!isRunning()) {
            return;
        }
        LuceneSearchEngineIndexManager indexManager = searchEngineFactory.getLuceneIndexManager();
        LuceneSubIndexInfo indexInfo;
        try {
            indexInfo = LuceneSubIndexInfo.getIndexInfo(subIndex, indexManager);
        } catch (IOException e) {
            throw new SearchEngineException("Failed to read index info for sub index [" + subIndex + "]", e);
        }
        if (indexInfo == null) {
            // no index data, simply continue
            return;
        }
        boolean needOptimizing = doNeedOptimizing(subIndex, indexInfo);
        if (!isRunning()) {
            return;
        }
        if (needOptimizing) {
            doOptimize(subIndex, indexInfo);
            searchEngineFactory.getIndexManager().clearCache(subIndex);
        }
    }

    public boolean needOptimization() throws SearchEngineException {
        LuceneSearchEngineIndexManager indexManager = searchEngineFactory.getLuceneIndexManager();
        String[] subIndexes = indexManager.getStore().getSubIndexes();
        boolean needOptmization = false;
        for (int i = 0; i < subIndexes.length; i++) {
            // here we go indirectly since it might be wrapped in a transaction
            needOptmization |= searchEngineFactory.getOptimizer().needOptimization(subIndexes[i]);
        }
        return needOptmization;
    }

    public boolean needOptimization(String subIndex) throws SearchEngineException {
        LuceneSearchEngineIndexManager indexManager = searchEngineFactory.getLuceneIndexManager();
        if (!isRunning()) {
            return false;
        }
        LuceneSubIndexInfo indexInfo;
        try {
            indexInfo = LuceneSubIndexInfo.getIndexInfo(subIndex, indexManager);
        } catch (IOException e) {
            throw new SearchEngineException("Failed to read index info for sub index [" + subIndex + "]", e);
        }
        if (indexInfo == null) {
            // no index data, simply continue
            return false;
        }
        return doNeedOptimizing(subIndex, indexInfo);
    }

    protected abstract void doOptimize(String subIndex, LuceneSubIndexInfo indexInfo) throws SearchEngineException;

    protected abstract boolean doNeedOptimizing(String subIndex, LuceneSubIndexInfo indexInfo) throws SearchEngineException;

    public LuceneSearchEngineFactory getSearchEngineFactory() {
        return searchEngineFactory;
    }

    public void setSearchEngineFactory(LuceneSearchEngineFactory searchEngineFactory) {
        this.searchEngineFactory = searchEngineFactory;
    }

}
