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
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.index.Term;

/**
 * @author kimchy
 */
public class LuceneSearchEngineMultiPhraseQueryBuilder implements SearchEngineQueryBuilder.SearchEngineMultiPhraseQueryBuilder {

    private LuceneSearchEngine searchEngine;

    private String resourceProperty;

    private MultiPhraseQuery multiPhraseQuery;

    public LuceneSearchEngineMultiPhraseQueryBuilder(LuceneSearchEngine searchEngine, String resourceProperty) {
        this.searchEngine = searchEngine;
        this.resourceProperty = resourceProperty;
        this.multiPhraseQuery = new MultiPhraseQuery();
    }

    public SearchEngineQueryBuilder.SearchEngineMultiPhraseQueryBuilder setSlop(int slop) {
        multiPhraseQuery.setSlop(slop);
        return this;
    }

    public SearchEngineQueryBuilder.SearchEngineMultiPhraseQueryBuilder add(String value) {
        multiPhraseQuery.add(new Term(resourceProperty, value));
        return this;
    }

    public SearchEngineQueryBuilder.SearchEngineMultiPhraseQueryBuilder add(String value, int position) {
        multiPhraseQuery.add(new Term[]{new Term(resourceProperty, value)}, position);
        return this;
    }

    public SearchEngineQueryBuilder.SearchEngineMultiPhraseQueryBuilder add(String[] values) {
        Term[] terms = new Term[values.length];
        for (int i = 0; i < values.length; i++) {
            terms[i] = new Term(resourceProperty, values[i]);
        }
        multiPhraseQuery.add(terms);
        return this;
    }

    public SearchEngineQueryBuilder.SearchEngineMultiPhraseQueryBuilder add(String[] values, int position) {
        Term[] terms = new Term[values.length];
        for (int i = 0; i < values.length; i++) {
            terms[i] = new Term(resourceProperty, values[i]);
        }
        multiPhraseQuery.add(terms, position);
        return this;
    }

    public SearchEngineQuery toQuery() {
        return new LuceneSearchEngineQuery(searchEngine, multiPhraseQuery);
    }
}
