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

package org.compass.core.lucene.engine.query;

import org.compass.core.engine.SearchEngineQueryBuilder;
import org.compass.core.engine.SearchEngineQuery;
import org.compass.core.lucene.engine.LuceneSearchEngine;
import org.compass.core.lucene.engine.LuceneSearchEngineQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;

/**
 * @author kimchy
 */
public class LuceneSearchEngineBooleanQueryBuilder implements SearchEngineQueryBuilder.SearchEngineBooleanQueryBuilder {

    private LuceneSearchEngine searchEngine;

    private BooleanQuery boolQuery;

    public LuceneSearchEngineBooleanQueryBuilder(LuceneSearchEngine searchEngine, boolean disableCoord) {
        this.searchEngine = searchEngine;
        boolQuery = new BooleanQuery(disableCoord);
    }

    public SearchEngineQueryBuilder.SearchEngineBooleanQueryBuilder addMust(SearchEngineQuery query) {
        boolQuery.add(((LuceneSearchEngineQuery) query).getQuery(), BooleanClause.Occur.MUST);
        return this;
    }

    public SearchEngineQueryBuilder.SearchEngineBooleanQueryBuilder addMustNot(SearchEngineQuery query) {
        boolQuery.add(((LuceneSearchEngineQuery) query).getQuery(), BooleanClause.Occur.MUST_NOT);
        return this;
    }

    public SearchEngineQueryBuilder.SearchEngineBooleanQueryBuilder addShould(SearchEngineQuery query) {
        boolQuery.add(((LuceneSearchEngineQuery) query).getQuery(), BooleanClause.Occur.SHOULD);
        return this;
    }

    public SearchEngineQuery toQuery() {
        return new LuceneSearchEngineQuery(searchEngine, boolQuery);
    }
}
