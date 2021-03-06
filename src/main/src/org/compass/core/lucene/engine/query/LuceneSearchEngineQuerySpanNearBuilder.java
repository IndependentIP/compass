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
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.index.Term;

/**
 * @author kimchy
 */
public class LuceneSearchEngineQuerySpanNearBuilder implements SearchEngineQueryBuilder.SearchEngineQuerySpanNearBuilder {

    private LuceneSearchEngine searchEngine;

    private String resourceProperty;

    private int slop = 0;

    private boolean inOrder = true;

    private ArrayList values = new ArrayList();

    public LuceneSearchEngineQuerySpanNearBuilder(LuceneSearchEngine searchEngine, String resourceProperty) {
        this.searchEngine = searchEngine;
        this.resourceProperty = resourceProperty;
    }

    public SearchEngineQueryBuilder.SearchEngineQuerySpanNearBuilder setSlop(int slop) {
        this.slop = slop;
        return this;
    }

    public SearchEngineQueryBuilder.SearchEngineQuerySpanNearBuilder setInOrder(boolean inOrder) {
        this.inOrder = inOrder;
        return this;
    }

    public SearchEngineQueryBuilder.SearchEngineQuerySpanNearBuilder add(String value) {
        values.add(new SpanTermQuery(new Term(resourceProperty, value)));
        return this;
    }

    public SearchEngineQueryBuilder.SearchEngineQuerySpanNearBuilder add(SearchEngineQuery.SearchEngineSpanQuery query) {
        values.add(((LuceneSearchEngineQuery.LuceneSearchEngineSpanQuery) query).getQuery());
        return this;
    }

    public SearchEngineQuery.SearchEngineSpanQuery toQuery() {
        SpanQuery[] spanQueries = (SpanQuery[]) values.toArray(new SpanQuery[values.size()]);
        SpanNearQuery spanNearQuery = new SpanNearQuery(spanQueries, slop, inOrder);
        return new LuceneSearchEngineQuery.LuceneSearchEngineSpanQuery(searchEngine, spanNearQuery);
    }

}
