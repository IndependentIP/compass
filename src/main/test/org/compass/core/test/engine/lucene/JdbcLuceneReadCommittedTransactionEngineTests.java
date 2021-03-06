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
import org.compass.core.lucene.engine.transaction.ReadCommittedTransaction;

/**
 */
public class JdbcLuceneReadCommittedTransactionEngineTests extends AbstractReadCommittedTransactionTests {

    protected CompassSettings buildCompassSettings() {
        CompassSettings settings = super.buildCompassSettings();
        settings.setSetting(CompassEnvironment.CONNECTION, "jdbc://jdbc:hsqldb:mem:test");
        settings.setSetting(CompassEnvironment.Transaction.ISOLATION_CLASS, ReadCommittedTransaction.class.getName());
        settings.setSetting(LuceneEnvironment.JdbcStore.DIALECT, "org.apache.lucene.store.jdbc.dialect.HSQLDialect");
        settings.setSetting(LuceneEnvironment.JdbcStore.Connection.DRIVER_CLASS, "org.hsqldb.jdbcDriver");
        settings.setSetting(LuceneEnvironment.JdbcStore.Connection.USERNAME, "sa");
        settings.setSetting(LuceneEnvironment.JdbcStore.Connection.PASSWORD, "");
        return settings;
    }

    public void testSettings() {
        assertEquals(ReadCommittedTransaction.class.getName(), getSettings().getSetting(
                CompassEnvironment.Transaction.ISOLATION_CLASS));
    }

}
