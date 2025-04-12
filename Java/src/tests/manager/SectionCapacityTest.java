package tests.manager;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.SectionCapacityComponent;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * @author Matthias Sommer.
 */
public class SectionCapacityTest {
    private static final float capacity = 1800;
    private static final float length = 500;
    private static final float speedLimit = 50;
    private static float dynamicCapacity;
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Start test -> " + description.getMethodName());
        }

        protected void finished(Description description) {
            System.out.println("Dynamic capacity: " + dynamicCapacity);
        }
    };
    private static Section section;
    private static SectionCapacityComponent capacityComponent;

    @BeforeClass
    public static void setup() {
        OTCManager.getInstance().createNetwork("");
    }

    @Test
    public void testTwoLanes() {
        section = new Section(1, 49, 2, new int[0], length, speedLimit, capacity);
        section.setNumberOfLanes(2);
    }

    @Test
    public void testOneLane() {
        section = new Section(1, 49, 2, new int[0], length, speedLimit, capacity);
        section.setNumberOfLanes(1);
    }

    @Test
    public void testZeroLanes() {
        section = new Section(1, 49, 2, new int[0], length, speedLimit, capacity);
    }

    @Test
    public void testHighway() {
        section = new Section(1, 49, 2, new int[0], length, 80, capacity);
        section.setNumberOfLanes(2);
    }

    @After
    public void run() {
        capacityComponent = new SectionCapacityComponent(section);
        dynamicCapacity = capacityComponent.getCapacity();
    }
}
