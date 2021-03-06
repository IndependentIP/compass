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

package org.compass.gps.device.jdbc.mapping;

import javax.sql.DataSource;

import org.compass.gps.device.jdbc.JdbcGpsDeviceException;

/**
 * A mapping that needs to perform actions in order to generate the required
 * mappings for it's proper operation.
 * 
 * @author kimchy
 */
public interface AutoGenerateMapping {

    /**
     * Results in inner generation of the required mappings and data structues
     * of the mapping using the given jdbc <code>DataSource</code>.
     */
    void generateMappings(DataSource dataSource) throws JdbcGpsDeviceException;
}
