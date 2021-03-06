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

package org.compass.core.config.binding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.config.CompassSettings;
import org.compass.core.config.ConfigurationException;
import org.compass.core.config.InputStreamMappingResolver;
import org.compass.core.mapping.CompassMapping;
import org.compass.core.mapping.MappingException;
import org.compass.core.mapping.ResourceMapping;
import org.compass.core.metadata.CompassMetaData;

/**
 * @author kimchy
 */
public abstract class AbstractInputStreamMappingBinding implements MappingBinding {

    private final Log log = LogFactory.getLog(getClass());

    protected CompassMapping mapping;

    protected CompassMetaData metaData;

    protected CompassSettings settings;

    public void setUpBinding(CompassMapping mapping, CompassMetaData metaData, CompassSettings settings) {
        this.mapping = mapping;
        this.metaData = metaData;
        this.settings = settings;
    }

    public boolean addResoruceMapping(ResourceMapping resourceMapping) throws ConfigurationException, MappingException {
        mapping.addMapping(resourceMapping);
        return true;
    }

    public boolean addResource(String path) throws ConfigurationException, MappingException {
        return addResource(path, Thread.currentThread().getContextClassLoader());
    }

    public boolean addResource(String path, ClassLoader classLoader) throws ConfigurationException, MappingException {
        InputStream rsrc = classLoader.getResourceAsStream(path);
        return rsrc != null && internalAddInputStream(rsrc, path, true);
    }

    public boolean addURL(URL url) throws ConfigurationException, MappingException {
        try {
            return internalAddInputStream(url.openStream(), url.toExternalForm(), true);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to open url [" + url.toExternalForm() + "]");
        }
    }

    public boolean addDirectory(File dir) throws ConfigurationException, MappingException {
        boolean addedAtLeastOne = false;
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                boolean retVal = addDirectory(file);
                if (retVal) {
                    addedAtLeastOne = true;
                }
            } else if (file.getName().endsWith(getSuffix())) {
                boolean retVal = addFile(file);
                if (retVal) {
                    addedAtLeastOne = true;
                }
            }
        }
        return addedAtLeastOne;
    }

    public boolean addJar(File jar) throws ConfigurationException, MappingException {
        final JarFile jarFile;
        try {
            jarFile = new JarFile(jar);
        } catch (IOException ioe) {
            throw new ConfigurationException("Could not configure datastore from jar [" + jar.getName() + "]", ioe);
        }

        boolean addedAtLeastOne = false;
        Enumeration jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) jarEntries.nextElement();
            if (ze.getName().endsWith(getSuffix())) {
                try {
                    boolean retVal = internalAddInputStream(jarFile.getInputStream(ze), ze.getName(), true);
                    if (retVal) {
                        addedAtLeastOne = true;
                    }
                } catch (ConfigurationException me) {
                    throw me;
                } catch (Exception e) {
                    throw new ConfigurationException("Could not configure datastore from jar [" + jar.getAbsolutePath() + "]", e);
                }
            }
        }
        return addedAtLeastOne;
    }

    public boolean addFile(String filePath) throws ConfigurationException, MappingException {
        return addFile(new File(filePath));
    }

    public boolean addFile(File file) throws ConfigurationException, MappingException {
        try {
            return internalAddInputStream(new FileInputStream(file), file.getAbsolutePath(), true);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Could not configure mapping from file, file not found ["
                    + file.getAbsolutePath() + "]", e);
        }
    }

    public boolean addPackage(String packageName) throws ConfigurationException, MappingException {
        // nothing for us to do here
        return false;
    }

    public boolean addClass(Class clazz) throws ConfigurationException, MappingException {
        String fileName = clazz.getName().replace('.', '/') + getSuffix();
        InputStream rsrc = clazz.getClassLoader().getResourceAsStream(fileName);
        if (rsrc == null) {
            return false;
        }
        try {
            return internalAddInputStream(rsrc, fileName, true);
        } catch (ConfigurationException me) {
            throw new ConfigurationException("Error reading resource [" + fileName + "]", me);
        }
    }

    public boolean addMappingResolver(InputStreamMappingResolver mappingResolver) throws ConfigurationException, MappingException {
        return internalAddInputStream(mappingResolver.getMappingAsInputStream(), mappingResolver.getName(), true);
    }

    public boolean addInputStream(InputStream is, String resourceName) throws ConfigurationException, MappingException {
        return internalAddInputStream(is, resourceName, false);
    }

    private boolean internalAddInputStream(InputStream is, String resourceName, boolean closeStream) throws ConfigurationException, MappingException {
        try {
            if (resourceName.indexOf(getSuffix()) == -1) {
                if (log.isTraceEnabled()) {
                    log.trace("Resource name [" + resourceName + "] does not end with suffix [" + getSuffix() + "], ignoring");
                }
                return false;
            }
            return doAddInputStream(is, resourceName);
        } finally {
            if (closeStream) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    protected abstract boolean doAddInputStream(InputStream is, String resourceName)
            throws ConfigurationException, MappingException;

    protected abstract String getSuffix();
}
