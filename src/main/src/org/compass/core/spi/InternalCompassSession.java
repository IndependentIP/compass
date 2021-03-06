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

package org.compass.core.spi;

import java.io.Serializable;

import org.compass.core.CompassException;
import org.compass.core.CompassSession;
import org.compass.core.Resource;
import org.compass.core.cache.first.FirstLevelCache;
import org.compass.core.engine.SearchEngine;
import org.compass.core.mapping.CompassMapping;
import org.compass.core.marshall.MarshallingContext;
import org.compass.core.marshall.MarshallingStrategy;
import org.compass.core.metadata.CompassMetaData;

/**
 * 
 * @author kimchy
 * 
 */
public interface InternalCompassSession extends CompassSession {

    InternalCompass getCompass();
    
    SearchEngine getSearchEngine();

    MarshallingStrategy getMarshallingStrategy();

    FirstLevelCache getFirstLevelCache();

    Object get(String alias, Serializable id, MarshallingContext context) throws CompassException;

    Object getByResource(Resource resource) throws CompassException;

    Resource getResourceByIdResource(Resource idResource) throws CompassException;

    CompassMapping getMapping();

    CompassMetaData getMetaData();

    /**
     * Flushed the current transaction. Only supported for batch_insert (no op in others).
     */
    void flush() throws CompassException;
}
