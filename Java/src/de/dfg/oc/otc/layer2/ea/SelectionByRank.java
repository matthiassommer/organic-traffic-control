package de.dfg.oc.otc.layer2.ea;

import org.apache.commons.math3.util.FastMath;

/**
 * Implements a rank-based selection operator.
 *
 * @author hpr
 */
public class SelectionByRank extends AbstractSelection {
    protected SelectionByRank(final EA ea) {
        super(ea);
    }

    /**
     * Determines the index of a selected individual.
     *
     * @param factor a factor influencing the selection
     * @return the index of a selected individual
     */
    private int getIndex(final int factor) {
        int temp = 0;
        final int size = this.ea.population.size();

        for (int i = 0; i < size; i++) {
            temp += (int) FastMath.pow(size + 1 - i, factor);
        }

        int pos = this.ea.getRandomNumberGenerator().nextInt(0, temp - 1);

        int index = 0;
        while (pos > FastMath.pow(size + 1 - index, factor)) {
            pos -= (int) FastMath.pow(size + 1 - index, factor);
            index++;
        }

        return index;
    }

    /**
     * Implements the selection operator. Two individuals are selected based on
     * their rank.
     *
     * @return an array containing the selected individuals
     */
    @Override
    protected final Individual[] selectParents() {
        final Individual[] parents = new Individual[2];
        parents[0] = this.ea.population.get(getIndex(1));
        parents[1] = this.ea.population.get(getIndex(1));
        return parents;
    }
}
