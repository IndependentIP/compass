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

package org.compass.core.transaction;

import org.compass.core.Compass;
import org.compass.core.CompassException;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.CompassTransaction.TransactionIsolation;
import org.compass.core.config.CompassSettings;
import org.compass.core.spi.InternalCompassSession;

/**
 * 
 * @author kimchy
 * 
 */

public interface TransactionFactory {

	void configure(Compass compass, CompassSettings settings) throws CompassException;

	CompassTransaction beginTransaction(InternalCompassSession session, TransactionIsolation transactionIsolation)
			throws CompassException;

    /**
     * Retuns a transaction bound session, or <code>null</code> if none is found.
     */
    CompassSession getTransactionBoundSession() throws CompassException;

    /**
     * If there is an outer running existing transaction, try and join it. This method
     * is called when opening a session and will ease the usage of Compass since there
     * won't be a need to begin a transaction explicitly.
     * <p/>
     * Note, this might end up working as if {@link org.compass.core.CompassSession#beginTransaction()}
     * was called, commit/rollback will not be called afterwards. Actually, beginTransaction might be
     * called again for the same session.
     */
    boolean tryJoinExistingTransaction(InternalCompassSession session) throws CompassException;
}
