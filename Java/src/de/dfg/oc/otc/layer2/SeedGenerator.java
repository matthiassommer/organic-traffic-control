package de.dfg.oc.otc.layer2;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManager;
import org.apache.commons.math3.random.RandomDataGenerator;

/**
 * Provides random seeds for different runs of the evolutionary algorithm.
 *
 * @author hpr
 */
public abstract class SeedGenerator {
    private static long masterSeed;
    private static RandomDataGenerator rand;

    private static void initRandomGenerator() {
        if (rand == null) {
            masterSeed = OTCManager.getInstance().getSystemSeed();
            if (masterSeed == 0) {
                masterSeed = System.currentTimeMillis();
            }
            rand = new RandomDataGenerator();
            if (DefaultParams.L2_MASTERSEED == 1) {
                rand.reSeed(OTCManager.getInstance().getSystemSeed());
            }
        }
    }

    /**
     * Return a new random seed for the EA.
     *
     * @return a new random seed
     */
    public static long getNextSeed() {
        initRandomGenerator();
        return rand.nextLong(0, 1);
    }
}