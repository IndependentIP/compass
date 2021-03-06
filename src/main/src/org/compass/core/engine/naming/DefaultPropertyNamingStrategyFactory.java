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

package org.compass.core.engine.naming;

import org.compass.core.config.CompassConfigurable;
import org.compass.core.config.CompassEnvironment;
import org.compass.core.config.CompassSettings;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.util.ClassUtils;

/**
 * A default implementation of the property naming strategy factory. Can be used
 * to create naming strategies that has default constructor, and no
 * initialization requirements.
 * <p/>
 * Uses the {@link org.compass.core.config.CompassEnvironment.NamingStrategy#TYPE}
 * setting for the fully qualified class name of the property naming strategy.
 * If non is set, defaults to the
 * {@link DefaultPropertyNamingStrategy}
 * <p/>
 * The {@link org.compass.core.config.CompassConfiguration} creates the factory,
 * which can be set using the
 * {@link org.compass.core.config.CompassEnvironment.NamingStrategy#FACTORY_TYPE}
 * setting, and defaults to this class as the factory.
 *
 * @author kimchy
 */
public class DefaultPropertyNamingStrategyFactory implements PropertyNamingStrategyFactory {

    public PropertyNamingStrategy createNamingStrategy(CompassSettings settings) {
        PropertyNamingStrategy namingStrategy;
        String namingStrategySetting = settings.getSetting(CompassEnvironment.NamingStrategy.TYPE,
                DefaultPropertyNamingStrategy.class.getName());
        try {
            namingStrategy = (PropertyNamingStrategy) ClassUtils.forName(namingStrategySetting).newInstance();
        } catch (Exception e) {
            throw new SearchEngineException("Cannot create naming Strategy [" + namingStrategySetting
                    + "]. Please verify the naming setting at [" + CompassEnvironment.NamingStrategy.TYPE + "]", e);
        }
        if (namingStrategy instanceof CompassConfigurable) {
            ((CompassConfigurable) namingStrategy).configure(settings);
        }
        return namingStrategy;
    }

}
