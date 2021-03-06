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

package org.compass.core.mapping.osem;

import org.compass.core.mapping.AbstractResourcePropertyMapping;
import org.compass.core.mapping.Mapping;
import org.compass.core.mapping.OverrideByNameMapping;

/**
 * @author kimchy
 */
public class DynamicMetaDataMapping extends AbstractResourcePropertyMapping implements OverrideByNameMapping, OsemMapping {

    private boolean overrideByName;

    private String expression;

    private String format;

    private Class type;

    public Mapping copy() {
        DynamicMetaDataMapping copy = new DynamicMetaDataMapping();
        super.copy(copy);
        copy.setOverrideByName(isOverrideByName());
        copy.setExpression(getExpression());
        copy.setFormat(getFormat());
        copy.setType(getType());
        return copy;
    }

    public boolean hasAccessors() {
        return false;
    }

    public boolean isOverrideByName() {
        return this.overrideByName;
    }

    public void setOverrideByName(boolean overrideByName) {
        this.overrideByName = overrideByName;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }
}
