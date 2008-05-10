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

package org.compass.core.mapping.internal;

import org.compass.core.Property;
import org.compass.core.mapping.ResourcePropertyMapping;
import org.compass.core.mapping.SpellCheckType;

/**
 * @author kimchy
 */
public interface InternalResourcePropertyMapping extends ResourcePropertyMapping {

    /**
     * Allows to set the resource property mapping null value.
     */
    void setNullValue(String nullValue);

    void setSpellCheck(SpellCheckType spellCheck);

    void setIndex(Property.Index index);

    void setStore(Property.Store store);

    void setTermVector(Property.TermVector termVector);

    void setOmitNorms(Boolean omitNorms);

    void setBoost(float boost);

    void setReverse(ReverseType reverseType);

    void setAnalyzer(String analyzer);

    void setExcludeFromAll(ExcludeFromAllType excludeFromAll);

    void setInternal(boolean internal);
}
