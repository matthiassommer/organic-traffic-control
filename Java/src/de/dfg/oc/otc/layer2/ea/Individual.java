package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.layer2.gui.Layer2Console;
import org.apache.commons.math3.util.FastMath;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Abstract base class for individuals.
 *
 * @author hpr
 */
public abstract class Individual implements Comparable<Individual> {
    /**
     * A frame used to display status information.
     */
    final Layer2Console l2c = Layer2Console.getInstance();
    /**
     * The maximum cycle time is assumed to be 120 seconds.
     * The minimum cycle time depends on the intersection.
     */
    final int MAX_CYCLETIME = 120;
    /**
     * The minimum values for this individual's genes.
     */
    int[] constraints;
    /**
     * Genotype: x0 codes the cycle length while xi, i = 1..n, represents the duration of phase i.
     */
    double[] genes;
    /**
     * Reference to the EA this individual belongs to.
     */
    EA ea;
    /**
     * Self-adaptation (see [ES07]).
     */
    double sigma;
    /**
     * Proportionality constant for self-adaptation (see [ES07]); usually
     * inversely proportional to the square root of the problem size.
     */
    double tau;
    float fitness = Float.NaN;
    /**
     * The number of evaluations the individual's fitness is based on.
     */
    private int fitnessCounter;
    /**
     * The generation when the fitness was last evaluated.
     */
    private long lastFitnessEvalWithSeed = -1;

    /**
     * Recalculates the fitness of this individual.
     *
     * @return the fitness of this individual
     */
    protected abstract double calculateFitness();

    /**
     * Returns a clone of this individual.
     *
     * @return a clone of this individual
     */
    protected abstract Individual clone() throws CloneNotSupportedException;

    /**
     * Returns the minimum cycle time for the intersection (i.e., the sum of
     * minimum phase durations and interphase durations).
     * <p>
     * Min. cycle = interphase durations + min. phase durations
     *
     * @return the minimum cycle time for the intersection
     */
    int getMinimalCycletime() {
        return ((EAInternalFTC) ea).getReferenceInterphasesDuration() + getSumOfMinimalPhaseDurations();
    }

    /**
     * Sums the minimum phase durations of all phases (not including
     * interphases).
     *
     * @return the minimum phase durations of all phases
     */
    private int getSumOfMinimalPhaseDurations() {
        int sumOfMinPhaseDurations = 0;
        for (int i = 1; i < genes.length; i++) {
            sumOfMinPhaseDurations += constraints[i];
        }

        return sumOfMinPhaseDurations;
    }

    /**
     * Returns {@code 1} if the fitness of the individual given as
     * parameter is better than the fitness of this individual, {@code -1}
     * if it is worse, and {@code 0} if it is the same.
     *
     * @return {@code 1}, {@code -1}, {@code 0} (see above)
     */
    public final int compareTo(@NotNull final Individual individual) {
        if (ea.getAttribute().isInverted()) {
            // Minimization
            // Fitness recalculation necessary?
            if (!ea.isFixedSeedForEvaluation() && ea.isUseAvgFitness() && lastFitnessEvalWithSeed != ea
                    .getAimsunSeed() || Double.isNaN(fitness)) {
                calculateFitness();
            }

            if (!ea.isFixedSeedForEvaluation() && ea.isUseAvgFitness() && individual.lastFitnessEvalWithSeed != ea
                    .getAimsunSeed() || Double.isNaN(individual.fitness)) {
                individual.calculateFitness();
            }

            if (individual.fitness < fitness) {
                return 1;
            } else if (individual.fitness == fitness) {
                return 0;
            }
            return -1;
        } else {
            // Maximization
            // Fitness recalculation necessary?
            if (!ea.isFixedSeedForEvaluation() && ea.isUseAvgFitness() && lastFitnessEvalWithSeed != ea
                    .getAimsunSeed() || Double.isNaN(fitness)) {
                calculateFitness();
            }

            if (!ea.isFixedSeedForEvaluation() && ea.isUseAvgFitness() && lastFitnessEvalWithSeed != ea
                    .getAimsunSeed() || Double.isNaN(individual.fitness)) {
                individual.calculateFitness();
            }

            if (individual.fitness > fitness) {
                return 1;
            } else if (individual.fitness == fitness) {
                return 0;
            }
            return -1;
        }
    }

    /**
     * Performs a one point crossover of this individual with the individual
     * given as parameter.
     * @return the individual resulting from the crossover
     */
    protected abstract Individual discreteRecombination(Individual partner);

    /**
     * Returns {@code true} if genes given as parameter equal the genes of
     * this individual.
     *
     * @param otherIndividual genes of an other individual
     * @return {@code true} if genes given as parameter equal the genes of
     * this individual.
     */
    @Override
    public final boolean equals(final Object otherIndividual) {
        // Check for null object
        if (otherIndividual == null) {
            return false;
            // Check object type
        } else if (!(otherIndividual instanceof Individual)) {
            return false;
            // Compare individuals
        } else {
            final double[] otherGenes = ((Individual) otherIndividual).genes;

            if (genes.length != otherGenes.length) {
                return false;
            }

            for (int i = 0; i < otherGenes.length; i++) {
                if (genes[i] != otherGenes[i]) {
                    return false;
                }
            }

            return true;
        }
    }

    final void setFitness(final float fitness) {
        if (!ea.isUseAvgFitness() || Float.isNaN(this.fitness)) {
            this.fitness = fitness;
        } else {
            this.fitness = (fitnessCounter * this.fitness + fitness) / (fitnessCounter + 1);
        }

        this.fitnessCounter++;
        this.lastFitnessEvalWithSeed = ea.getAimsunSeed();
    }

    final double[] getGenes() {
        return genes;
    }

    final int getSize() {
        return genes.length;
    }

    /**
     * Mutates this individual.
     */
    final void mutate() {
        final double mutationStep = ea.getConfig().getMutationStep();
        if (mutationStep == 0) {
            // Self-adaptation
            // mutate self-adaptation parameter sigma
            sigma *= FastMath.exp(tau * ea.getRandomNumberGenerator().nextGaussian(0, 1));
        } else {
            // Fixed step size
            sigma = mutationStep;
        }

        // Check whether a fixed cycle time is required
        int k = 0;
        if (this.constraints[0] != 0) {
            // mutate only phase durations
            k = 1;
        }

        for (int i = k; i < genes.length; i++) {
            // Eiben, Smith, Sect. 4.4.1: N(0,\tau) = \tau * N(0,1)
            genes[i] += sigma * ea.getRandomNumberGenerator().nextGaussian(0, 1);
            // genes[i] <= 1
            genes[i] = Math.min(genes[i], 1);
            // genes[i] >= 0
            genes[i] = Math.max(genes[i], 0);
        }

        resetFitness();
    }

    /**
     * Calculates the level of service (LoS) for a traffic node. The id of the
     * relevant replication and the node's turnings (defined by the ids of their
     * start and end section) are needed as parameter for this method.
     *
     * @param rid        the id of the relevant replication
     * @param turningIds an array containing the node's turnings (defined by the ids of
     *                   their start and end section)
     * @return the calculated LoS
     */
    final float getFitness(final int rid, final int[] turningIds) {
        return ea.readFitnessFromDB(rid, turningIds);
    }

    /**
     * Resets the fitness of this individual to {@code Double.NaN}.
     */
    final void resetFitness() {
        this.fitness = Float.NaN;
        this.fitnessCounter = 0;
        this.lastFitnessEvalWithSeed = -1;
    }

    /**
     * Resets the individual's genes to random values.
     */
    protected abstract void resetGenesToRandomValues();

    @Override
    public String toString() {
        return Arrays.toString(genes) + " --- " + fitness;
    }
}
