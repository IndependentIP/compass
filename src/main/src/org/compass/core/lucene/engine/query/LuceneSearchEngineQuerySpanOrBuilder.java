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

import java.util.ArrayList;

import org.compass.core.engine.SearchEngineQueryBuilder;
import org.compass.core.engine.SearchEngineQuery;
import org.compass.core.lucene.engine.LuceneSearchEngine;
import org.compass.core.lucene.engine.LuceneSearchEngineQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanOrQuery;

/**
 * @author kimchy
 */
public class LuceneSearchEngineQuerySpanOrBuilder implements SearchEngineQueryBuilder.SearchEngineQuerySpanOrBuilder {

    private LuceneSearchEngine searchEngine;

    private ArrayList queries = new ArrayList();

    public LuceneSearchEngineQuerySpanOrBuilder(LuceneSearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    public SearchEngineQueryBuilder.SearchEngineQuerySpanOrBuilder add(SearchEngineQuery.SearchEngineSpanQuery query) {
        queries.add(((LuceneSearchEngineQuery.LuceneSearchEngineSpanQuery) query).getQuery());
        return this;
    }

    public SearchEngineQuery.SearchEngineSpanQuery toQuery() {
        SpanQuery[] spanQueries = (SpanQuery[]) queries.toArray(new SpanQuery[queries.size()]);
        SpanOrQuery spanOrQuery = new SpanOrQuery(spanQueries);
        return new LuceneSearchEngineQuery.LuceneSearchEngineSpanQuery(searchEngine, spanOrQuery);
    }
}
