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

package org.compass.core.test.nounmarshall.component;

import java.util.ArrayList;

import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.Resource;
import org.compass.core.test.AbstractTestCase;

/**
 * @author kimchy
 */
public class CyclicTests extends AbstractTestCase {

    protected String[] getMappings() {
        return new String[]{"nounmarshall/component/Cyclic.cpm.xml"};
    }

    public void testSingleLevelCyclic() throws Exception {
        CompassSession session = openSession();
        CompassTransaction tr = session.beginTransaction();

        Cyclic rootCyclic = new Cyclic();
        rootCyclic.id = new Long(1);
        rootCyclic.value = "rootCyclic";

        Cyclic cyclic1 = new Cyclic();
        cyclic1.id = new Long(2);
        cyclic1.value = "cyclic1";

        rootCyclic.cyclic = cyclic1;

        session.save(rootCyclic);

        Resource resource = session.loadResource(Cyclic.class, new Long(1));
        assertEquals(5, resource.getProperties().length);
        assertEquals(2, resource.getProperties("value").length);
        // this is our actual id
        assertEquals("1", resource.get("$/cyclic/id"));
        // the id saved because of the component, but has not effect
        assertEquals(2, resource.getProperties("$/cyclic/id").length);

        tr.commit();
        session.close();
    }

    public void testSingleLevelCollectionCyclic() throws Exception {
        CompassSession session = openSession();
        CompassTransaction tr = session.beginTransaction();

        Cyclic rootCyclic = new Cyclic();
        rootCyclic.id = new Long(1);
        rootCyclic.value = "rootCyclic";

        Cyclic cyclic1 = new Cyclic();
        cyclic1.id = new Long(2);
        cyclic1.value = "cyclic1";

        Cyclic cyclic2 = new Cyclic();
        cyclic2.id = new Long(3);
        cyclic2.value = "cyclic2";

        rootCyclic.cyclics = new ArrayList();
        rootCyclic.cyclics.add(cyclic1);
        rootCyclic.cyclics.add(cyclic2);

        session.save(rootCyclic);

        Resource resource = session.loadResource(Cyclic.class, new Long(1));
        assertEquals(7, resource.getProperties().length);
        assertEquals(3, resource.getProperties("value").length);
        // this is our actual id
        assertEquals("1", resource.get("$/cyclic/id"));
        // the id saved because of the component, but has not effect
        assertEquals(3, resource.getProperties("$/cyclic/id").length);

        tr.commit();
        session.close();
    }
}