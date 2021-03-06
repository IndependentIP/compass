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

package org.compass.core.mapping.rsem;

import org.compass.core.Property.Index;
import org.compass.core.Property.Store;
import org.compass.core.mapping.BoostPropertyMapping;
import org.compass.core.mapping.Mapping;

/**
 * @author kimchy
 */
public class RawBoostPropertyMapping extends RawResourcePropertyMapping implements BoostPropertyMapping {

    private float defaultBoost = 1.0f;

    public Mapping copy() {
        RawBoostPropertyMapping boostPropertyMapping = new RawBoostPropertyMapping();
        super.copy(boostPropertyMapping);
        boostPropertyMapping.setDefaultBoost(getDefaultBoost());
        return boostPropertyMapping;
    }

    public String getBoostResourcePropertyName() {
        return getPath().getPath();
    }

    public void setDefaultBoost(float defaultBoost) {
        this.defaultBoost = defaultBoost;
    }

    public float getDefaultBoost() {
        return defaultBoost;
    }

    public Index getIndex() {
        return Index.UN_TOKENIZED;
    }

    public Store getStore() {
        return Store.YES;
    }
}