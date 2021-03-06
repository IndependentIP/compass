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

package org.compass.core.engine;

import org.compass.core.Property;
import org.compass.core.Property.TermVector;
import org.compass.core.converter.Converter;
import org.compass.core.engine.naming.PropertyPath;
import org.compass.core.mapping.Mapping;
import org.compass.core.mapping.ResourcePropertyMapping;

/**
 * @author kimchy
 */
public class MockPropertyMapping implements ResourcePropertyMapping {

    private float boost = 1.0f;

    private String name;

    private PropertyPath path;

    private boolean isInternal = false;

    public MockPropertyMapping(String name, PropertyPath path) {
        this.name = name;
        this.path = path;
    }

    /**
     * @return Returns the boost.
     */
    public float getBoost() {
        return boost;
    }

    /**
     * @param boost
     *            The boost to set.
     */
    public void setBoost(float boost) {
        this.boost = boost;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the isInternal.
     */
    public boolean isInternal() {
        return isInternal;
    }

    /**
     * @param isInternal
     *            The isInternal to set.
     */
    public void setInternal(boolean isInternal) {
        this.isInternal = isInternal;
    }

    public Property.Store getStore() {
        return null;
    }

    public Property.Index getIndex() {
        return null;
    }

    public TermVector getTermVector() {
        return null;
    }

    public Mapping copy() {
        return null;
    }

    public void setPath(PropertyPath path) {
        this.path = path;
    }

    public PropertyPath getPath() {
        return path;
    }

    public void addConverterParam(String paramName, String paramValue) {
    }

    public boolean controlsObjectNullability() {
        return false;
    }

    public Converter getConverter() {
        return null;
    }

    public String getConverterParam() {
        return null;
    }

    public String getConverterParam(String paramName) {
        return null;
    }

    public void setConverter(Converter converter) {
    }

    public String getAnalyzer() {
        return null;
    }

    public boolean isExcludeFromAll() {
        return false;
    }

    public ReverseType getReverse() {
        return ReverseType.NO;
    }

    public String getConverterName() {
        return null;
    }

    public void setConverterName(String name) {
        
    }

    public boolean isOmitNorms() {
        return false;
    }

    public String getRootAlias() {
        return null;
    }

    public String getNullValue() {
        return null;
    }

    public boolean hasNullValue() {
        return false;
    }
}
