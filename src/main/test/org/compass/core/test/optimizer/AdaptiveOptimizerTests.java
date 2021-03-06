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

package org.compass.core.test.optimizer;

import java.io.IOException;

import org.apache.lucene.index.LuceneSubIndexInfo;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.config.CompassSettings;
import org.compass.core.lucene.LuceneEnvironment;
import org.compass.core.lucene.engine.optimizer.AdaptiveOptimizer;

public class AdaptiveOptimizerTests extends AbstractOptimizerTests {

    protected void addSettings(CompassSettings settings) {
        super.addSettings(settings);
        settings.setSetting(LuceneEnvironment.Optimizer.TYPE, AdaptiveOptimizer.class.getName());
        settings.setBooleanSetting(LuceneEnvironment.Optimizer.SCHEDULE, false);
        settings.setSetting(LuceneEnvironment.Optimizer.Adaptive.MERGE_FACTOR, "3");
        settings.setIntSetting(LuceneEnvironment.SearchEngineIndex.CACHE_INTERVAL_INVALIDATION, 0);
    }

    public void testOptimizerWithIndexCache() throws Exception {

        addData(0, 1);
        addData(1, 2);
        addData(2, 3);
        addData(3, 4);
        addData(4, 5);
        addData(5, 6);
        addData(6, 7);
        addData(7, 8);

        assertData(0, 8);

        assertTrue(getCompass().getSearchEngineOptimizer().needOptimization());
        getCompass().getSearchEngineOptimizer().optimize();

        CompassSession session = openSession();
        LuceneSubIndexInfo infos = LuceneSubIndexInfo.getIndexInfo("a", session);
        assertEquals(1, infos.size());
        session.close();

        assertData(0, 8);
    }


    public void testOptimizerMergeFactorSingleAdds() throws IOException {
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(0, 1);
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());

        addData(1, 2);
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());

        addData(2, 3);
        assertTrue(getCompass().getSearchEngineOptimizer().needOptimization());

        getCompass().getSearchEngineOptimizer().optimize();

        CompassSession session = openSession();
        LuceneSubIndexInfo infos = LuceneSubIndexInfo.getIndexInfo("a", session);
        assertEquals(1, infos.size());
        session.close();

        session = openSession();
        CompassTransaction tr = session.beginTransaction();
        A a = (A) session.load(A.class, new Long(0));
        assertNotNull(a);
        tr.commit();
        session.close();

    }

    public void testOptimizerWithBigFirstSegment() throws IOException {
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(0, 20);
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(20, 21);
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(21, 22);
        assertTrue(getCompass().getSearchEngineOptimizer().needOptimization());
        getCompass().getSearchEngineOptimizer().optimize();
        CompassSession session = openSession();
        LuceneSubIndexInfo infos = LuceneSubIndexInfo.getIndexInfo("a", session);
        session.close();
        assertEquals(2, infos.size());
        assertEquals(20, infos.info(0).docCount());
        assertEquals(2, infos.info(1).docCount());


        session = openSession();
        CompassTransaction tr = session.beginTransaction();
        A a = (A) session.load(A.class, new Long(0));
        assertNotNull(a);
        a = (A) session.load(A.class, new Long(21));
        assertNotNull(a);
        tr.commit();
        session.close();
    }

    public void testCamelCaseSegmentsLeftWithTwoSegments() throws IOException {
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(0, 10);
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(10, 11);
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(11, 15);
        assertTrue(getCompass().getSearchEngineOptimizer().needOptimization());
        getCompass().getSearchEngineOptimizer().optimize();
        CompassSession session = openSession();
        LuceneSubIndexInfo infos = LuceneSubIndexInfo.getIndexInfo("a", session);
        session.close();
        assertEquals(2, infos.size());
        assertEquals(10, infos.info(0).docCount());
        assertEquals(5, infos.info(1).docCount());

        session = openSession();
        CompassTransaction tr = session.beginTransaction();
        A a = (A) session.load(A.class, new Long(0));
        assertNotNull(a);
        a = (A) session.load(A.class, new Long(14));
        assertNotNull(a);
        tr.commit();
        session.close();
    }

    public void testCamelCaseSegmentsLeftWithOneSegment() throws IOException {
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(0, 10);
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(10, 11);
        assertFalse(getCompass().getSearchEngineOptimizer().needOptimization());
        addData(11, 25);
        assertTrue(getCompass().getSearchEngineOptimizer().needOptimization());
        getCompass().getSearchEngineOptimizer().optimize();
        CompassSession session = openSession();
        LuceneSubIndexInfo infos = LuceneSubIndexInfo.getIndexInfo("a", session);
        session.close();
        assertEquals(1, infos.size());
        assertEquals(25, infos.info(0).docCount());

        session = openSession();
        CompassTransaction tr = session.beginTransaction();
        A a = (A) session.load(A.class, new Long(0));
        assertNotNull(a);
        a = (A) session.load(A.class, new Long(24));
        assertNotNull(a);
        tr.commit();
        session.close();
    }
}
