package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer2.OptimisationResult;
import de.dfg.oc.otc.layer2.OptimisationTask;
import de.dfg.oc.otc.tools.AbstractArrayUtilities;
import org.apache.commons.math3.random.RandomDataGenerator;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for evolutionary algorithms.
 *
 * @author hpr
 */
abstract class EA extends SocketConnector {
    /**
     * Layer 1 connection for this EA.
     */
    final EAServer eaServer;
    private final DatabaseConnector databaseConnector;
    /**
     * The current optimization task.
     */
    OptimisationTask task;
    /**
     * The best solution of current generation.
     */
    Individual bestSolution;
    /**
     * The best solution obtained so far.
     */
    Individual bestSolutionAllTime;
    /**
     * The population.
     */
    List<Individual> population = new ArrayList<>();
    /**
     * Turning ids for the junction considered for optimization.
     */
    int[] turningIds;
    /**
     * The simulated duration for the best solution obtained so far.
     */
    private int bestSolutionAllTimeSimDur;
    /**
     * Current simulation period (without warm-up) in s used for evaluation.
     */
    private int simulationDuration = -1;
    /**
     * Id for identifying this EA.
     */
    private int eaId;
    /**
     * Determines whether the random seed used in evaluation runs is kept
     * constant ({@code true}) or whether it is changed each generation (
     * {@code false}).
     */
    private boolean fixedSeedForEvaluation;
    /**
     * The generation counter.
     */
    private int generationCounter = 1;
    /**
     * The random number generator for this class.
     */
    private RandomDataGenerator rand;
    /**
     * The selector (for parent selection).
     */
    private AbstractSelection selector;
    /**
     * Array containing the length of the simulation period (in s) for each generation.
     */
    private int[] simulationDurationArray;
    /**
     * Start time of this optimization.
     */
    private LocalTime startTime;
    /**
     * The stopping criteria for this EA.
     */
    private StopCriteria stopCriteria;
    /**
     * Determines whether the fitness is calculated based on the current random
     * seed only or as average of several seeds. The parameter is only relevant
     * if {@code fixedSeedForEvaluation} is {@code false}.
     */
    private boolean useAverageFitness;

    /**
     * Creates a new evolutionary algorithm.
     *
     * @param eaServer a connection between this EA and Layer 1
     */
    EA(final EAServer eaServer) {
        this.eaServer = eaServer;

        this.eaId = this.eaServer.registerEAAtLayer1(this);

        setupGUI();
        this.databaseConnector = new DatabaseConnector(eaId);
    }

    /**
     * Cleans up after an optimization is finished and return optimisation result to Layer 1.
     */
    private void finishOptimisation() {
        // close socket
        socketConnection.send("DONE");

        updateConsole(startTime);

        if (DefaultParams.EA_CREATE_LOGFILES) {
            saveLogs(this.eaServer.getFilenamePrefix(), task);
        }

        eaServer.returnOptimisationResult(createOptimsationResult());
    }

    /**
     * Creates the child individuals.
     */
    private void createChildren() {
        final List<Individual> childPop = new ArrayList<>();

        for (int j = 0; j < task.getEAConfig().getNumberOfChildren(); j++) {
            final Individual[] parents = selector.selectParents();
            Individual child = null;

            if (rand.nextUniform(0, 1) < task.getEAConfig().getCrossOverProb()) {
                // Recombination
                child = parents[0].discreteRecombination(parents[1]);
            } else {
                // No recombination
                try {
                    child = parents[0].clone();
                } catch (CloneNotSupportedException e) {
                    System.err.println(e.getMessage());
                }
            }

            if (rand.nextUniform(0, 1) < task.getEAConfig().getMutationProb()) {
                if (child != null) {
                    child.mutate();
                }
            }

            // Add child to population
            childPop.add(child);
        }

        if (task.getEAConfig().isCommaStrategy()) {
            // Replace population with children (comma strategy)
            this.population = childPop;
        } else {
            // Add offspring to population (plus strategy)
            population.addAll(childPop);
        }
    }

    protected abstract OptimisationResult createOptimsationResult();

    /**
     * Creates the population for this class.
     */
    protected abstract void createPopulation();

    /**
     * Creates an array defining the simulated durations for the run.
     */
    private void createSimulationDurationArray() {
        // Use simulated duration in config.xml for all but the final
        // generation
        simulationDurationArray = new int[getConfig().getMaxGenerations()];
        for (int i = 0; i < simulationDurationArray.length - 1; i++) {
            simulationDurationArray[i] = getConfig().getSimulationDuration();
        }

        // Increased simulated duration for final generation
        simulationDurationArray[getConfig().getMaxGenerations() - 1] = getConfig().getLastRunSimulationDuration();
    }

    /**
     * Checks whether the best individual in the current generation is the best
     * solution found so far.
     */
    private void determineBestSolutionAllTime() {
        try {
            bestSolution = population.get(0).clone();

            if (bestSolutionAllTime == null) {
                // No "bestSolutionAllTime" so far
                bestSolutionAllTime = bestSolution.clone();
                bestSolutionAllTimeSimDur = simulationDuration;
            } else if (getAttribute().isInverted() && bestSolutionAllTime.fitness > bestSolution.fitness
                    && simulationDuration >= bestSolutionAllTimeSimDur) {
                // minimization
                bestSolutionAllTime = bestSolution.clone();
                bestSolutionAllTimeSimDur = simulationDuration;
            } else if (!getAttribute().isInverted() && bestSolutionAllTime.fitness < bestSolution.fitness
                    && simulationDuration >= bestSolutionAllTimeSimDur) {
                // maximization
                bestSolutionAllTime = bestSolution.clone();
                bestSolutionAllTimeSimDur = simulationDuration;
            }
        } catch (CloneNotSupportedException e) {
            System.err.println(e.getMessage());
        }
    }

    final Attribute getAttribute() {
        return task.getAttribute();
    }

    EAConfig getConfig() {
        return task.getEAConfig();
    }

    final int getEaId() {
        return eaId;
    }

    final void setEaId(final int eaId) {
        this.eaId = eaId;
    }

    final int getGenerationCounter() {
        return generationCounter;
    }

    final RandomDataGenerator getRandomNumberGenerator() {
        return rand;
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
    final float readFitnessFromDB(final int rid, final int[] turningIds) {
        return databaseConnector.getDatabase().calculateLoS(rid, turningIds, -1, -1);
    }

    /**
     * Initializes the EA.
     */
    private void initializeEA() {
        this.stopCriteria = new StopCriteria(task.getEAConfig().getMaxGenerations(), 0, 0, this);
        this.generationCounter = 1;
        this.bestSolutionAllTime = null;
        this.bestSolutionAllTimeSimDur = 0;

        this.fixedSeedForEvaluation = task.getEAConfig().isFixedSeedForEvaluation();
        this.useAverageFitness = task.getEAConfig().isUseAvgFitness();

        this.startTime = LocalTime.now();
        this.l2c.printEAInfo("STARTTIME " + LocalDateTime.now());

        this.rand = new RandomDataGenerator();
        this.rand.reSeed(task.getEAConfig().getRandSeedEA());

        this.selector = new SelectionRandom(this);

        sendOptimisationTask(task, eaId);
        createPopulation();
        createSimulationDurationArray();
    }

    final boolean isFixedSeedForEvaluation() {
        return fixedSeedForEvaluation;
    }

    final boolean isUseAvgFitness() {
        return useAverageFitness;
    }

    /**
     * Starts the optimization.
     */
    private void optimize() {
        stopCriteria.setStartTime();

        if (!task.getEAConfig().isUseWebster()) {
            aimsunSeed = rand.nextInt(0, Integer.MAX_VALUE);
            sendAIMSUNSeed(aimsunSeed);
        }
        printStatus(false, population, task, generationCounter, aimsunSeed, bestSolution, bestSolutionAllTime);

        if (!task.getEAConfig().isUseWebster()) {
            // Simulationsdauer (falls nicht Webster!)
            setSimulationDuration(simulationDurationArray[generationCounter - 1], true);
        }

        // Evaluate initial population
        sortList();

        // While stop criteria are not reached
        while (stopCriteria.runAgain()) {
            if (generationCounter != 1) {
                printStatus(false, population, task, generationCounter, aimsunSeed, bestSolution, bestSolutionAllTime);
            }

            if (!task.getEAConfig().isUseWebster()) {
                setSimulationDuration(simulationDurationArray[generationCounter - 1], true);
            }

            createChildren();
            sortList();

            resizePopulation();
            // Update best solution
            determineBestSolutionAllTime();

            printStatus(true, population, task, generationCounter, aimsunSeed, bestSolution, bestSolutionAllTime);

            generationCounter++;

            // Handling of AIMSUN seeds
            if (!task.getEAConfig().isUseWebster()) {
                if (!fixedSeedForEvaluation) {
                    // Prerequisite: Varying seeds
                    if ((generationCounter - 1) % task.getEAConfig().getSeedChangeAfterXGenerations() == 0) {
                        // Change seed every x generations
                        aimsunSeed = rand.nextInt(0, Integer.MAX_VALUE);
                        sendAIMSUNSeed(aimsunSeed);

                        if (task.getEAConfig().isReevaluateSurvivors() && !useAverageFitness) {
                            resetFitnessForPopulation();
                        }
                    }
                }
            }
        }
    }

     /**
     * Drops the current table from the databaseConnector.
     */
    final void resetDB() {
        databaseConnector.getDatabase().deleteTableEntries();
    }

    /**
     * Sets the fitness of all individuals in the current population to
     * {@code Double.NaN}.
     */
    private void resetFitnessForPopulation() {
        l2c.printEAInfo("Resetting fitness for population.");
        population.forEach(Individual::resetFitness);
    }

    /**
     * Provides the survivor selection.
     */
    private void resizePopulation() {
        while (population.size() > task.getEAConfig().getPopSize()) {
            population.remove(population.size() - 1);
        }
    }

    /**
     * Sets the simulated duration for AIMSUN. Updates the best known solution
     * after a change in the simulated duration. Resets population and best
     * solution after a changed simulated duration.
     *
     * @param duration        the simulated duration (in seconds)
     * @param resetPopulation {@code false} to suppress reset; otherwise {@code true}
     */
    private void setSimulationDuration(final int duration, final boolean resetPopulation) {
        if (simulationDuration != duration) {
            sendNewSimulationDuration(duration);

            if (resetPopulation) {
                // Update bestSolutionAllTime
                if (bestSolutionAllTime != null && duration > bestSolutionAllTimeSimDur) {
                    // In case that the simulated duration has increased,
                    // reevaluate best solution
                    bestSolutionAllTime.calculateFitness();
                    l2c.printEAInfo("[setSimulationDuration] Re-evaluating bestSolutionAllTime: " + bestSolutionAllTime);
                }

                // Reset fitness of (survivor) population
                resetFitnessForPopulation();
                l2c.printEAInfo("[setSimulationDuration] Resetting fitness for parents");
            }

            // Store simulated duration
            this.simulationDuration = duration;
        }
    }

    /**
     * Sorts the population depending on the fitness of the individuals.
     */
    private void sortList() {
        Collections.sort(population);
    }

    /**
     * Starts the given optimization task.
     *
     * @param task an OptTask that will be started
     */
    final void startOptimisation(final OptimisationTask task) {
        this.task = task;

        // Reset console and chart
        initGUI(task);
        initializeEA();
        optimize();
        finishOptimisation();
    }
}
