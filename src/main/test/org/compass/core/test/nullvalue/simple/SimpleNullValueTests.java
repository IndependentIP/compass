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

package org.compass.core.test.nullvalue.simple;

import org.compass.core.CompassHits;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.test.AbstractTestCase;

/**
 * @author kimchy
 */
public class SimpleNullValueTests extends AbstractTestCase {

    protected String[] getMappings() {
        return new String[]{"nullvalue/simple/mapping.cpm.xml"};
    }

    public void testSimleNullValue() throws Exception {
        CompassSession session = openSession();
        CompassTransaction tr = session.beginTransaction();

        A a = new A();
        a.id = 1;
        a.value = null;
        session.save("a", a);

        a = (A) session.load("a", "1");
        assertEquals(1, a.id);
        assertNull(a.value);

        CompassHits hits = session.find("moo");
        assertEquals(1, hits.length());
        hits = session.find("value:moo");
        assertEquals(1, hits.length());

        tr.commit();
        session.close();
    }
}
