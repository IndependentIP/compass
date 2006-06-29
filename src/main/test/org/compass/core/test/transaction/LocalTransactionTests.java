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

package org.compass.core.test.transaction;

import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.test.AbstractTestCase;

public class LocalTransactionTests extends AbstractTestCase {

    protected String[] getMappings() {
        return new String[] { "transaction/A.cpm.xml" };
    }

    public void testNestedLocalTransactionsWithCommits() throws Exception {
        CompassSession session = openSession();
        CompassTransaction tr = session.beginTransaction();

        Long id = new Long(1);
        A a = new A();
        a.setId(id);
        session.save(a);

        CompassSession nestedSession = openSession();
        CompassTransaction nestedTr = nestedSession.beginTransaction();
        assertTrue(session == nestedSession);
        a = (A) nestedSession.get(A.class, id);
        assertNotNull(a);
        nestedTr.commit();
        nestedSession.close();

        a = (A) session.get(A.class, id);
        assertNotNull(a);

        tr.commit();

        // verify that the instance was saved
        tr = session.beginTransaction();
        a = (A) session.get(A.class, id);
        assertNotNull(a);

        tr.commit();
        session.close();
    }

    public void testNestedLocalTransactionsWithNestedRollback() throws Exception {
        CompassSession session = openSession();
        CompassTransaction tr = session.beginTransaction();

        Long id = new Long(1);
        A a = new A();
        a.setId(id);
        session.save(a);

        CompassSession nestedSession = openSession();
        CompassTransaction nestedTr = nestedSession.beginTransaction();
        assertTrue(session == nestedSession);
        a = (A) nestedSession.get(A.class, id);
        assertNotNull(a);
        nestedTr.rollback();
        nestedSession.close();

        try {
            a = (A) session.get(A.class, id);
            tr.commit();
            fail();
        } catch (Exception e) {

        }

    }
}
