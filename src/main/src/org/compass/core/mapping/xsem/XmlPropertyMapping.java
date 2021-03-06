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

package org.compass.core.mapping.xsem;

import org.compass.core.converter.Converter;
import org.compass.core.mapping.AbstractResourcePropertyMapping;
import org.compass.core.mapping.Mapping;
import org.compass.core.mapping.OverrideByNameMapping;
import org.compass.core.mapping.ResourcePropertyMapping;
import org.compass.core.xml.XmlXPathExpression;

/**
 * @author kimchy
 */
public class XmlPropertyMapping extends AbstractResourcePropertyMapping implements ResourcePropertyMapping,
        OverrideByNameMapping, XPathEnabledMapping {

    private boolean overrideByName;

    private String xpath;

    private Converter valueConverter;

    private String valueConverterName;

    private XmlXPathExpression xpathExpression;

    public Mapping copy() {
        XmlPropertyMapping xmlPropertyMapping = new XmlPropertyMapping();
        copy(xmlPropertyMapping);
        return xmlPropertyMapping;
    }

    protected void copy(XmlPropertyMapping copy) {
        super.copy(copy);
        copy.setOverrideByName(isOverrideByName());
        copy.setXPath(getXPath());
        copy.setValueConverter(getValueConverter());
        copy.setValueConverterName(getValueConverterName());
        copy.setXPathExpression(getXPathExpression());
    }

    public boolean isOverrideByName() {
        return this.overrideByName;
    }

    public void setOverrideByName(boolean overrideByName) {
        this.overrideByName = overrideByName;
    }

    public String getXPath() {
        return xpath;
    }

    public void setXPath(String xpath) {
        this.xpath = xpath;
    }

    public Converter getValueConverter() {
        return valueConverter;
    }

    public void setValueConverter(Converter valueConverter) {
        this.valueConverter = valueConverter;
    }

    public String getValueConverterName() {
        return valueConverterName;
    }

    public void setValueConverterName(String valueConverterName) {
        this.valueConverterName = valueConverterName;
    }

    public XmlXPathExpression getXPathExpression() {
        return xpathExpression;
    }

    public void setXPathExpression(XmlXPathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
    }
}
