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

package org.compass.gps.device.hibernate.simple;

import junit.framework.TestCase;
import org.compass.core.Compass;
import org.compass.core.config.CompassConfiguration;
import org.compass.gps.CompassGps;
import org.compass.gps.device.hibernate.HibernateGpsDevice;
import org.compass.gps.impl.SingleCompassGps;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * @author kimchy
 */
public abstract class AbstractHibernateGpsDeviceTests extends TestCase {

    protected SessionFactory sessionFactory;

    protected Compass compass;

    protected CompassGps compassGps;

    protected void setUp() throws Exception {
        sessionFactory = doSetUpSessionFactory();
        setUpCompass();
        setUpGps();
        compassGps.start();
        setUpDB();
    }

    protected void tearDown() throws Exception {
        tearDownDB();
        compassGps.stop();
        compass.close();
        sessionFactory.close();
    }

    protected abstract SessionFactory doSetUpSessionFactory();

    protected abstract void setUpCoreCompass(CompassConfiguration conf);

    protected void setUpCompass() {
        CompassConfiguration cpConf = new CompassConfiguration()
                .setConnection("target/test-index");
        setUpCoreCompass(cpConf);
        compass = cpConf.buildCompass();
        compass.getSearchEngineIndexManager().deleteIndex();
        compass.getSearchEngineIndexManager().verifyIndex();
    }

    protected void setUpGps() {
        compassGps = new SingleCompassGps(compass);
        setUpGpsDevice();
    }

    protected void setUpGpsDevice() {
        HibernateGpsDevice hibernateGpsDevice = new HibernateGpsDevice();
        hibernateGpsDevice.setName("jdoDevice");
        hibernateGpsDevice.setSessionFactory(sessionFactory);
        addDeviceSettings(hibernateGpsDevice);
        ((SingleCompassGps) compassGps).addGpsDevice(hibernateGpsDevice);
    }

    protected void addDeviceSettings(HibernateGpsDevice device) {

    }

    protected void setUpDB() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        setUpDB(session);
        transaction.commit();
        session.close();
    }

    protected void setUpDB(Session session) {
    }

    protected void tearDownDB() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        tearDownDB(session);
        transaction.commit();
        session.close();
    }

    protected void tearDownDB(Session session) {
    }
}