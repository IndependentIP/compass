package org.compass.gps.device.hibernate.transaction;

import junit.framework.TestCase;
import org.compass.core.Compass;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.config.CompassConfiguration;
import org.compass.gps.device.hibernate.HibernateSyncTransactionFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * @author kimchy
 */
public class HibernateTransactionTests extends TestCase {

    private Compass compass;

    private SessionFactory sessionFactory;

    protected void setUp() throws Exception {

        Configuration conf = new Configuration().configure("/org/compass/gps/device/hibernate/transaction/hibernate.cfg.xml")
                .setProperty(Environment.HBM2DDL_AUTO, "create");
        sessionFactory = conf.buildSessionFactory();

        // set the session factory for the Hibernate transcation factory BEFORE we construct the compass instnace
        HibernateSyncTransactionFactory.setSessionFactory(sessionFactory);

        CompassConfiguration cpConf = new CompassConfiguration()
                .configure("/org/compass/gps/device/hibernate/transaction/compass.cfg.xml");
        compass = cpConf.buildCompass();
        compass.getSearchEngineIndexManager().deleteIndex();
        compass.getSearchEngineIndexManager().verifyIndex();

    }

    protected void tearDown() throws Exception {
        compass.close();
        sessionFactory.close();
    }

    public void testInnerHibernateManagement() throws Exception {
        CompassSession session = compass.openSession();
        CompassTransaction tr = session.beginTransaction();

        // save a new instance of A
        Long id = new Long(1);
        A a = new A();
        a.id = id;
        session.save(a);

        a = (A) session.get(A.class, id);
        assertNotNull(a);

        // check that if we open a new transaction within the current one it
        // will still work
        CompassSession newSession = compass.openSession();
        assertTrue(session == newSession);
        CompassTransaction newTr = session.beginTransaction();
        a = (A) session.get(A.class, id);
        assertNotNull(a);
        // this one should not commit the jta transaction since the out
        // controlls it
        newTr.commit();
        newSession.close();

        tr.commit();

        // verify that the instance was saved
        tr = session.beginTransaction();
        a = (A) session.get(A.class, id);
        assertNotNull(a);

        tr.commit();
        session.close();
    }

    public void testOuterHibernteManagementWithCommit() throws Exception {
        Session hibernateSession = sessionFactory.getCurrentSession();
        Transaction hibernateTr = hibernateSession.beginTransaction();

        CompassSession session = compass.openSession();
        CompassTransaction tr = session.beginTransaction();
        Long id = new Long(1);
        A a = new A();
        a.id = id;
        session.save(a);
        a = (A) session.get(A.class, id);
        assertNotNull(a);
        tr.commit();
        session.close();

        CompassSession oldSession = session;
        session = compass.openSession();
        assertTrue(oldSession == session);
        tr = session.beginTransaction();
        a = (A) session.get(A.class, id);
        assertNotNull(a);
        tr.commit();
        session.close();

        hibernateTr.commit();

        session = compass.openSession();
        tr = session.beginTransaction();
        a = (A) session.get(A.class, id);
        assertNotNull(a);
        tr.commit();
        session.close();
    }

    public void testOuterUTManagementWithCommitAndNoSessionOrTransactionManagement() throws Exception {
        Session hibernateSession = sessionFactory.getCurrentSession();
        Transaction hibernateTr = hibernateSession.beginTransaction();

        CompassSession session = compass.openSession();
        Long id = new Long(1);
        A a = new A();
        a.id = id;
        session.save(a);
        a = (A) session.get(A.class, id);
        assertNotNull(a);

        CompassSession oldSession = session;
        session = compass.openSession();
        assertTrue(oldSession == session);
        a = (A) session.get(A.class, id);
        assertNotNull(a);

        hibernateTr.commit();

        // now check that things were committed
        // here we do need explicit session/transaciton mangement
        // just cause we are lazy and want to let Comapss to manage JTA
        session = compass.openSession();
        CompassTransaction tr = session.beginTransaction();
        a = (A) session.get(A.class, id);
        assertNotNull(a);
        tr.commit();
        session.close();
    }

    public void testOuterUTManagementWithRollback() throws Exception {
        Session hibernateSession = sessionFactory.getCurrentSession();
        Transaction hibernateTr = hibernateSession.beginTransaction();

        CompassSession session = compass.openSession();
        CompassTransaction tr = session.beginTransaction();
        Long id = new Long(1);
        A a = new A();
        a.id = id;
        session.save(a);
        a = (A) session.get(A.class, id);
        assertNotNull(a);
        tr.commit();
        session = compass.openSession();
        tr = session.beginTransaction();
        a = (A) session.get(A.class, id);
        assertNotNull(a);
        tr.commit();

        hibernateTr.rollback();

        session = compass.openSession();
        tr = session.beginTransaction();
        a = (A) session.get(A.class, id);
        assertNull(a);
        tr.commit();
    }
}
