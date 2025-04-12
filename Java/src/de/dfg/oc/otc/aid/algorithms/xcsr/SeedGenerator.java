package de.dfg.oc.otc.aid.algorithms.xcsr;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by dmrauh on 02.03.15.
 *
 * @author rauhdomi
 *         <p>
 *         Holds seeds for the random generators. (Momentarily statically 30 true random Seeds)
 *         Or alternatively an infinite amount of pesudo random seeds.
 */
final class SeedGenerator {
    private static final int TRUE_RANDOM_SEEDS_COUNT;
    private static final Set<Long> TRUE_RANDOM_SEEDS = new LinkedHashSet<>(
            Arrays.asList(
                    830473998L,
                    403834299L,
                    238965585L,
                    408142638L,
                    942717059L,
                    281379800L,
                    368686664L,
                    810244816L,
                    294075679L,
                    773666111L,
                    589692214L,
                    185784654L,
                    316694866L,
                    934074476L,
                    848114577L,
                    600564467L,
                    546526942L,
                    647767920L,
                    340089806L,
                    532650536L,
                    960592243L,
                    189889986L,
                    985561741L,
                    714632367L,
                    999256368L,
                    528695203L,
                    240380654L,
                    591742309L,
                    808896301L,
                    204196615L
            )
    );

    static {
        TRUE_RANDOM_SEEDS_COUNT = TRUE_RANDOM_SEEDS.size();
    }

    private SeedGenerator() {
    }

    /**
     * Returns a truly random seed for a given experiment number (30 max)
     *
     * @param numberOfExperiment The number of the experiment to get the true random seed for (starting at 1)
     * @return The true random seed
     */
    static long getTrueRandomSeedForExperiment(int numberOfExperiment) {
        if (numberOfExperiment < 1) {
            throw new IllegalArgumentException("Number of Experiment has to start at 1!");
        } else if (numberOfExperiment > TRUE_RANDOM_SEEDS_COUNT) {
            throw new IllegalArgumentException("Number of Experiments too high. Generator only has "
                    + TRUE_RANDOM_SEEDS.size()
                    + " true random seeds!");
        }
        return TRUE_RANDOM_SEEDS.toArray(new Long[TRUE_RANDOM_SEEDS_COUNT])[numberOfExperiment - 1];
    }
}
