package tests.manager;

import de.dfg.oc.otc.layer0.tlc.fixedTimeController.FixedTimeController;
import de.dfg.oc.otc.layer1.Layer1Exception;
import de.dfg.oc.otc.layer1.controller.AbstractTLCSelector;
import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.layer1.observer.DetectorObserver;
import de.dfg.oc.otc.layer1.observer.StatisticsObserver;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import de.dfg.oc.otc.manager.aimsun.TrafficType;
import tests.testNetworks.SimpleJunction;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

/**
 * @author rochner
 */
public class OTCNodeTest {
    private static final int NUMBERARMS = 4;
    private static AimsunNetwork network;
    private static OTCManager manager;
    private static OTCNode node;
    private static SimpleJunction net = new SimpleJunction();

    @BeforeClass
    public static void setUp() throws Exception {
        manager = OTCManager.getInstance();
        Assume.assumeNotNull(manager);

        net.create(NUMBERARMS);

        network = manager.getNetwork();
        Assume.assumeNotNull(network);

        node = network.getJunction(1).getNode();
        Assume.assumeNotNull(node);
    }

    /**
     * Test method for
     * {@link de.dfg.oc.otc.manager.OTCNode#changeTLC(de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters)}
     */
    @Test
    public final void changeTLC() {
        AimsunJunction junction = manager.getNetwork().getJunction(1);
        Assume.assumeNotNull(junction);

        int numberArms = junction.getInSections().size();
        Assume.assumeThat(numberArms, is(NUMBERARMS));

        // F�r jeden Arm eine Phase, die alle m�glichen Turnings abdeckt.
        List<Integer> simplePhases = new ArrayList<>();
        for (int i = 0; i < numberArms; i++) {
            simplePhases.add(i, i + 1);
        }
        Assume.assumeThat(simplePhases.size(), is(NUMBERARMS));

        float[] maxOuts1 = new float[numberArms];
        for (int i = 0; i < numberArms; i++) {
            maxOuts1[i] = 20 + i;
        }

        // Zwei identische FTCs, bei deren Wechsel der jeweils andere Controller
        // in den exakt gleichen Zustand versetzt werden soll (Phase,
        // timeLastChange).
        FixedTimeController ftc1 = new FixedTimeController(maxOuts1, network.getJunction(1), simplePhases);
        Assume.assumeNotNull(ftc1);
        junction.setTrafficLightController(ftc1);

        FixedTimeController ftc2 = new FixedTimeController(maxOuts1, network.getJunction(1), simplePhases);
        Assume.assumeNotNull(ftc2);
        junction.getNode().changeTLC(ftc2.getParameters());

        // Inkrementieren der Zeit bis nach den ersten Phasenwechsel
        manager.setReplicationID(0);
        for (int i = 0; i < maxOuts1[0] + 2; i += 0.75f) {
            net.incrementTime();
        }

        int currentPhase = junction.getActiveTLC().getCurrentPhaseID();
        Assume.assumeThat("Sollte eigentlich Phase 2 sein", currentPhase, is(2));

        float timeLastChange = junction.getActiveTLC().getTimeOfLastChange();
        assertEquals(
                "Letzte �nderung sollte nach Ablauf der ersten Phase erfolgt sein",
                timeLastChange, maxOuts1[0], 0.75f);

        junction.getNode().changeTLC(ftc1.getParameters());
        currentPhase = junction.getActiveTLC().getCurrentPhaseID();
        Assume.assumeThat("Sollte eigentlich Phase 2 sein", currentPhase, is(2));

        timeLastChange = junction.getActiveTLC().getTimeOfLastChange();
        assertEquals(
                "Letzte �nderung sollte nach Ablauf der ersten Phase erfolgt sein",
                timeLastChange, maxOuts1[0], 0.75f);

        // FTC, bei dem die letzte Phase der ersten beiden FTCs in einzelne
        // Phasen aufgespalten wird, f�r jedes Turning eine eigene Phase. Somit
        // kann f�r diese Phase kein exaktes Matching des Zustands mehr
        // erfolgen.
        List<Integer> finerPhases = new ArrayList<>();

        for (int i = 0; i < numberArms - 1; i++) {
            finerPhases.add(i, i + 1);
        }

        for (int i = 0; i < numberArms - 1; i++) {
            finerPhases.add(i + numberArms - 1, numberArms * 10 + i + 1);
        }

        float[] maxOuts2 = new float[2 * numberArms - 2];

        // Phasendauern der ersten Phasen identisch zu den einfacheren FTCs.
        for (int i = 0; i < 2 * numberArms - 2; i++) {
            maxOuts2[i] = 20.0f + i;
        }

        FixedTimeController ftc3 = new FixedTimeController(maxOuts2, network.getJunction(1), finerPhases);
        float currentTime = manager.getTime();
        float timeToLastPhase = 0f;

        for (int i = 0; i < numberArms - 1; i++) {
            timeToLastPhase += maxOuts1[i];
        }

        // Inkrementieren der Zeit bis zur letzten Phase der einfacheren Controller
        for (int i = 0; i < timeToLastPhase - currentTime + 2; i += 0.75f) {
            net.incrementTime();
        }

        junction.getNode().changeTLC(ftc3.getParameters());
        currentPhase = junction.getActiveTLC().getCurrentPhaseID();
        Assume.assumeThat("Sollte eigentlich Phase " + (numberArms * 10 + 1)
                + " sein", numberArms * 10 + 1, is(currentPhase));

        timeLastChange = junction.getActiveTLC().getTimeOfLastChange();
        assertEquals("Letzte �nderung sollte zum Zeitpunkt " + timeToLastPhase
                + " erfolgt sein", timeToLastPhase, timeLastChange, 0.75f);

        // Wechsel zur�ck zu einem einfachen Controller
        junction.getNode().changeTLC(ftc2.getParameters());
        currentPhase = junction.getActiveTLC().getCurrentPhaseID();
        Assume.assumeThat("Sollte Phase " + numberArms + " sein", numberArms,
                is(currentPhase));

        timeLastChange = junction.getActiveTLC().getTimeOfLastChange();
        assertEquals("Letzte �nderung sollte zum Zeitpunkt " + timeToLastPhase
                + " erfolgt sein", timeToLastPhase, timeLastChange, 0.75f);
    }

    /**
     * Test method for
     * {@link OTCNode#getEvaluation(de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource,
     * de.dfg.oc.otc.layer1.observer.Attribute, TrafficType, boolean)}
     */
    @Test
    public final void getEvaluationForNode() {
        float evaluation = node.getEvaluation(DataSource.STATISTICS,
                Attribute.LOS, TrafficType.INDIVIDUAL_TRAFFIC, false);
        Assume.assumeTrue("Noch keine Daten, sollte NaN zur�ckgeben", Float.isNaN(evaluation));
    }

    /**
     * Test method for {@link de.dfg.oc.otc.manager.OTCNode#getId()}.
     */
    @Test
    public final void getId() {
        int id = node.getId();
        Assume.assumeThat("Id des Nodes sollte der Id der Junction entsprechen", 1,
                is(id));
    }

    /**
     * Test method for
     * {@link de.dfg.oc.otc.manager.OTCNode#getL1DetectorObserver()}.
     */
    @Test
    public final void getL1DetObserver() {
        DetectorObserver l1DetObserver = node.getL1DetectorObserver();
        if (!manager.getNetwork().getJunction(1).getDetectors().isEmpty()) {
            Assume.assumeNotNull(l1DetObserver);
        } else {
            Assume.assumeTrue(l1DetObserver == null);
        }
    }

    /**
     * Test method for {@link de.dfg.oc.otc.manager.OTCNode#getL1StatObserver()}
     * .
     */
    @Test
    public final void getL1StatObserver() {
        StatisticsObserver l1StatObserver = node.getL1StatObserver();
        Assume.assumeNotNull(l1StatObserver);
    }

    /**
     * Test method for {@link de.dfg.oc.otc.manager.OTCNode#getTLCSelector()}.
     */
    @Test
    public final void getLCS() {
        AbstractTLCSelector lcs = node.getTLCSelector();
        Assume.assumeNotNull(lcs);
    }

    /**
     * Test method for
     * {@link de.dfg.oc.otc.manager.OTCNode#OTCNode(de.dfg.oc.otc.manager.aimsun.AimsunJunction, Attribute)}
     * .
     */
    @Test(expected = Layer1Exception.class)
    public final void testOTCNode() {
        AimsunJunction unControlledJunction = new AimsunJunction(102, 0, "");
        OTCNode newNode = new OTCNode(unControlledJunction, Attribute.LOS);
        Assume.assumeNotNull(newNode);

        newNode = new OTCNode(manager.getNetwork().getJunction(1), Attribute.LOS);
        Assume.assumeNotNull(newNode);
    }
}