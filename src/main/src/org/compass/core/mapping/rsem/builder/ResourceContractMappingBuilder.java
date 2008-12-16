/*
 * Copyright 2004-2008 the original author or authors.
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

package org.compass.core.mapping.rsem.builder;

import org.compass.core.mapping.ContractMapping;
import org.compass.core.mapping.ContractMappingProvider;
import org.compass.core.mapping.internal.DefaultContractMapping;

/**
 * @author kimchy
 */
public class ResourceContractMappingBuilder implements ContractMappingProvider {

    private final DefaultContractMapping mapping;

    ResourceContractMappingBuilder(DefaultContractMapping mapping) {
        this.mapping = mapping;
    }

    public ContractMapping getMapping() {
        return mapping;
    }

    public ResourceContractMappingBuilder extendsAliases(String... extendedAliases) {
        mapping.setExtendedAliases(extendedAliases);
        return this;
    }

    public ResourceContractMappingBuilder analyzer(String analyzer) {
        mapping.setAnalyzer(analyzer);
        return this;
    }

    public ResourceContractMappingBuilder add(ResourceIdMappingBuilder builder) {
        mapping.addMapping(builder.mapping);
        return this;
    }

    public ResourceContractMappingBuilder add(ResourcePropertyMappingBuilder builder) {
        mapping.addMapping(builder.mapping);
        return this;
    }

    public ResourceContractMappingBuilder add(ResourceAnalyzerMappingBuilder builder) {
        mapping.addMapping(builder.mapping);
        return this;
    }

    public ResourceContractMappingBuilder add(ResourceBoostMappingBuilder builder) {
        mapping.addMapping(builder.mapping);
        return this;
    }
}