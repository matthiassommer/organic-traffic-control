package tests.manager;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.gui.MainFrame;
import tests.testNetworks.AbstractTestNetwork;
import tests.testNetworks.MinimalNetwork;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OTCManagerTest {
    private static OTCManager manager;

    @BeforeClass
    public static void setUp() {
        manager = OTCManager.getInstance();
        Assume.assumeNotNull(manager);

        AbstractTestNetwork net = new MinimalNetwork();
        net.create();

        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);
    }

    @Test
    public final void testConstructor() {
        assertEquals(0, manager.getTime(), 0);

        Assume.assumeTrue(manager.getException() == null);
        Assume.assumeNotNull(manager.getWarning());
        Assume.assumeNotNull(manager.getInfo());
    }
}
