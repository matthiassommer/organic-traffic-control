package de.dfg.oc.otc.layer2.ea;

import org.apache.commons.math3.random.RandomDataGenerator;

import java.util.List;

/**
 * Random selector. Selects parents randomly, ignoring their fitness.
 *
 * @author hpr
 */
public class SelectionRandom extends AbstractSelection {
    private final RandomDataGenerator random;

    SelectionRandom(final EA ea) {
        super(ea);
        random = ea.getRandomNumberGenerator();
    }

    @Override
    protected final Individual[] selectParents() {
        final Individual[] parents = new Individual[2];
        final List<Individual> population = this.ea.population;

        parents[0] = population.get(random.nextInt(0, population.size() - 1));
        parents[1] = population.get(random.nextInt(0, population.size() - 1));
        return parents;
    }
}
