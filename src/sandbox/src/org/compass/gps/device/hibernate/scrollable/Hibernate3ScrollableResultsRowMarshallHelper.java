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

package org.compass.gps.device.hibernate.scrollable;

import java.util.Iterator;

import org.compass.core.Compass;
import org.compass.core.CompassSession;
import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.mapping.ResourceMapping;
import org.compass.core.mapping.ResourcePropertyMapping;
import org.compass.core.spi.InternalCompass;
import org.compass.core.spi.InternalCompassSession;
import org.compass.gps.device.hibernate.HibernateGpsDeviceException;
import org.compass.gps.device.hibernate.scrollable.snapshot.HibernateAliasRowSnapshot;
import org.compass.gps.device.jdbc.mapping.ColumnToPropertyMapping;
import org.compass.gps.device.jdbc.mapping.ResultSetToResourceMapping;
import org.compass.gps.device.jdbc.mapping.VersionColumnMapping;
import org.hibernate.ScrollableResults;

public class Hibernate3ScrollableResultsRowMarshallHelper {

    private ResultSetToResourceMapping mapping;

    private CompassSession session;

    private HibernateAliasRowSnapshot rowSnapshot;

    private Resource resource;

    private boolean marshallResource = false;

    private boolean marshallVersioning = false;

    private ResourceMapping resourceMapping;

    private Hibernate3Dialect dialect;

    /**
     * Creates a new marshaller helper that will marhsall the
     * <code>ScrollableResults</code> to the given <code>Resource</code>.
     */
    public Hibernate3ScrollableResultsRowMarshallHelper(ResultSetToResourceMapping mapping, CompassSession session,
                                                        Resource resource) {
        this(mapping, session, resource, null);
    }

    /**
     * Creates a new marshaller helper that will marshall the
     * <code>ScrollableResults</code> to the given {@link HibernateAliasRowSnapshot}.
     */
    public Hibernate3ScrollableResultsRowMarshallHelper(ResultSetToResourceMapping mapping,
                                                        HibernateAliasRowSnapshot rowSnapshot, Compass compass) {
        this(mapping, null, null, rowSnapshot, compass);
    }

    /**
     * Creates a new marshaller helper that will marhsall that
     * <code>ScrollableResults</code> to both the given <code>Resource</code> and
     * {@link HibernateAliasRowSnapshot}.
     */
    public Hibernate3ScrollableResultsRowMarshallHelper(ResultSetToResourceMapping mapping, CompassSession session,
                                                        Resource resource, HibernateAliasRowSnapshot rowSnapshot) {
        this(mapping, session, resource, rowSnapshot, ((InternalCompassSession) session).getCompass());
    }

    public Hibernate3ScrollableResultsRowMarshallHelper(ResultSetToResourceMapping mapping, CompassSession session,
                                                        Resource resource, HibernateAliasRowSnapshot rowSnapshot, Compass compass) {
        this.mapping = mapping;
        this.session = session;
        this.rowSnapshot = rowSnapshot;
        this.dialect = new Hibernate3Dialect();
        resourceMapping = ((InternalCompass) compass).getMapping().getMappingByAlias(mapping.getAlias());
        if (rowSnapshot == null || !mapping.supportsVersioning()) {
            marshallVersioning = false;
        } else {
            marshallVersioning = true;
        }
        this.resource = resource;
        if (resource == null) {
            marshallResource = false;
        } else {
            marshallResource = true;
        }
    }


    /**
     * Marshalls the <code>ScrollableResults</code>.
     */
    public void marshallResultSet(ScrollableResults rs, String[] returnAliases) {
        marshallIds(rs, returnAliases);
        marshallVersionsIfNeeded(rs, returnAliases);
        marshallMappedData(rs, returnAliases);
        marshallUnMappedIfNeeded(rs, returnAliases);
    }


    public void marshallIds(ScrollableResults rs, String[] returnAliases) {
        for (Iterator it = mapping.idMappingsIt(); it.hasNext();) {
            ColumnToPropertyMapping ctpMapping = (ColumnToPropertyMapping) it.next();
            Object value = dialect.getValue(rs, returnAliases, ctpMapping);
            String stringValue = dialect.getStringValue(rs, returnAliases, ctpMapping);

            if (value == null) {
                throw new HibernateGpsDeviceException("Id [" + ctpMapping + "] for alias [" + mapping.getAlias()
                        + "] can not be null");
            }
            if (marshallResource) {
                marshallProperty(ctpMapping, stringValue);
            }
            if (marshallVersioning) {
                rowSnapshot.addIdValue(value);
            }
        }
    }


    public void marshallMappedData(ScrollableResults rs, String[] returnAliases) {
        if (!marshallResource) {
            return;
        }
        for (Iterator it = mapping.dataMappingsIt(); it.hasNext();) {
            ColumnToPropertyMapping ctpMapping = (ColumnToPropertyMapping) it.next();
            String value = dialect.getStringValue(rs, returnAliases, ctpMapping);
            if (value == null) {
                continue;
            }
            marshallProperty(ctpMapping, value);
        }
    }


    public void marshallVersionsIfNeeded(ScrollableResults rs, String[] returnAliases) {
        if (!marshallVersioning) {
            return;
        }
        for (Iterator it = mapping.versionMappingsIt(); it.hasNext();) {
            VersionColumnMapping versionMapping = (VersionColumnMapping) it.next();
            Long version = dialect.getVersion(rs, returnAliases, versionMapping);
            rowSnapshot.addVersionValue(version);
        }
    }


    public void marshallUnMappedIfNeeded(ScrollableResults rs, String[] returnAliases) {
        if (!marshallResource || !mapping.isIndexUnMappedColumns()) {
            return;
        }
        int count = returnAliases.length;
        for (int i = 0; i != count; i++) {
            String columnName = returnAliases[i];
            if (mapping.getMappingsForColumn(columnName) == null && mapping.getMappingsForColumn(i) == null) {
                String value = dialect.getStringValue(rs, i);
                if (value == null) {
                    continue;
                }
                Property p = session.createProperty(columnName, value, Property.Store.YES, Property.Index.TOKENIZED);
                resource.addProperty(p);
            }
        }
    }

    public void marshallProperty(ColumnToPropertyMapping ctpMapping, String value) {
        ResourcePropertyMapping propertyMapping = resourceMapping.getResourcePropertyMapping(ctpMapping.getPropertyName());
        if (propertyMapping == null) {
            Property p = session.createProperty(ctpMapping.getPropertyName(), value, ctpMapping.getPropertyStore(),
                    ctpMapping.getPropertyIndex(), ctpMapping.getPropertyTermVector());
            p.setBoost(ctpMapping.getBoost());
            resource.addProperty(p);
        } else {
            // has explicit mappings (not auto generated), use additional settings (like analyzer and such).
            resource.addProperty(ctpMapping.getPropertyName(), value);
        }
    }

}
