package dk.statsbiblioteket.infra.workflowstatemonitor;

import junit.framework.TestCase;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.SessionImpl;
import org.hsqldb.jdbc.JDBCConnection;
import org.hsqldb.persist.HsqlProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestHibernatedStateManager extends TestCase {
    @Before
    public void setUp() throws Exception {
        fillTestDB();
    }

    @After
    public void tearDown() throws Exception {
        clearTestDB();
    }

    @Test
    public void testAddState() throws Exception {
        // Clean database
        clearTestDB();

        //Insert an element
        State state = new State();
        state.setComponent("comp");
        state.setDate(new Date(1000));
        state.setState("stat");
        HibernatedStateManager hibernatedStateManager = new HibernatedStateManager();
        hibernatedStateManager.addState("test", state);

        //Check element is inserted as expected
        assertEquals(1, hibernatedStateManager.listEntities().size());
        assertEquals(1, hibernatedStateManager.listStates().size());
        assertEquals("test", hibernatedStateManager.listEntities().get(0));
        assertEquals("stat", hibernatedStateManager.listStates().get(0).getState());
        assertEquals("comp", hibernatedStateManager.listStates().get(0).getComponent());
        assertEquals(new Date(1000), hibernatedStateManager.listStates().get(0).getDate());
    }

    @Test
    public void testListEntities() {
        // List entities
        List<String> list = new HibernatedStateManager().listEntities();

        // Check them
        assertEquals(2, list.size());
        assertTrue(list.contains("file1"));
        assertTrue(list.contains("file2"));
    }

    @Test
    public void testListStates() {
        HibernatedStateManager hibernatedStateManager = new HibernatedStateManager();

        // List all states
        List<State> states = hibernatedStateManager.listStates();

        // Check them
        assertEquals(7, states.size());
        assertTrue(contains(states, "comp1", new Date(0), "state1", "file1"));
        assertTrue(contains(states, "comp1", new Date(1000), "state2", "file1"));
        assertTrue(contains(states, "comp2", new Date(2000), "state1", "file1"));
        assertTrue(contains(states, "comp2", new Date(3000), "state2", "file1"));
        assertTrue(contains(states, "comp1", new Date(4000), "state1", "file2"));
        assertTrue(contains(states, "comp1", new Date(5000), "state2", "file2"));
        assertTrue(contains(states, "comp2", new Date(6000), "state1", "file2"));

        // List states for file1
        states = hibernatedStateManager.listStates("file1");

        // Check them
        assertEquals(4, states.size());
        assertTrue(contains(states, "comp1", new Date(0), "state1", "file1"));
        assertTrue(contains(states, "comp1", new Date(1000), "state2", "file1"));
        assertTrue(contains(states, "comp2", new Date(2000), "state1", "file1"));
        assertTrue(contains(states, "comp2", new Date(3000), "state2", "file1"));
        assertFalse(contains(states, "comp1", new Date(4000), "state1", "file2"));
        assertFalse(contains(states, "comp1", new Date(5000), "state2", "file2"));
        assertFalse(contains(states, "comp2", new Date(6000), "state1", "file2"));

        // List all state1 states
        states = hibernatedStateManager.listStates(false, Arrays.asList(new String[]{"state1"}), null);

        // Check them
        assertEquals(4, states.size());
        assertTrue(contains(states, "comp1", new Date(0), "state1", "file1"));
        assertFalse(contains(states, "comp1", new Date(1000), "state2", "file1"));
        assertTrue(contains(states, "comp2", new Date(2000), "state1", "file1"));
        assertFalse(contains(states, "comp2", new Date(3000), "state2", "file1"));
        assertTrue(contains(states, "comp1", new Date(4000), "state1", "file2"));
        assertFalse(contains(states, "comp1", new Date(5000), "state2", "file2"));
        assertTrue(contains(states, "comp2", new Date(6000), "state1", "file2"));

        // List all NOT state2 states
        states = hibernatedStateManager.listStates(false, null, Arrays.asList(new String[]{"state2"}));

        // Check them
        assertEquals(4, states.size());
        assertTrue(contains(states, "comp1", new Date(0), "state1", "file1"));
        assertFalse(contains(states, "comp1", new Date(1000), "state2", "file1"));
        assertTrue(contains(states, "comp2", new Date(2000), "state1", "file1"));
        assertFalse(contains(states, "comp2", new Date(3000), "state2", "file1"));
        assertTrue(contains(states, "comp1", new Date(4000), "state1", "file2"));
        assertFalse(contains(states, "comp1", new Date(5000), "state2", "file2"));
        assertTrue(contains(states, "comp2", new Date(6000), "state1", "file2"));

        // List all state1 and state2 states and NOT state1 (i.e. state2)
        states = hibernatedStateManager.listStates(false, Arrays.asList(new String[]{"state1", "state2"}),
                                                             Arrays.asList(new String[]{"state1"}));

        //Check them
        assertEquals(3, states.size());
        assertFalse(contains(states, "comp1", new Date(0), "state1", "file1"));
        assertTrue(contains(states, "comp1", new Date(1000), "state2", "file1"));
        assertFalse(contains(states, "comp2", new Date(2000), "state1", "file1"));
        assertTrue(contains(states, "comp2", new Date(3000), "state2", "file1"));
        assertFalse(contains(states, "comp1", new Date(4000), "state1", "file2"));
        assertTrue(contains(states, "comp1", new Date(5000), "state2", "file2"));
        assertFalse(contains(states, "comp2", new Date(6000), "state1", "file2"));

        states = hibernatedStateManager.listStates(true, null, null);
        System.out.println(states);

        //Check them
        assertEquals(2, states.size());
        assertFalse(contains(states, "comp1", new Date(0), "state1", "file1"));
        assertFalse(contains(states, "comp1", new Date(1000), "state2", "file1"));
        assertFalse(contains(states, "comp2", new Date(2000), "state1", "file1"));
        assertTrue(contains(states, "comp2", new Date(3000), "state2", "file1"));
        assertFalse(contains(states, "comp1", new Date(4000), "state1", "file2"));
        assertFalse(contains(states, "comp1", new Date(5000), "state2", "file2"));
        assertTrue(contains(states, "comp2", new Date(6000), "state1", "file2"));
    }

    private boolean contains(List<State> states, String component, Date date, String state1, String file) {
        for (State state : states) {
            if (state.getComponent().equals(component) && state.getDate().getTime() == date.getTime()
                    && state.getState().equals(state1) && state.getEntities().iterator().next().getName()
                    .equals(file)) {
                return true;
            }
        }
        return false;
    }

    private void fillTestDB() {
        HibernatedStateManager hibernatedStateManager = new HibernatedStateManager();
        State state1 = new State();
        state1.setComponent("comp1");
        state1.setDate(new Date(0));
        state1.setState("state1");
        hibernatedStateManager.addState("file1", state1);
        State state2 = new State();
        state2.setComponent("comp1");
        state2.setDate(new Date(1000));
        state2.setState("state2");
        hibernatedStateManager.addState("file1", state2);
        State state3 = new State();
        state3.setComponent("comp2");
        state3.setDate(new Date(2000));
        state3.setState("state1");
        hibernatedStateManager.addState("file1", state3);
        State state4 = new State();
        state4.setComponent("comp2");
        state4.setDate(new Date(3000));
        state4.setState("state2");
        hibernatedStateManager.addState("file1", state4);
        State state5 = new State();
        state5.setComponent("comp1");
        state5.setDate(new Date(4000));
        state5.setState("state1");
        hibernatedStateManager.addState("file2", state5);
        State state6 = new State();
        state6.setComponent("comp1");
        state6.setDate(new Date(5000));
        state6.setState("state2");
        hibernatedStateManager.addState("file2", state6);
        State state7 = new State();
        state7.setComponent("comp2");
        state7.setDate(new Date(6000));
        state7.setState("state1");
        hibernatedStateManager.addState("file2", state7);
    }

    public void clearTestDB() throws Exception {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:hsqldb:mem:testdb", "SA", "");
            try {
                Statement stmt = connection.createStatement();
                try {
                    stmt.execute("TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK");
                    connection.commit();
                } finally {
                    stmt.close();
                }
            } catch (SQLException e) {
                connection.rollback();
                throw new Exception(e);
            }
        } catch (SQLException e) {
            throw new Exception(e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /*    public static void main(String[] args) {
        TestHibernatedStateManager registrar = new TestHibernatedStateManager();

        if (args[0].equals("store")) {
            registrar.registerState(args[1], "component", "statename");
        } else if (args[0].equals("listAll")) {
            List<State> states = registrar.listStates();
            for (State theState : states) {
                System.out.println(
                        "Name: " + theState.getName() + " Time: " + theState
                                .getDate());
            }
        } else if (args[0].equals("list")) {
            List<State> states = registrar.listStates(args[1]);
            for (State theState : states) {
                System.out.println(
                        "Name: " + theState.getName() + " Time: " + theState
                                .getDate());
            }
        }

        HibernateUtil.getSessionFactory().close();
    }

    private void registerState(String name, String component,
                               String stateName) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        State state = new State();
        state.setName(name);
        state.setComponent(component);
        state.setState(stateName);
        state.setDate(new Date());

        session.save(state);
        session.getTransaction().commit();
    }

    private List<State> listStates() {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List result = session.createQuery("from State").list();
        session.getTransaction().commit();
        return result;
    }

    private List<State> listStates(String name) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List result = session
                .createQuery("from State where name = '" + name + "'").list();
        session.getTransaction().commit();
        return result;
    }*/
}