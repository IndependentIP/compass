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

package org.compass.core.lucene.engine;

import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Explanation;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.engine.SearchEngineHits;

/**
 * A Lucene specific extension to search engine hits. Allows to
 * get and execute Lucene specific operations.
 *
 * @author kimchy
 */
public interface LuceneSearchEngineHits extends SearchEngineHits {

    /**
     * Returns the actual Lucene hits.
     */
    Hits getHits();

    /**
     * Returns Lucene Explanation for hit number i.
     */
    Explanation explain(int i) throws SearchEngineException;
}
