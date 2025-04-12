package de.dfg.oc.otc.layer2.ea;

/**
 * Implements the stopping criteria for the {@code EA}.
 *
 * @author hpr
 */
class StopCriteria {
    private final EA ea;

    /**
     * Start time for this optimization (current time in milliseconds).
     */
    private long startTime;

    /**
     * Maximum time for optimization (in milliseconds); set to 0 to ignore this
     * criterion.
     */
    private final long stopAfterTime;

    /**
     * Fitness threshold; set to 0 to ignore this criterion.
     */
    private final double stopAtFitness;

    /**
     * Maximum number of generations; set to 0 to ignore this criterion.
     */
    private final int stopAtGeneration;

    /**
     * Creates the stop criteria. Optimization is stopped iff one of the
     * criteria is reached.
     *
     * @param stopAtGeneration the maximum number of generations
     * @param stopAtFitness    the fitness threshold
     * @param stopAfterTime    the maximum time for optimization
     * @param ea               the EA these criteria belong to
     */
    StopCriteria(final int stopAtGeneration, final double stopAtFitness, final long stopAfterTime, final EA ea) {
        this.stopAtGeneration = stopAtGeneration;
        this.stopAtFitness = stopAtFitness;
        this.stopAfterTime = stopAfterTime;
        this.ea = ea;
    }

    /**
     * Returns {@code false} iff the optimization should be stopped because
     * one the the stop criteria is reached.
     */
    final boolean runAgain() {
        return stopAfterGeneration() && stopAtFitness() && stopAtTime();
    }

    final void setStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Returns {@code false} iff the optimization should be stopped because
     * the maximum number of generations is exceeded.
     */
    private boolean stopAfterGeneration() {
        return stopAtGeneration <= 0 || ea.getGenerationCounter() <= stopAtGeneration;
    }

    /**
     * Returns {@code false} iff the optimization should be stopped because
     * the best individuals fitness has exceeded the necessary fitness.
     */
    private boolean stopAtFitness() {
        return stopAtFitness <= 0 || ea.bestSolution.calculateFitness() <= stopAtFitness;
    }

    /**
     * Returns {@code false} iff the optimization should be stopped because
     * the maximum time for this optimization is exceeded.
     */
    private boolean stopAtTime() {
        return stopAfterTime <= 0 || System.currentTimeMillis() - startTime <= stopAfterTime;
    }
}
