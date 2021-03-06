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

package org.compass.core.config.builder;

import java.io.*;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.config.CompassConfiguration;
import org.compass.core.config.CompassEnvironment;
import org.compass.core.config.ConfigurationException;

/**
 * @author kimchy
 */
public abstract class AbstractInputStreamConfigurationBuilder implements ConfigurationBuilder {

    protected Log log = LogFactory.getLog(getClass());

    public void configure(String resource, CompassConfiguration config) throws ConfigurationException {
        InputStream stream = CompassEnvironment.class.getResourceAsStream(resource);
        if (stream == null) {
            stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        }
        if (stream == null) {
            throw new ConfigurationException("Resource [" + resource + "] not found in class path");
        }
        configure(stream, resource, config);
    }

    public void configure(URL url, CompassConfiguration config) throws ConfigurationException {
        try {
            configure(url.openStream(), url.toExternalForm(), config);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to open url [" + url.toExternalForm() + "]", e);
        }
    }

    public void configure(File file, CompassConfiguration config) throws ConfigurationException {
        try {
            configure(new FileInputStream(file), file.getAbsolutePath(), config);
        } catch (FileNotFoundException fnfe) {
            throw new ConfigurationException(
                    "Could not find configuration file [" + file.getAbsolutePath() + "]", fnfe);
        }
    }

    private void configure(InputStream is, String resourceName, CompassConfiguration config) {
        try {
            doConfigure(is, resourceName, config);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                log.warn("Failed to close input stream for [" + resourceName + "]", e);
            }
        }
    }

    protected abstract void doConfigure(InputStream is, String resourceName, CompassConfiguration config)
            throws ConfigurationException;
}
