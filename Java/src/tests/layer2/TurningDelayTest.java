package tests.layer2;

import org.apache.commons.math3.util.FastMath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Test class for Webster's approximation of the average delay of a vehicle.
 *
 * @author Matthias Sommer
 */
public class TurningDelayTest {
    private static float delay;
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Start test: " + description.getMethodName());
        }

        protected void finished(Description description) {
            System.out.println("Delay: " + delay);
        }
    };
    private final int lanes = 1;
    /**
     * In veh/s.
     */
    private final double capacity = lanes * 1800;
    /**
     * Greentime.
     */
    private final double tf = 20;
    private final double cycleTime = 50;
    private final int flow = 600;

    @Test
    public void runWebster() {
        // Greentime to cycle time factor.
        final double f = tf / cycleTime;
        final double degreeOfSaturation = flow / (f * capacity);

        // Represents the delay when the traffic is assumed to be arriving uniformly.
        final double td = cycleTime * FastMath.pow(1 - f, 2)
                / (2 * (1 - flow / capacity));

        delay = (float) (0.9 * (td + capacity * FastMath.pow(degreeOfSaturation,
                2) / (flow * (1 - degreeOfSaturation))));

        assumeThat(delay, is(23.4f));
    }

    @Test
    public void runHCM2000() {
        final double x = flow / capacity;
        /**
         * Incremental delay factor dependent on signal controller setting
         * (0.50 for pretimed signals; vary between 0.04 to 0.50 for actuated controllers).
         */
        final float k = 0.4f;
        /**
         * Upstream filtering/metering adjustment factor (1.0 for an isolated intersection).
         */
        final float I = 1;
        /**
         * Uniform delay progression adjustment factor, which accounts for effects of signal
         * progression (PF = 1 because an isolated intersection is assumed).
         */
        final float PF = 1;
        /**
         * Duration of analysis period, hour.
         */
        final float T = 1;

        double d1 = 0.5 * cycleTime * FastMath.pow(1 - flow / capacity, 2);
        // incremental delay
        double d2 = 900 * T * (x - 1 + Math.sqrt(FastMath.pow(x - 1, 2) + (8 * k * I * x) / (capacity * T)));
        /**
         * Initial queue delay, which accounts for delay to all vehicles in analysis period due to
         * an initial queue at the start of analysis period, s/veh.
         */
        double d3 = 1;

        delay = (float) (d1 * PF + d2 + d3);
    }
}
