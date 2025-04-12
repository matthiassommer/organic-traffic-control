package tests.manager;

import de.dfg.oc.otc.layer1.observer.DetectorObserver;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import de.dfg.oc.otc.manager.aimsun.detectors.SubDetector;
import tests.tlc.NetworkGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OTCObserverTest {
    private static int detectorId;
    private static AimsunNetwork network;
    private static DetectorObserver networkObserver;
    private static SubDetector subDetector;

    @BeforeClass
    public static void setUp() {
        try {
            network = NetworkGenerator.generateTestNetwork(3);
        } catch (Exception e) {
            e.printStackTrace();
        }
        networkObserver = network.getJunction(1).getNode().getL1DetectorObserver();
        Detector detector = network.getDetectors().values().iterator().next();
        detectorId = detector.getId();
    }

    @Test
    public final void addValue() throws Exception {
        float averageValue;
        try {
            averageValue = networkObserver.getAverageValue(detectorId, DetectorCapabilities.COUNT, 30.0f);
            fail("OTCManagerException mu� geworfen werden, da noch keine Werte angegeben wurden.");
        } catch (OTCManagerException e) {
            e.getStackTrace();
        }

        Detector detector = network.getDetectors().get(subDetector.getDetectorIdentifier());
        float[] values = new float[DetectorCapabilities.NUM];
        for (int i = 0; i < DetectorCapabilities.NUM; i++) {
            values[i] = 1;
        }
        detector.setValues(1, values);
        averageValue = networkObserver.getAverageValue(detectorId, DetectorCapabilities.COUNT, 30.0f);
        assertEquals("Durchschnitt sollte 1.0 sein.", averageValue, 1, 0.01f);

        for (int i = 0; i < DetectorCapabilities.NUM; i++) {
            values[i] = 2;
        }
        detector.setValues(2f, values);
        averageValue = networkObserver.getAverageValue(detectorId, DetectorCapabilities.COUNT, 30.0f);
        assertEquals("Durchschnitt sollte 1.5 sein.", 1.5f, averageValue, 0.01f);

        // networkObserver.setAverageTime(10f);
        averageValue = networkObserver.getAverageValue(detectorId, DetectorCapabilities.COUNT, 30.0f);
        assertEquals("Durchschnitt sollte unver�ndert 1.5 sein.", 1.5f, averageValue, 0.01f);

        try {
            averageValue = networkObserver.getAverageValue(1000, DetectorCapabilities.COUNT, 30.0f);
            fail("OTCManagerException mu� geworfen werden, da kein SubDetector mit der �bergebenen ID existiert.");
        } catch (OTCManagerException e) {
            e.getStackTrace();
        }

        for (int i = 3; i < 600; i++) {
            for (int j = 0; j < DetectorCapabilities.NUM; j++) {
                values[j] = (float) i;
            }
            detector.setValues((float) i, values);
            System.out.println(i + ", " + values[0] + " --> "
                    + networkObserver.getAverageValue(detectorId, DetectorCapabilities.COUNT, 30.0f));
        }

        averageValue = networkObserver.getAverageValue(detectorId, DetectorCapabilities.COUNT, 30.0f);
        float expectedValue = 599f * (599f + 1f) / (2f * 599f);
        assertEquals("Durchschnitt sollte " + expectedValue + " sein.", expectedValue, averageValue, 0.01f);

        for (int i = 600; i < 610; i++) {
            for (int j = 0; j < DetectorCapabilities.NUM; j++) {
                values[j] = (float) i;
            }
            detector.setValues((float) i, values);
            System.out.println(i + ", " + values[0] + " --> "
                    + networkObserver.getAverageValue(detectorId, DetectorCapabilities.COUNT, 30.0f));
        }

        averageValue = networkObserver.getAverageValue(detectorId, DetectorCapabilities.COUNT, 30.0f);
        expectedValue = (609f * (609f + 1f) / 2f - 8f * (8f + 1f) / 2f) / 601f;
        assertEquals("Durchschnitt sollte " + expectedValue + " sein.", expectedValue, averageValue, 0.01f);
    }
}
