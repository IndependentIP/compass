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

import java.io.Reader;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.CompassTransaction.TransactionIsolation;
import org.compass.core.config.CompassSettings;

/**
 * Helper class that simplifies the Compass access code using the template
 * design pattern.
 * <p>
 * The central method is "execute", supporting Compass code implementing the
 * CompassCallback interface. It provides Compass Session handling such that
 * neither the CompassCallback implementation nor the calling code needs to
 * explicitly care about retrieving/closing Compass Sessions, handling Session
 * lifecycle exceptions, or managing transactions. The template code is similar
 * to
 *
 * <pre>
 * CompassSession session = compass.openSession();
 * CompassTransaction tx = null;
 * try {
 * 	tx = session.beginTransaction();
 * 	Object result = compassCallback.doInCompass(session);
 * 	tx.commit();
 * 	return result;
 * } catch (RuntimeException e) {
 * 	if (tx != null) {
 * 		tx.rollback();
 * 	}
 * 	throw e;
 * } finally {
 * 	session.close();
 * }
 * </pre>
 *
 * <p>
 * The template must have a Compass reference set, either using the tempalte
 * constructor or using the set method.
 * <p>
 * CompassTemplate also provides the same operations available when working with
 * CompassSession, just that they are executed using the "execute" template
 * method, which means that they enjoy it's session lifecycle and transaction
 * support.
 *
 * @author kimchy
 */
public class CompassTemplate implements CompassOperations {

    private static Log log = LogFactory.getLog(CompassTemplate.class);

    private Compass compass;

    private CompassSettings globalSessionSettings = new CompassSettings();

    /**
     * Creates a new CompassTemplate instance (remember to set Compass using the
     * setCompass method).
     */
    public CompassTemplate() {

    }

    /**
     * Creates a new CompassTemplate instance, already initialized with a
     * Compass instance.
     *
     * @param compass
     */
    public CompassTemplate(Compass compass) {
        this.compass = compass;
    }

    /**
     * Sets the compass instance that will be used by the template.
     *
     * @param compass
     */
    public void setCompass(Compass compass) {
        this.compass = compass;
    }

    /**
     * Returns the compass instance used by the template.
     *
     * @return the compass instance
     */
    public Compass getCompass() {
        return compass;
    }

    /**
     * Executes the compass callback within a session and a transaction context.
     *
     * @param action The action to execute witin a compass transaction
     * @return An object as the result of the compass action
     * @throws CompassException
     */
    public Object execute(CompassCallback action) throws CompassException {
        return execute(null, action);
    }

    /**
     * Executes the compass callback within a session and a transaction context.
     * Applies the given transaction isolation level.
     *
     * @param transactionIsolation The transaction isolation
     * @param action               The action to execute witin a compass transaction
     * @return An object as the result of the compass action
     * @throws CompassException
     */
    public Object execute(TransactionIsolation transactionIsolation, CompassCallback action) throws CompassException {
        CompassSession session = compass.openSession();
        session.getSettings().addSettings(globalSessionSettings);
        CompassTransaction tx = null;
        try {
            tx = session.beginTransaction(transactionIsolation);
            Object result = action.doInCompass(session);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception e1) {
                    log.error("Failed to rollback transaction, ignoring", e1);
                }
            }
            throw e;
        } catch (Error err) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception e1) {
                    log.error("Failed to rollback transaction, ignoring", e1);
                }
            }
            throw err;
        } finally {
            session.close();
        }
    }

    /**
     * A helper execute method for find operations.
     *
     * @param action the callback to execute.
     * @return The hits that match the query
     * @throws CompassException
     */
    public CompassHitsOperations executeFind(CompassCallback action) throws CompassException {
        return (CompassHitsOperations) execute(action);
    }

    // Compass Operations

    public CompassSettings getSettings() {
        throw new CompassException("getSettings should not be used with CompassTemplate. Either use getGlobalSettings or execute");
    }

    public void create(final Object obj) throws CompassException {
        execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                session.create(obj);
                return null;
            }
        });
    }

    public void create(final String alias, final Object obj) throws CompassException {
        execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                session.create(alias, obj);
                return null;
            }
        });
    }

    public Property createProperty(final String name, final Reader value) throws CompassException {
        return (Property) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.createProperty(name, value);
            }
        });
    }

    public Property createProperty(final String name, final byte[] value, final Property.Store store)
            throws CompassException {
        return (Property) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.createProperty(name, value, store);
            }
        });
    }

    public Property createProperty(final String name, final Reader value, final Property.TermVector termVector)
            throws CompassException {
        return (Property) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.createProperty(name, value, termVector);
            }
        });
    }

    public Property createProperty(final String name, final String value, final Property.Store store,
                                   final Property.Index index) throws CompassException {
        return (Property) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.createProperty(name, value, store, index);
            }
        });
    }

    public Property createProperty(final String name, final String value, final Property.Store store,
                                   final Property.Index index, final Property.TermVector termVector) throws CompassException {
        return (Property) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.createProperty(name, value, store, index, termVector);
            }
        });
    }

    public Resource createResource(final String alias) throws CompassException {
        return (Resource) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.createResource(alias);
            }
        });
    }

    public void delete(final Object obj) throws CompassException {
        execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                session.delete(obj);
                return null;
            }
        });
    }

    public void delete(final Resource resource) throws CompassException {
        execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                session.delete(resource);
                return null;
            }
        });
    }

    public void delete(final Class clazz, final Object obj) throws CompassException {
        execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                session.delete(clazz, obj);
                return null;
            }
        });
    }

    public void delete(final String alias, final Object obj) throws CompassException {
        execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                session.delete(alias, obj);
                return null;
            }
        });
    }

    public void delete(final CompassQuery query) throws CompassException {
        execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                session.delete(query);
                return null;
            }
        });
    }

    public CompassHits find(final String query) throws CompassException {
        return (CompassHits) executeFind(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.find(query);
            }
        });
    }

    public CompassDetachedHits findWithDetach(final String query) throws CompassException {
        return (CompassDetachedHits) executeFind(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.find(query).detach();
            }
        });
    }

    public CompassDetachedHits findWithDetach(final String query, final int from, final int size)
            throws CompassException {
        return (CompassDetachedHits) executeFind(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.find(query).detach(from, size);
            }
        });
    }

    public Object get(final Class clazz, final Serializable id) throws CompassException {
        return execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.get(clazz, id);
            }
        });
    }

    public Object get(final String alias, final Serializable id) throws CompassException {
        return execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.get(alias, id);
            }
        });
    }

    public Resource getResource(final Class clazz, final Serializable id) throws CompassException {
        return (Resource) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.getResource(clazz, id);
            }
        });
    }

    public Resource getResource(final String alias, final Serializable id) throws CompassException {
        return (Resource) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.getResource(alias, id);
            }
        });
    }

    public Object load(final Class clazz, final Serializable id) throws CompassException {
        return execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.load(clazz, id);
            }
        });
    }

    public Object load(final String alias, final Serializable id) throws CompassException {
        return execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.load(alias, id);
            }
        });
    }

    public Resource loadResource(final Class clazz, final Serializable id) throws CompassException {
        return (Resource) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.loadResource(clazz, id);
            }
        });
    }

    public Resource loadResource(final String alias, final Serializable id) throws CompassException {
        return (Resource) execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                return session.loadResource(alias, id);
            }
        });
    }

    public void save(final Object obj) throws CompassException {
        execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                session.save(obj);
                return null;
            }
        });
    }

    public void save(final String alias, final Object obj) throws CompassException {
        execute(new CompassCallback() {
            public Object doInCompass(CompassSession session) throws CompassException {
                session.save(alias, obj);
                return null;
            }
        });
    }

    public void evict(final Object obj) {
        execute(new CompassCallbackWithoutResult() {
            protected void doInCompassWithoutResult(CompassSession session) throws CompassException {
                session.evict(obj);
            }
        });
    }

    public void evict(final String alias, final Object id) {
        execute(new CompassCallbackWithoutResult() {
            protected void doInCompassWithoutResult(CompassSession session) throws CompassException {
                session.evict(alias, id);
            }
        });
    }

    public void evict(final Resource resource) {
        execute(new CompassCallbackWithoutResult() {
            protected void doInCompassWithoutResult(CompassSession session) throws CompassException {
                session.evict(resource);
            }
        });
    }

    public void evictAll() {
        execute(new CompassCallbackWithoutResult() {
            protected void doInCompassWithoutResult(CompassSession session) throws CompassException {
                session.evictAll();
            }
        });
    }
}
