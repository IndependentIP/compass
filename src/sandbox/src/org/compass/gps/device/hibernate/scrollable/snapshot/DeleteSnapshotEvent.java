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

package org.compass.gps.device.hibernate.scrollable.snapshot;

import java.util.List;

import org.compass.gps.device.jdbc.mapping.ResultSetToResourceMapping;
import org.compass.gps.spi.CompassGpsInterfaceDevice;
import org.hibernate.Session;

/**
 * A delete snapshot event, works with
 * {@link org.compass.gps.device.hibernate.scrollable.snapshot.HibernateSnapshotEventListener#onDelete(DeleteSnapshotEvent)}.
 * Holds the
 * {@link org.compass.gps.device.jdbc.mapping.ResultSetToResourceMapping}
 * that maps to the result set that initiated the event, a list of
 * {@link org.compass.gps.device.hibernate.scrollable.snapshot.HibernateAliasRowSnapshot}
 * for all the row snapapshots that were deleted, and the
 * <code>CompassTemplate</code> to use in order to reflect the changes to the
 * index.
 *
 * @author kimchy
 */
public class DeleteSnapshotEvent extends AbstractSnapshotEvent {

    private ResultSetToResourceMapping mapping;

    private List deleteSnapshots;

    private CompassGpsInterfaceDevice compassGps;

    public DeleteSnapshotEvent(Session session, ResultSetToResourceMapping mapping,
                               List deleteSnapshots, CompassGpsInterfaceDevice compassGps) {
        super(session);
        this.mapping = mapping;
        this.deleteSnapshots = deleteSnapshots;
        this.compassGps = compassGps;
    }

    public List getDeleteSnapshots() {
        return deleteSnapshots;
    }

    public ResultSetToResourceMapping getMapping() {
        return mapping;
    }

    public CompassGpsInterfaceDevice getCompassGps() {
        return compassGps;
    }

}
