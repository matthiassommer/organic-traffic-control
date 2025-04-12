package tests.tlc;

import de.dfg.oc.otc.layer0.tlc.TLCException;
import de.dfg.oc.otc.layer0.tlc.Recall;
import de.dfg.oc.otc.layer0.tlc.fixedTimeController.FixedTimeRecallController;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class FixedTimeRecallControllerTest {
    private static AimsunNetwork network1;
    private static AimsunNetwork network2;
    private final float[] phasenDauern = {2.2f, 2.3f, 3.3f};
    private static Recall[] recalls;

    @BeforeClass
    public static void setUp() throws Exception {
        network1 = NetworkGenerator.generateTestNetwork(3);
        network2 = NetworkGenerator.generateTestNetwork(3);
        recalls = new Recall[3];
        recalls[0] = Recall.max;
        recalls[1] = Recall.min;
        recalls[2] = Recall.no;
    }

    @Test
    public final void constructor() throws Exception {
        try {
            float[] phasenDauern = {2.2f, 2.3f, 3.3f, 4.0f};
            FixedTimeRecallController ftrc1 = new FixedTimeRecallController(phasenDauern, network1.getJunction(1), recalls);
            fail("TLCException mu� geworfen werden, da Anzahl der Phasen nicht mit der Anzahl der Phasendauern �bereinstimmt");
        } catch (TLCException otcex) {
            otcex.getStackTrace();
        }

        FixedTimeRecallController ftrc11;
        try {
            network1.setDetectorCapabilities(1, false, false, false, false, false, false, false);
            network1.initSubDetectors();
            network1.initJunctions();
            ftrc11 = new FixedTimeRecallController(phasenDauern, network1.getJunction(1), recalls);
            fail("TLCException mu� geworfen werden, da eine Phase �ber keinen Presence Detektor verf�gt.");
        } catch (TLCException otcex) {
            network1.setDetectorCapabilities(1, true, true, false, false, true, false, false);
            network1.initSubDetectors();
            network1.initJunctions();
        }

        ftrc11 = new FixedTimeRecallController(phasenDauern, network1.getJunction(1), recalls);
        assertNotNull("Konstruktor gibt ein Objekt ungleich null zur�ck", ftrc11);
    }

    @Test
    public final void step() {
        FixedTimeRecallController ftrc2 = new FixedTimeRecallController(phasenDauern, network2.getJunction(1), recalls);
        ftrc2.reset();
        assertEquals("FTRC sollte nach init() Phase 1 aktiv haben.", ftrc2.getCurrentPhaseID(), 1);
        assertEquals("FTRC sollte nach init() timeLastChange 0.0f gesetzt haben.", ftrc2.getTimeOfLastChange(), 0,
                0.01f);

        ftrc2.init(1);
        assertEquals("FTRC sollte nach init(1) Phase 1 aktiv haben.", ftrc2.getCurrentPhaseID(), 1);
        assertEquals("FTRC sollte nach init(1) timeLastChange 0.0f gesetzt haben.", ftrc2.getTimeOfLastChange(), 0,
                0.01f);

        try {
            ftrc2.init(4);
            fail("TLCException mu� geworfen werden, da Anzahl der Phasen f�r init zu gro�.");
        } catch (TLCException otcex) {
            otcex.getStackTrace();
        }

        ftrc2.init(1);
        ftrc2.step(2.1f);
        assertEquals("FTRC sollte nach step(2.1) noch in Phase 1 sein.", ftrc2.getCurrentPhaseID(), 1);
        assertEquals("FTRC sollte nach step(2.1) noch timeLastChange 0.0f gesetzt haben.", ftrc2.getTimeOfLastChange(),
                0, 0.01f);

        ftrc2.step(2.4f);
        assertEquals("FTRC sollte nach step(2.4) in Phase 2 wechseln.", ftrc2.getCurrentPhaseID(), 2);
        assertEquals("FTRC sollte nach step(2.4) timeLastChange 2.2f gesetzt haben.", ftrc2.getTimeOfLastChange(),
                2.2f, 0.01f);

        ftrc2.step(6);
        assertEquals(
                "FTRC sollte nach step(6) in Phase 1 wechseln, da Phase 3 no recall ist und der Detektor nicht geschaltet ist.",
                ftrc2.getCurrentPhaseID(), 1);
        assertEquals("FTRC sollte nach step(6) timeLastChange 6 gesetzt haben.", ftrc2.getTimeOfLastChange(), 6, 0.01f);
    }
}
