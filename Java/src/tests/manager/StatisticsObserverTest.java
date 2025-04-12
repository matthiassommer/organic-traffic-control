package tests.manager;

import de.dfg.oc.otc.layer1.observer.AbstractObserver;
import de.dfg.oc.otc.layer1.observer.StatisticsObserver;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticalDataValue;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticsCapabilities;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import de.dfg.oc.otc.manager.aimsun.TrafficType;
import de.dfg.oc.otc.manager.aimsun.Turning;
import tests.testNetworks.SimpleJunction;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class StatisticsObserverTest {
    private static final int NUMBERARMS = 4;
    private static AimsunNetwork network;
    private static OTCManager manager;
    private static StatisticsObserver statObserver;
    private static SimpleJunction net = new SimpleJunction();

    @BeforeClass
    public static void setUp() {
        manager = OTCManager.getInstance();

        net.create(NUMBERARMS);

        network = manager.getNetwork();
        OTCNode node = manager.getNetwork().getJunction(1).getNode();
        statObserver = node.getL1StatObserver();
    }

    @AfterClass
    public static void tearDown() {
        manager.restart();
    }

    /**
     * Test method for
     * {@link StatisticsObserver#getAverageValue(int, int, float)}
     * .
     */
    @Test
    public final void getAverageValueIntIntFloat() {
        Turning turning = network.getJunction(1).getTurnings(TrafficType.ALL).get(0);
        float[] values = new float[StatisticsCapabilities.NUM];
        int turningId = turning.getId();

        for (int i = 0; i <= 30; i++) {
            net.incrementTime();
            float time = manager.getTime();
            values[StatisticsCapabilities.FLOW] = 1;
            values[StatisticsCapabilities.DELAYTIME] = 20;
            values[StatisticsCapabilities.STOPTIME] = 5;
            values[StatisticsCapabilities.TRAVELTIME] = 40;
            values[StatisticsCapabilities.QUEUELENGTH] = 5;
            StatisticalDataValue sdv = new StatisticalDataValue(time, values);
            statObserver.update(turning, sdv);
        }

        float average = statObserver.getAverageValue(turningId, StatisticsCapabilities.FLOW, 30);
        assertEquals("Flow", 3600, average, 0.1f);

        average = statObserver.getAverageValue(turningId, StatisticsCapabilities.DELAYTIME, 30);
        assertEquals("Flow", 20, average, 0.1f);
    }

    /**
     * Test method for
     * {@link AbstractObserver#getAverageValue(int, int, int)}.
     */
    @Test
    public final void getAverageValueIntIntInt() {
        Turning turning = network.getJunction(1).getTurnings(TrafficType.ALL).get(0);
        float[] values = new float[StatisticsCapabilities.NUM];
        int turningId = turning.getId();

        for (int i = 0; i <= 30; i++) {
            net.incrementTime();
            float time = manager.getTime();
            values[StatisticsCapabilities.FLOW] = 1;
            values[StatisticsCapabilities.DELAYTIME] = 20;
            values[StatisticsCapabilities.STOPTIME] = 5;
            values[StatisticsCapabilities.TRAVELTIME] = 40;
            values[StatisticsCapabilities.QUEUELENGTH] = 5;
            StatisticalDataValue sdv = new StatisticalDataValue(time, values);
            statObserver.update(turning, sdv);
        }

        float average = statObserver.getAverageValue(turningId, StatisticsCapabilities.FLOW, 20);
        assertEquals("Flow", 1, average, 0.1f);

        average = statObserver.getAverageValue(turningId, StatisticsCapabilities.DELAYTIME, 20);
        assertEquals("Flow", 20f, average, 0.1f);
    }

    /**
     * Test method for
     * {@link AbstractObserver#getCurrentValue(int, int)}.
     */
    @Test
    public final void getCurrentValue() {
        Turning turning1 = network.getJunction(1).getTurnings(TrafficType.ALL).get(0);
        float[] values = new float[StatisticsCapabilities.NUM];
        net.incrementTime();
        float time = manager.getTime();

        values[StatisticsCapabilities.FLOW] = 1;
        values[StatisticsCapabilities.DELAYTIME] = 20;
        values[StatisticsCapabilities.STOPTIME] = 5;
        values[StatisticsCapabilities.TRAVELTIME] = 40;
        values[StatisticsCapabilities.QUEUELENGTH] = 5;

        StatisticalDataValue sdv = new StatisticalDataValue(time, values);
        statObserver.update(turning1, sdv);
        values[StatisticsCapabilities.FLOW] = statObserver.getCurrentValue(turning1.getId(),
                StatisticsCapabilities.FLOW);
        assertEquals("Flow", 1f, values[StatisticsCapabilities.FLOW], 0.01f);
    }

    /**
     * Test method for
     * {@link AbstractObserver#getSituation(float, int)}.
     */
    @Test
    public final void getSituationFloat() {
        List<Turning> turnings = network.getJunction(1).getTurnings(TrafficType.ALL);
        float[] values = new float[StatisticsCapabilities.NUM];

        for (int i = 0; i <= 30; i++) {
            net.incrementTime();
            float time = manager.getTime();

            for (Turning turning : turnings) {
                int turningId = turning.getId();
                values[StatisticsCapabilities.FLOW] = turningId;
                values[StatisticsCapabilities.DELAYTIME] = 20;
                values[StatisticsCapabilities.STOPTIME] = 5;
                values[StatisticsCapabilities.TRAVELTIME] = 40;
                values[StatisticsCapabilities.QUEUELENGTH] = 5;
                StatisticalDataValue sdv = new StatisticalDataValue(time, values);
                statObserver.update(turning, sdv);
            }
        }

        float[] situation = statObserver.getSituation(30, StatisticsCapabilities.FLOW);
        assertEquals("F�r jedes Turning ein Eintrag in der Situation", situation.length, turnings.size());
    }

    /**
     * Test method for
     * {@link AbstractObserver#getSituation(float, int)}.
     */
    @Test
    public final void getSituationFloatInt() {
        List<Turning> turnings = network.getJunction(1).getTurnings(TrafficType.ALL);
        float[] values = new float[StatisticsCapabilities.NUM];
        int numSteps = 30;

        assertTrue("Reset muss erfolgt sein", manager.getTime() < 1f);

        for (int i = 0; i <= numSteps; i++) {
            net.incrementTime();
            float time = manager.getTime();

            for (Turning turning : turnings) {
                int turningId = turning.getId();
                values[StatisticsCapabilities.FLOW] = turningId;
                values[StatisticsCapabilities.DELAYTIME] = 20 * turningId;
                values[StatisticsCapabilities.STOPTIME] = 5;
                values[StatisticsCapabilities.TRAVELTIME] = 40;
                values[StatisticsCapabilities.QUEUELENGTH] = 5;
                StatisticalDataValue sdv = new StatisticalDataValue(time, values);
                statObserver.update(turning, sdv);
            }
        }

        float[] situation = statObserver.getSituation(30f, StatisticsCapabilities.DELAYTIME);
        assertEquals("F�r jedes Turning ein Eintrag in der Situation", situation.length, turnings.size());
    }

    @Test
    public final void otcStatisticsObserver() {
        assertNotNull(statObserver);
    }

    /**
     * Test method for
     * {@link StatisticsObserver#update(java.util.Observable, java.lang.Object)}
     * . Testet auf kontinuierliche Werte, sowie auf korrekte Fehlererkennung,
     * wenn zu wenige Werte vorhanden sind.
     */
    @Test
    public final void update() {
        Turning turning1 = network.getJunction(1).getTurnings(TrafficType.ALL).get(0);
        Object testObject = new Object();

        try {
            statObserver.update(turning1, testObject);
            fail("�bergebenes Argument ist kein StatisticalDataValue, h�tte zu Exception f�hren m�ssen.");
        } catch (OTCManagerException ome) {
            ome.getStackTrace();
        }

        float[] values = new float[StatisticsCapabilities.NUM];
        float[] averages = new float[StatisticsCapabilities.NUM];

        for (int i = 0; i <= 10; i++) {
            net.incrementTime();
            float time = manager.getTime();
            values[StatisticsCapabilities.FLOW] = 1;
            values[StatisticsCapabilities.DELAYTIME] = 20;
            values[StatisticsCapabilities.STOPTIME] = 5;
            values[StatisticsCapabilities.TRAVELTIME] = 40;
            values[StatisticsCapabilities.QUEUELENGTH] = 5;
            StatisticalDataValue sdv = new StatisticalDataValue(time, values);
            statObserver.update(turning1, sdv);

            averages[StatisticsCapabilities.FLOW] = statObserver.getAverageValue(turning1.getId(),
                    StatisticsCapabilities.FLOW, 30);
            averages[StatisticsCapabilities.DELAYTIME] = statObserver.getAverageValue(turning1.getId(),
                    StatisticsCapabilities.DELAYTIME, 30);
            averages[StatisticsCapabilities.STOPTIME] = statObserver.getAverageValue(turning1.getId(),
                    StatisticsCapabilities.STOPTIME, 30);
            averages[StatisticsCapabilities.TRAVELTIME] = statObserver.getAverageValue(turning1.getId(),
                    StatisticsCapabilities.TRAVELTIME, 30);
            averages[StatisticsCapabilities.QUEUELENGTH] = statObserver.getAverageValue(turning1.getId(),
                    StatisticsCapabilities.QUEUELENGTH, 30);

            if (i < 3) {
                assertTrue("Noch nicht gen�gend Werte f�r einen Durchschnitt vorhanden, sollte NaN ergeben.",
                        Float.isNaN(averages[StatisticsCapabilities.FLOW]));
                assertTrue("Noch nicht gen�gend Werte f�r einen Durchschnitt vorhanden, sollte NaN ergeben.",
                        Float.isNaN(averages[StatisticsCapabilities.DELAYTIME]));
                assertTrue("Noch nicht gen�gend Werte f�r einen Durchschnitt vorhanden, sollte NaN ergeben.",
                        Float.isNaN(averages[StatisticsCapabilities.TRAVELTIME]));
                assertTrue("Noch nicht gen�gend Werte f�r einen Durchschnitt vorhanden, sollte NaN ergeben.",
                        Float.isNaN(averages[StatisticsCapabilities.STOPTIME]));
                assertTrue("Noch nicht gen�gend Werte f�r einen Durchschnitt vorhanden, sollte NaN ergeben.",
                        Float.isNaN(averages[StatisticsCapabilities.QUEUELENGTH]));
            } else {
                assertEquals("Durchschnitt f�r Flow in Durchlauf " + i, 3600 / 0.75f, averages[StatisticsCapabilities.FLOW], 0.5f);
                assertEquals("Durchschnitt f�r Traveltime in Durchlauf " + i, 40, averages[StatisticsCapabilities.TRAVELTIME], 0.5f);
                assertEquals("Durchschnitt f�r Delaytime in Durchlauf " + i, 20, averages[StatisticsCapabilities.DELAYTIME], 0.5f);
                assertEquals("Durchschnitt f�r Stoptime in Durchlauf " + i, 5, averages[StatisticsCapabilities.STOPTIME], 0.5f);
                assertEquals("Durchschnitt f�r Queuelength in Durchlauf " + i, 5, averages[StatisticsCapabilities.QUEUELENGTH], 0.5f);
            }
        }

        for (int i = 0; i < 10; i++) {
            net.incrementTime();
            float time = manager.getTime();
            values[StatisticsCapabilities.FLOW] = 2f;
            values[StatisticsCapabilities.DELAYTIME] = 40f;
            values[StatisticsCapabilities.STOPTIME] = 10f;
            values[StatisticsCapabilities.TRAVELTIME] = 80f;
            values[StatisticsCapabilities.QUEUELENGTH] = 5f;
            StatisticalDataValue sdv = new StatisticalDataValue(time, values);
            statObserver.update(turning1, sdv);
        }

        averages[StatisticsCapabilities.FLOW] = statObserver.getAverageValue(turning1.getId(),
                StatisticsCapabilities.FLOW, 30f);
        averages[StatisticsCapabilities.DELAYTIME] = statObserver.getAverageValue(turning1.getId(),
                StatisticsCapabilities.DELAYTIME, 30f);
        averages[StatisticsCapabilities.STOPTIME] = statObserver.getAverageValue(turning1.getId(),
                StatisticsCapabilities.STOPTIME, 30f);
        averages[StatisticsCapabilities.TRAVELTIME] = statObserver.getAverageValue(turning1.getId(),
                StatisticsCapabilities.TRAVELTIME, 30f);
        averages[StatisticsCapabilities.QUEUELENGTH] = statObserver.getAverageValue(turning1.getId(),
                StatisticsCapabilities.QUEUELENGTH, 30f);

        assertEquals("Durchschnitt f�r Flow", (3600 + 7200) * 0.5 / 0.75f,
                averages[StatisticsCapabilities.FLOW], 0.5f);
        assertEquals("Durchschnitt f�r Traveltime", 40, averages[StatisticsCapabilities.TRAVELTIME], 0.5f);
        assertEquals("Durchschnitt f�r Delaytime", 20, averages[StatisticsCapabilities.DELAYTIME], 0.5f);
        assertEquals("Durchschnitt f�r Stoptime", 5, averages[StatisticsCapabilities.STOPTIME], 0.5f);
        assertEquals("Durchschnitt f�r Queuelength", 5, averages[StatisticsCapabilities.QUEUELENGTH], 0.5f);
    }

    /**
     * Test method for
     * {@link StatisticsObserver#update(java.util.Observable, java.lang.Object)}
     * . Ber�cksichtigt auch Zeitpunkte, zu denen keine g�ltigen Werte anliegen
     * (Float.NaN).
     */
    @Test
    public final void update2() {
        Turning turning1 = network.getJunction(1).getTurnings(TrafficType.ALL).get(0);

        float[] values = new float[StatisticsCapabilities.NUM];
        float[] averages = new float[StatisticsCapabilities.NUM];

        for (int i = 0; i <= 10; i++) {
            net.incrementTime();
            float time = manager.getTime();
            if (i % 2 == 0) {
                values[StatisticsCapabilities.FLOW] = 1f;
                values[StatisticsCapabilities.DELAYTIME] = 20f;
                values[StatisticsCapabilities.STOPTIME] = 5f;
                values[StatisticsCapabilities.TRAVELTIME] = 40f;
            } else {
                values[StatisticsCapabilities.FLOW] = Float.NaN;
                values[StatisticsCapabilities.DELAYTIME] = Float.NaN;
                values[StatisticsCapabilities.STOPTIME] = Float.NaN;
                values[StatisticsCapabilities.TRAVELTIME] = Float.NaN;
            }
            values[StatisticsCapabilities.QUEUELENGTH] = 5f;
            StatisticalDataValue sdv = new StatisticalDataValue(time, values);
            statObserver.update(turning1, sdv);
        }

        int id = turning1.getId();
        averages[StatisticsCapabilities.FLOW] = statObserver.getAverageValue(id, StatisticsCapabilities.FLOW, 30f);
        averages[StatisticsCapabilities.DELAYTIME] = statObserver.getAverageValue(id,
                StatisticsCapabilities.DELAYTIME, 30f);
        averages[StatisticsCapabilities.STOPTIME] = statObserver.getAverageValue(id,
                StatisticsCapabilities.STOPTIME, 30f);
        averages[StatisticsCapabilities.TRAVELTIME] = statObserver.getAverageValue(id,
                StatisticsCapabilities.TRAVELTIME, 30f);

        averages[StatisticsCapabilities.QUEUELENGTH] = statObserver.getAverageValue(id,
                StatisticsCapabilities.QUEUELENGTH, 30f);

        assertEquals("Durchschnitt f�r Flow", 1800 / 0.75f, averages[StatisticsCapabilities.FLOW], 0.5f);
        assertEquals("Durchschnitt f�r Traveltime", 40, averages[StatisticsCapabilities.TRAVELTIME], 0.5f);
        assertEquals("Durchschnitt f�r Delaytime", 20, averages[StatisticsCapabilities.DELAYTIME], 0.5f);
        assertEquals("Durchschnitt f�r Stoptime", 5, averages[StatisticsCapabilities.STOPTIME], 0.5f);
        assertEquals("Durchschnitt f�r Queuelength", 5, averages[StatisticsCapabilities.QUEUELENGTH], 0.5f);
    }
}
