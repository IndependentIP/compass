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

package org.compass.core.test.engine.lucene;

import org.compass.core.config.CompassEnvironment;
import org.compass.core.config.CompassSettings;
import org.compass.core.lucene.LuceneEnvironment;
import org.compass.core.lucene.engine.store.wrapper.AsyncMemoryMirrorDirectoryWrapperProvider;
import org.compass.core.lucene.engine.transaction.readcommitted.ReadCommittedTransaction;

/**
 * @author kimchy
 */
public class AsyncMemoryWrapperReadCommittedTransactionEngineTests extends AbstractReadCommittedTransactionTests {

    protected CompassSettings buildCompassSettings() {
        CompassSettings settings = super.buildCompassSettings();
        settings.setSetting(CompassEnvironment.CONNECTION, "target/test-index");
        settings.setSetting(CompassEnvironment.Transaction.ISOLATION_CLASS, ReadCommittedTransaction.class.getName());
        settings.setGroupSettings(LuceneEnvironment.DirectoryWrapper.PREFIX, "wrapper",
                new String[] {LuceneEnvironment.DirectoryWrapper.TYPE}, 
                new String[] {AsyncMemoryMirrorDirectoryWrapperProvider.class.getName()});
        return settings;
    }

    public void testSettings() {
        assertEquals(ReadCommittedTransaction.class.getName(), getSettings().getSetting(
                CompassEnvironment.Transaction.ISOLATION_CLASS));
    }

}
