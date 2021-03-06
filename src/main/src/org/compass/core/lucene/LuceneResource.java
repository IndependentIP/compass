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

package org.compass.core.lucene;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.converter.ResourcePropertyConverter;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.lucene.engine.LuceneSearchEngine;
import org.compass.core.lucene.util.LuceneUtils;
import org.compass.core.mapping.ResourceMapping;
import org.compass.core.mapping.ResourcePropertyMapping;
import org.compass.core.spi.AliasedObject;
import org.compass.core.spi.InternalResource;
import org.compass.core.spi.ResourceKey;
import org.compass.core.util.StringUtils;

/**
 * @author kimchy
 */
public class LuceneResource implements AliasedObject, InternalResource, Map {

    private static final long serialVersionUID = 3904681565727306034L;

    private Document document;

    private ArrayList properties = new ArrayList();

    private String aliasProperty;

    private int docNum;

    private transient LuceneSearchEngine searchEngine;

    private transient ResourceMapping resourceMapping;

    private transient ResourceKey resourceKey;

    public LuceneResource(String alias, LuceneSearchEngine searchEngine) {
        this(alias, new Document(), -1, searchEngine);
    }

    public LuceneResource(Document document, int docNum, LuceneSearchEngine searchEngine) {
        this(null, document, docNum, searchEngine);
    }

    public LuceneResource(String alias, Document document, int docNum, LuceneSearchEngine searchEngine) {
        this.document = document;
        this.searchEngine = searchEngine;
        this.aliasProperty = searchEngine.getSearchEngineFactory().getAliasProperty();
        this.docNum = docNum;
        if (alias != null) {
            removeProperties(aliasProperty);
            Field aliasField = new Field(aliasProperty, alias, Field.Store.YES, Field.Index.UN_TOKENIZED);
            aliasField.setOmitNorms(true);
            document.add(aliasField);
        }

        verifyResourceMapping();

        List fields = document.getFields();
        for (Iterator fieldsIt = fields.iterator(); fieldsIt.hasNext();) {
            Field field = (Field) fieldsIt.next();
            LuceneProperty lProperty = new LuceneProperty(field);
            lProperty.setPropertyMapping(resourceMapping.getResourcePropertyMapping(field.name()));
            properties.add(lProperty);
        }
    }

    public void copy(Resource resource) {
        LuceneResource luceneResource = (LuceneResource) resource;
        this.document = luceneResource.document;
        this.docNum = luceneResource.docNum;
        this.properties = luceneResource.properties;
        this.aliasProperty = luceneResource.aliasProperty;
        this.searchEngine = luceneResource.searchEngine;
        this.resourceMapping = luceneResource.resourceMapping;
    }

    public Document getDocument() {
        return this.document;
    }

    public ResourceKey resourceKey() {
        if (resourceKey == null) {
            resourceKey = new ResourceKey(resourceMapping, this);
        }
        return resourceKey;
    }

    public String get(String name) {
        return document.get(name);
    }

    public Object getObject(String name) {
        Property prop = getProperty(name);
        if (prop == null) {
            return null;
        }
        return prop.getObjectValue();
    }

    public String[] getValues(String name) {
        return document.getValues(name);
    }

    public String getAlias() {
        return get(aliasProperty);
    }

    public String getId() {
        String[] ids = getIds();
        return ids[0];
    }

    public String[] getIds() {
        Property[] idProperties = getIdProperties();
        String[] ids = new String[idProperties.length];
        for (int i = 0; i < idProperties.length; i++) {
            if (idProperties[i] != null) {
                ids[i] = idProperties[i].getStringValue();
            }
        }
        return ids;
    }

    public Property getIdProperty() {
        Property[] idProperties = getIdProperties();
        return idProperties[0];
    }

    public Property[] getIdProperties() {
        ResourcePropertyMapping[] resourcePropertyMappings = resourceMapping.getIdMappings();
        Property[] idProperties = new Property[resourcePropertyMappings.length];
        for (int i = 0; i < resourcePropertyMappings.length; i++) {
            idProperties[i] = getProperty(resourcePropertyMappings[i].getPath().getPath());
        }
        return idProperties;
    }

    public Resource addProperty(String name, Object value) throws SearchEngineException {
        String alias = getAlias();

        ResourcePropertyMapping propertyMapping = resourceMapping.getResourcePropertyMapping(name);
        if (propertyMapping == null) {
            throw new SearchEngineException("No resource property mapping is defined for alias [" + alias
                    + "] and resource property [" + name + "]");
        }
        ResourcePropertyConverter converter = (ResourcePropertyConverter) propertyMapping.getConverter();
        if (converter == null) {
            converter = (ResourcePropertyConverter) searchEngine.getSearchEngineFactory().getMapping().
                    getConverterLookup().lookupConverter(value.getClass());
        }
        String strValue = converter.toString(value, propertyMapping);

        Property property = searchEngine.createProperty(strValue, propertyMapping);
        property.setBoost(propertyMapping.getBoost());
        return addProperty(property);
    }

    public Resource addProperty(String name, Reader value) throws SearchEngineException {
        String alias = getAlias();

        ResourcePropertyMapping propertyMapping = resourceMapping.getResourcePropertyMapping(name);
        if (propertyMapping == null) {
            throw new SearchEngineException("No resource property mapping is defined for alias [" + alias
                    + "] and resource property [" + name + "]");
        }

        Field.TermVector fieldTermVector = LuceneUtils.getFieldTermVector(propertyMapping.getTermVector());
        Field field = new Field(name, value, fieldTermVector);
        LuceneProperty property = new LuceneProperty(field);
        property.setBoost(propertyMapping.getBoost());
        property.setPropertyMapping(propertyMapping);
        return addProperty(property);
    }

    public Resource addProperty(Property property) {
        LuceneProperty lProperty = (LuceneProperty) property;
        lProperty.setPropertyMapping(resourceMapping.getResourcePropertyMapping(property.getName()));
        properties.add(property);
        document.add(lProperty.getField());
        return this;
    }

    public Resource removeProperty(String name) {
        document.removeField(name);
        Iterator it = properties.iterator();
        while (it.hasNext()) {
            Property property = (Property) it.next();
            if (property.getName().equals(name)) {
                it.remove();
                return this;
            }
        }
        return this;
    }

    public Resource removeProperties(String name) {
        document.removeFields(name);
        Iterator it = properties.iterator();
        while (it.hasNext()) {
            Property property = (Property) it.next();
            if (property.getName().equals(name)) {
                it.remove();
            }
        }
        return this;
    }

    public Property getProperty(String name) {
        for (int i = 0; i < properties.size(); i++) {
            Property property = (Property) properties.get(i);
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    public Property[] getProperties(String name) {
        List result = new ArrayList();
        for (int i = 0; i < properties.size(); i++) {
            Property property = (Property) properties.get(i);
            if (property.getName().equals(name)) {
                result.add(property);
            }
        }

        if (result.size() == 0)
            return new Property[0];

        return (Property[]) result.toArray(new Property[result.size()]);
    }

    public Property[] getProperties() {
        return (Property[]) properties.toArray(new LuceneProperty[properties.size()]);
    }

    public float getBoost() {
        return document.getBoost();
    }

    public Resource setBoost(float boost) {
        document.setBoost(boost);
        return this;
    }

    public void setDocNum(int docNum) {
        this.docNum = docNum;
    }

    /**
     * Returns the Lucene document number. If not set (can be in case the
     * resource is newly created), than returns -1.
     */
    public int getDocNum() {
        return this.docNum;
    }

    private void verifyResourceMapping() throws SearchEngineException {
        String alias = getAlias();
        if (resourceMapping == null) {
            if (alias == null) {
                throw new SearchEngineException(
                        "Can't add a resource property based on resource mapping without an alias associated with the resource first");
            }
            if (!searchEngine.getSearchEngineFactory().getMapping().hasRootMappingByAlias(alias)) {
                throw new SearchEngineException("No mapping is defined for alias [" + alias + "]");
            }
            resourceMapping = searchEngine.getSearchEngineFactory().getMapping().getRootMappingByAlias(alias);
        }
    }

    public String toString() {
        return "{" + getAlias() + "} " + StringUtils.arrayToCommaDelimitedString(getProperties());
    }

    // methods from the Map interface
    // ------------------------------

    public void clear() {
        throw new UnsupportedOperationException("Map operations are supported for read operations only");
    }

    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Map operations are supported for read operations only");
    }

    public void putAll(Map t) {
        for (Iterator it = t.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Entry) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public Object remove(Object key) {
        removeProperties(key.toString());
        // TODO should return the old value
        return null;
    }

    public Object put(Object key, Object value) {
        removeProperties(key.toString());
        if (value instanceof Property) {
            addProperty((Property) value);
        } else if (value instanceof Property[]) {
            Property[] valueProps = (Property[]) value;
            for (int i = 0; i < valueProps.length; i++) {
                addProperty(valueProps[i]);
            }
        }
        // TODO should return the old value
        return null;
    }

    public Set entrySet() {
        Set keySey = keySet();
        Set entrySet = Collections.unmodifiableSet(new HashSet());
        for (Iterator it = keySey.iterator(); it.hasNext();) {
            final String name = it.next().toString();
            final Property[] props = getProperties(name);
            entrySet.add(new Map.Entry() {
                public Object getKey() {
                    return name;
                }

                public Object getValue() {
                    return props;
                }

                public Object setValue(Object value) {
                    put(name, value);
                    // TODO should return the old value
                    return null;
                }
            });
        }
        return entrySet;
    }

    public Set keySet() {
        Set keySet = new HashSet();
        for (Iterator it = properties.iterator(); it.hasNext();) {
            keySet.add(((Property) it.next()).getName());
        }
        return Collections.unmodifiableSet(keySet);
    }

    public boolean containsKey(Object key) {
        return getProperty(key.toString()) != null;
    }

    public int size() {
        return this.properties.size();
    }

    public boolean isEmpty() {
        return this.properties.isEmpty();
    }

    public Collection values() {
        Set keySey = keySet();
        List values = Collections.unmodifiableList(new ArrayList());
        for (Iterator it = keySey.iterator(); it.hasNext();) {
            values.add(get(it.next()));
        }
        return values;
    }

    public Object get(Object key) {
        return getProperties(key.toString());
    }
}
