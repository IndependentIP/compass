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

import org.apache.lucene.document.Field;
import org.compass.core.Property;
import org.compass.core.converter.ResourcePropertyConverter;
import org.compass.core.engine.RepeatableReader;
import org.compass.core.mapping.ResourcePropertyMapping;

/**
 * @author kimchy
 */
public class LuceneProperty implements Property {

    private static final long serialVersionUID = 3690475809949104182L;

    private Field field;

    private RepeatableReader reader;

    private transient ResourcePropertyMapping propertyMapping;

    public LuceneProperty(Field field) {
        this.field = field;
    }

    public LuceneProperty(Field field, RepeatableReader reader) {
        this.field = field;
        this.reader = reader;
    }

    public void setPropertyMapping(ResourcePropertyMapping propertyMapping) {
        this.propertyMapping = propertyMapping;
    }

    public RepeatableReader getRepeatableReader() {
        return this.reader;
    }

    public Field getField() {
        return this.field;
    }

    public String getName() {
        return field.name();
    }

    public Object getObjectValue() {
        String value = getStringValue();
        if (propertyMapping == null) {
            return value;
        }
        ResourcePropertyConverter converter = (ResourcePropertyConverter) propertyMapping.getConverter();
        if (converter == null) {
            return null;
        }
        return converter.fromString(value, propertyMapping);
    }

    public String getStringValue() {
        return field.stringValue();
    }

    public byte[] getBinaryValue() {
        return field.binaryValue();
    }

    public float getBoost() {
        return field.getBoost();
    }

    public void setBoost(float boost) {
        field.setBoost(boost);
    }

    public boolean isIndexed() {
        return field.isIndexed();
    }

    public boolean isStored() {
        return field.isStored();
    }

    public boolean isCompressed() {
        return field.isCompressed();
    }

    public boolean isBinary() {
        return field.isBinary();
    }

    public boolean isTokenized() {
        return field.isTokenized();
    }

    public boolean isTermVectorStored() {
        return field.isTermVectorStored();
    }

    /**
     * Not exported to the users since it is always false when loading the Field
     * from Lucene.
     */
    public boolean isStoreOffsetWithTermVector() {
        return field.isStoreOffsetWithTermVector();
    }

    /**
     * Not exported to the users since it is always false when loading the Field
     * from Lucene.
     */
    public boolean isStorePositionWithTermVector() {
        return field.isStorePositionWithTermVector();
    }

    public boolean isOmitNorms() {
        return field.getOmitNorms();
    }

    public void setOmitNorms(boolean omitNorms) {
        field.setOmitNorms(omitNorms);
    }

    public String toString() {
        return "[" + field + "]";
    }
}
