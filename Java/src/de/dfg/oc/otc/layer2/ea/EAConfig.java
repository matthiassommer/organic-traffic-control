package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer2.SeedGenerator;
import de.dfg.oc.otc.manager.OTCManager;

import java.io.Serializable;

/**
 * Collects the configuration of the EA.
 *
 * @author hpr
 */
@SuppressWarnings("serial")
public class EAConfig implements Serializable {
    private final boolean reevaluateSurvivors = false;
    private final int seedChangeAfterXGens = 1;
    /**
     * Plus or comma selection.
     */
    private boolean commaStrategy;
    /**
     * Probability that the crossover operator is used during the creation of a
     * new individual.
     */
    private double crossoverProb;
    /**
     * Determines whether the random seed used in evaluation runs is kept
     * constant ({@code true}) or whether it is changed each generation (
     * {@code false}).
     */
    private boolean fixedSeedForEvaluation;
    private int maxGenerations;
    /**
     * Probability that the mutation operator is used during the creation of a
     * new individual.
     */
    private double mutationProb;
    /**
     * Step size (std. dev.) for mutation operator.
     */
    private double mutationStepSize;
    /**
     * The number of children created for each generation.
     */
    private int numberOfChildren;
    private int populationSize;
    /**
     * RandomSeed for the EA; always use setRandSeedEA the set this value!
     */
    private long randomSeedEA;
    private int reevaluationDuration = 5400;
    /**
     * Simulations duration (in seconds) used during evaluations.
     */
    private int simulationDuration;

    /**
     * Determines whether the fitness is calculated based on the current random
     * seed only or as average of several seeds. The parameter is only relevant
     * if {@code fixedSeedForEvaluation} is {@code false}.
     */
    private boolean useAverageFitness;

    /**
     * Determines whether the fitness is calculated based on AIMSUN simulation
     * only or whether Webster approximations are used during the optimisation.
     */
    private boolean useWebster;

    /**
     * Warm-up duration (in seconds) used during evaluations.
     */
    private int warmUpDuration;
    /**
     * Simulations duration (in seconds) for the last evaluation run. Should be longer than the normal one.
     */
    private int lastRunSimulationDuration;

    public EAConfig() {
        initialiseDefaultParams();
    }

    /**
     * Creates a configuration using the given parameters.
     *
     * @param simDur                 simulation duration
     * @param warmDur                warm-up duration
     * @param popSize                population size
     * @param maxGens                number of generations
     * @param numChild               number of children
     * @param fixedSeedForEvaluation
     * @param randSeedEA             random seed for the EA (0 = use seed from seed generator)
     */
    public EAConfig(final int simDur, final int warmDur, final int popSize, final int maxGens, final int numChild,
                    final long randSeedEA, final boolean fixedSeedForEvaluation, final boolean useAvgFitness,
                    final boolean useWebster) {
        this.simulationDuration = simDur;
        this.warmUpDuration = warmDur;
        this.populationSize = popSize;
        this.maxGenerations = maxGens;
        this.numberOfChildren = numChild;
        setRandSeedEA(randSeedEA);
        this.fixedSeedForEvaluation = fixedSeedForEvaluation;
        this.useAverageFitness = useAvgFitness;
        this.useWebster = useWebster;
    }

    int getLastRunSimulationDuration() {
        return lastRunSimulationDuration;
    }

    final double getCrossOverProb() {
        return crossoverProb;
    }

    public final void setCrossOverProb(final double crossOverProb) {
        this.crossoverProb = crossOverProb;
    }

    final int getMaxGenerations() {
        return maxGenerations;
    }

    final double getMutationProb() {
        return mutationProb;
    }

    public final void setMutationProb(final double mutationProb) {
        this.mutationProb = mutationProb;
    }

    final double getMutationStep() {
        return mutationStepSize;
    }

    /**
     * Returns the number of children created in each generation.
     */
    final int getNumberOfChildren() {
        return numberOfChildren;
    }

    final int getPopSize() {
        return populationSize;
    }

    final long getRandSeedEA() {
        return randomSeedEA;
    }

    /**
     * Sets the random seed. If the seed equals 0, a seed is obtained from the
     * seed generator.
     *
     * @param randomSeed the random seed
     * @see de.dfg.oc.otc.layer2.SeedGenerator
     */
    private void setRandSeedEA(final long randomSeed) {
        if (randomSeed == 0) {
            this.randomSeedEA = SeedGenerator.getNextSeed();
        } else {
            this.randomSeedEA = randomSeed;
        }
    }

    final int getReevaluationDuration() {
        return reevaluationDuration;
    }

    final int getSeedChangeAfterXGenerations() {
        return seedChangeAfterXGens;
    }

    final int getSimulationDuration() {
        return simulationDuration;
    }

    final int getWarmUpDuration() {
        return warmUpDuration;
    }

    /**
     * Method used to create default parameters, if not provides as argument
     * for the constructor.
     */
    private void initialiseDefaultParams() {
        this.simulationDuration = DefaultParams.EA_SIM_DURATION;
        this.lastRunSimulationDuration = DefaultParams.EA_LAST_RUN_SIM_DURATION;
        this.warmUpDuration = DefaultParams.L2_WARMUP_TIME;
        this.populationSize = DefaultParams.EA_POP_SIZE;
        this.maxGenerations = DefaultParams.L2_MAX_GENERATIONS;
        this.numberOfChildren = DefaultParams.EA_NUMBER_OF_CHILDREN;
        setRandSeedEA(OTCManager.getInstance().getSystemSeed());
        this.fixedSeedForEvaluation = DefaultParams.L2_FIXED_SEED_FOR_EVALUATION;
        this.useAverageFitness = DefaultParams.L2_USE_AVG_FITNESS;
        this.useWebster = DefaultParams.L2_USE_WEBSTER;
        this.crossoverProb = DefaultParams.EA_CROSSOVERPROB;
        this.mutationProb = DefaultParams.EA_MUTATION_PROPABILITY;
        this.mutationStepSize = DefaultParams.EA_MUTATION_STEP;
        this.commaStrategy = DefaultParams.EA_COMMASTRATEGY;
        this.reevaluationDuration = DefaultParams.EA_RE_EVALUATION_DURATION;
    }

    final boolean isCommaStrategy() {
        return commaStrategy;
    }

    final boolean isFixedSeedForEvaluation() {
        return fixedSeedForEvaluation;
    }

    final boolean isReevaluateSurvivors() {
        return reevaluateSurvivors;
    }

    final boolean isUseAvgFitness() {
        return useAverageFitness;
    }

    final boolean isUseWebster() {
        return useWebster;
    }

    @Override
    public final String toString() {
        final String linesep = System.getProperty("line.separator");
        return "EA CONFIG" + linesep + "simulationDuration " + this.simulationDuration
                + ", warmUpDuration " + this.warmUpDuration + ", populationSize " + this.populationSize
                + ", maxGenerations " + this.maxGenerations + ", numberOfChildren " + this.numberOfChildren + linesep
                + ", commaStrategy " + this.commaStrategy + ", crossoverProbability " + this.crossoverProb
                + ", mutationProbability " + this.mutationProb + ", mutationStepSize " + this.mutationStepSize
                + linesep + ", fixedSeedForEvaluation " + this.fixedSeedForEvaluation + ", useAverageFitness "
                + this.useAverageFitness + ", useWebster " + this.useWebster + ", randomSeedEA " + this.randomSeedEA;
    }
}
