package tests.tlc;

import de.dfg.oc.otc.layer0.tlc.TLCException;
import de.dfg.oc.otc.layer0.tlc.fixedTimeController.FixedTimeController;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FixedTimeControllerTest {
    private static AimsunNetwork network1, network2;

    @BeforeClass
    public static void setUp() {
        network1 = NetworkGenerator.generateTestNetwork(3);
        Assume.assumeNotNull(network1);

        network2 = NetworkGenerator.generateTestNetwork(3);
        Assume.assumeNotNull(network2);
    }

    @Test(expected = TLCException.class)
    public final void constructor() {
        float[] phasenDauern = {2.2f, 2.3f, 3.3f, 4.0f};
        new FixedTimeController(phasenDauern, network1.getJunction(1));
    }

    @Test
    public final void constructor2() {
        float[] phasenDauern = {2.2f, 2.3f, 3.3f};
        FixedTimeController ftc = new FixedTimeController(phasenDauern, network1.getJunction(1));
        Assume.assumeNotNull(ftc);
    }

    @Test(expected = TLCException.class)
    public final void step() {
        float[] phasenDauern = {2.2f, 2.3f, 3.3f};
        FixedTimeController ftc = new FixedTimeController(phasenDauern, network2.getJunction(1));
        Assume.assumeNotNull(ftc);

        ftc.reset();
        Assume.assumeThat("FTC sollte nach init() Phase 1 aktiv haben.", ftc.getCurrentPhaseID(), is(1));
        Assume.assumeThat("FTC sollte nach init() timeLastChange 0.0f gesetzt haben.", ftc.getTimeOfLastChange(), is(0.0f));

        ftc.init(1);
        Assume.assumeThat("FTC sollte nach init(1) Phase 2 aktiv haben.", ftc.getCurrentPhaseID(), is(2));
        Assume.assumeThat("FTC sollte nach init(1) timeLastChange 0.0f gesetzt haben.", ftc.getTimeOfLastChange(), is(0.0f));

        try {
            ftc.init(3);
            fail("TLCException muß geworfen werden, da Anzahl der Phasen für init zu groß.");
        } catch (TLCException otcex) {
            otcex.getStackTrace();
        }

        ftc.init(0);
        ftc.step(2.1f);
        Assume.assumeThat("FTC sollte nach step(2.1) noch in Phase 1 sein.", ftc.getCurrentPhaseID(), is(1));
        Assume.assumeThat("FTC sollte nach step(2.1) noch timeLastChange 0.0f gesetzt haben.", ftc.getTimeOfLastChange(), is(0.0f));

        ftc.step(2.4f);
        Assume.assumeThat("FTC sollte nach step(2.4) in Phase 2 wechseln.", ftc.getCurrentPhaseID(), is(2));
        Assume.assumeThat("FTC sollte nach step(2.4) timeLastChange 2.4f gesetzt haben.", ftc.getTimeOfLastChange(), is(2.4f));

        ftc.step(24f);
        Assume.assumeThat("FTC sollte nach step(24) in Phase 3 wechseln.", ftc.getCurrentPhaseID(), is(3));
        Assume.assumeThat("FTC sollte nach step(24) timeLastChange 24f gesetzt haben.", ftc.getTimeOfLastChange(), is(24f));

        try {
            ftc.step(3.2f);
            fail("TLCException mu� geworfen werden, da Zeit kleiner als im vorangegangenen Schritt.");
        } catch (TLCException otcex) {
            otcex.getStackTrace();
        }
    }
}
