package tests.manager;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import tests.testNetworks.AbstractTestNetwork;
import tests.testNetworks.MinimalNetwork;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AimsunNetworkTest {
    private static AimsunNetwork network;
    private static OTCManager manager;

    @BeforeClass
    public static void setUp() {
        manager = OTCManager.getInstance();
        AbstractTestNetwork net = new MinimalNetwork();
        net.create();
        network = manager.getNetwork();
        manager.getNetwork().getJunction(1).getNode();
    }

    @AfterClass
    public static void tearDown() {
        manager.restart();
    }

    /**
     * Test method for
     * {@link de.dfg.oc.otc.manager.aimsun.AimsunNetwork#detectorDistance(Detector, Detector)}
     * .
     */
    @Test
    public final void distanceDetector() {
        System.out.println(network);
        Detector inDetector = network.getDetectors().get(11);
        Detector outDetector = network.getDetectors().get(1);
        float distance;

        /*try {
            distance = network.detectorDistance(outDetector, inDetector);
            fail("Kein g�ltiger Weg von Detector 1 nach 11, Exception sollte geworfen werden. Ermittelte Distanz: "
                    + distance);
        } catch (OTCManagerException e) {
            e.getStackTrace();
        }

        try {
            distance = network.detectorDistance(outDetector, outDetector);
            assertEquals("Distanz von Detector 1 zu sich selbst", outDetector.getLength(), distance, 0.1f);
        } catch (OTCManagerException e) {
            fail("In- und Out-Detector identisch, es sollte keine Exception geworfen werden.");
        }

        try {
            distance = network.detectorDistance(inDetector, outDetector);
            assertEquals("Distanz von Detector 11 zu Detector 1", 10.2f, distance, 0.1f);
        } catch (OTCManagerException e) {
            fail("G�ltiger Weg von Detector 11 nach 1, hier sollte keine Exception geworfen werden.");
        }

        outDetector = network.getDetectors().get(2);
        try {
            distance = network.detectorDistance(inDetector, outDetector);
            fail("Kein g�ltiger Weg von Detector 11 nach 2, Exception sollte geworfen werden. Ermittelte Distanz: "
                    + distance);
        } catch (OTCManagerException e) {
            e.getStackTrace();
        }*/
    }

    /**
     * Test method for
     * {@link de.dfg.oc.otc.manager.aimsun.AimsunNetwork#sectionDistance(Section, Section)}
     */
    @Test
    public final void sectionDistance() {
        Section inSection = network.getSection(1);
        Section outSection = network.getSection(21);
        float distance;

        try {
            distance = network.sectionDistance(inSection, outSection);
            fail("Kein g�ltiger Weg von Section 1 nach 21, Exception sollte geworfen werden. Ermittelte Distanz: "
                    + distance);
        } catch (OTCManagerException e) {
            e.getStackTrace();
        }

        outSection = network.getSection(11);
        try {
            distance = network.sectionDistance(inSection, outSection);
            fail("Kein g�ltiger Weg von Section 1 nach 11, Exception sollte geworfen werden. Ermittelte Distanz: "
                    + distance);
        } catch (OTCManagerException e) {
            e.getStackTrace();
        }

        inSection = network.getSection(21);
        outSection = network.getSection(1);
        try {
            distance = network.sectionDistance(inSection, outSection);
            assertEquals("Distanz von Section 11 zu Section 1", 20f, distance, 0.1f);
        } catch (OTCManagerException e) {
            fail("G�ltiger Weg von Section 21 nach 1, hier sollte keine Exception geworfen werden.");
        }

        inSection = network.getSection(21);
        outSection = network.getSection(13);
        try {
            distance = network.sectionDistance(inSection, outSection);
            assertEquals("Distanz von Section 21 zu Section 13", 30f, distance, 0.1f);
        } catch (OTCManagerException e) {
            fail("G�ltiger Weg von Section 21 nach 13, hier sollte keine Exception geworfen werden.");
        }
    }
}
