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

package org.compass.core;

import java.io.Serializable;
import javax.naming.Referenceable;

import org.compass.core.config.CompassSettings;
import org.compass.core.engine.SearchEngineIndexManager;
import org.compass.core.engine.SearchEngineOptimizer;

/**
 * Creates a CompassSession. Usually an application has a single Compass object.
 * Threads servicing client requests obtain sessions from Compass.
 * <p/>
 * Implementors must be threadsafe.
 * <p/>
 * CompassSessions are immutable. The behaviour of a Compass is controlled by
 * settings supplied at configuration time through CompassConfiguration. These
 * settings are defined on the web site.
 * <p/>
 * Compass also provides operations that are on a higher level than a session,
 * like create/delete operations on index data files and manages the index
 * optimiser lifecycle.
 *
 * @author kimchy
 * @see CompassSession
 * @see org.compass.core.config.CompassConfiguration
 */
public interface Compass extends Referenceable, Serializable {

    /**
     * If there is a transaction bound session, will return it. Otherwise
     * returns a new session.
     * <p/>
     * A transactional bound session is bounded to the transaction when calling
     * the CompassSession.beginTransaction() or if Compass tries to automatically join
     * an already running transaction (see next paragraph).
     * <p/>
     * If creating a new session, will try to automatically join an existing
     * outer transaction. An outer transaction might be an already running Compass
     * local transaction, or an external transaciton (JTA or Spring for example). In
     * such cases, there is no need to perform any transaction managment code (begin
     * or commit/rollback transaction) or closing the opened session. Compass will also
     * bind the session to the same transaction if an outer transaction exists. Note, when
     * doing so, the mentioned code will have to always be excuted within an already running
     * transaction.
     *
     * @return CompassSession The compass session
     * @throws CompassException
     */
    CompassSession openSession() throws CompassException;

    /**
     * Closes Compass
     *
     * @throws CompassException
     */
    void close() throws CompassException;

    /**
     * Clones the current <code>Compass</code> instance. The added settings will merged with the current
     * compass settings, and control the creation of the new Compass.
     * <p/>
     * Note, that the new instance will not be registered with JNDI, as well as not start the optimizer.
     *
     * @param addedSettings The settings to be added.
     * @return the cloned compass instance.
     */
    Compass clone(CompassSettings addedSettings);

    /**
     * Retruns the search engine optimizer. You can controll the state of the
     * optimizer (by calling <code>stop</code> or <code>start</code>), you
     * can check if the index need optimization, and you can optimize the index.
     *
     * @return the search engine optimizer
     */
    SearchEngineOptimizer getSearchEngineOptimizer();

    /**
     * Return the search engine index manager. You can control the index using
     * it.
     *
     * @return the search engine index manager
     */
    SearchEngineIndexManager getSearchEngineIndexManager();
}
