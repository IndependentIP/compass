package org.compass.gps.device.jpa.openjpa;

import java.util.HashMap;
import javax.persistence.EntityManagerFactory;

import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.compass.gps.device.jpa.AbstractSimpleJpaGpsDeviceTests;
import org.compass.gps.device.jpa.JpaGpsDevice;

/**
 * Performs JPA tests using OpenJPA specific support.
 *
 * Currently, this test is disabled (the X suffix) since it requires the following setting:
 * -javaagent:lib/openjpa/openjpa-0.9.7-incubating.jar
 *
 * @author kimchy
 */
public class OpenJPASimpleJpaGpsDeviceTestsX extends AbstractSimpleJpaGpsDeviceTests {

    @Override
    protected void addDeviceSettings(JpaGpsDevice device) {
        device.setInjectEntityLifecycleListener(true);
    }

    protected EntityManagerFactory doSetUpEntityManagerFactory() {
        return new PersistenceProviderImpl().createEntityManagerFactory("openjpa", new HashMap());
    }
}
