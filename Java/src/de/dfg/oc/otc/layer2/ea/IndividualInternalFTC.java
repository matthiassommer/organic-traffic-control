package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.layer2.TurningData;
import org.apache.commons.math3.util.FastMath;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Represents individuals which are used when optimizing internal fixed-time
 * controllers.
 * <p>
 * Genotype: A signal program is represented by its cycle time (first
 * allele), phase durations are specified as fractions of the cycle (succeeding
 * alleles). See [BW05] for details on coding.
 * <p>
 * Phenotype:
 *
 * @author hpr
 */
public class IndividualInternalFTC extends Individual {
    /**
     * Initial value for self-adaptive mutation step size (can also be randomly
     * chosen).
     */
    private final float SIMGA_INIT = .2f;

    /**
     * Creates an individual of the given size.
     *
     * @param size        the size of the new individual, number of non-interphases
     * @param ea          EA this individual belongs to
     * @param constraints constraints for the optimisation (contains a cycle time (
     *                    {@code 0}, iff unconstrained) followed by the minimum
     *                    phase durations)
     */
    IndividualInternalFTC(final int size, final EA ea, final int[] constraints) {
        this.ea = ea;
        this.constraints = constraints;
        this.genes = new double[size];
        this.tau = 1 / Math.sqrt(size);

        // Random initialization of individual
        resetGenesToRandomValues();

        // Consider cycle time constraint
        if (this.constraints[0] != 0) {
            final int minimalCycletime = getMinimalCycletime();
            this.genes[0] = (this.constraints[0] - minimalCycletime) / (this.MAX_CYCLETIME - minimalCycletime);
        }
    }

    /**
     * Determines the average delay for this individual using an AIMSUN
     * simulation.
     *
     * @return fitness
     */
    private void aimsunAverageDelay() {
        ea.resetDB();

        sendInidividualToAimsun();

        // TODO Unterschiedliche Zielfunktionen (Attribute) beachten!
        getFitnessFromDB();
    }

    /**
     * Receive and set fitness from AIMSUN when simulation is done.
     */
    private void getFitnessFromDB() {
        try {
            // Wait until simulator is ready
            final String message = ea.socketConnection.recv();
            if (message.equals("SIM_DONE")) {
                // Read objective function value
                float fitness = getFitness(ea.task.getReplicationId(), ea.turningIds);
                setFitness(fitness);
            } else {
                l2c.printEAWarning("Socket protocol error: Received " + message + ", expected SIM_DONE.");
            }
        } catch (IOException e) {
            l2c.printEAWarning("A problem occurred while reading from socket: " + e.getMessage());
        }
    }

    /**
     * Sends the absolute phase durations (phenotype) to AIMSUN.
     */
    private void sendInidividualToAimsun() {
        final int[] phenotype = decodeGenotype();

        ea.socketConnection.send("NEW_IND");
        for (int aPhenotype : phenotype) {
            try {
                final String message = ea.socketConnection.recv();
                if (message.equals("NEXT_ALLELE")) {
                    ea.socketConnection.send(String.valueOf(aPhenotype));
                } else {
                    l2c.printEAWarning("Socket protocol error: Received " + message + ", expected NEXT_ALLELE.");
                }
            } catch (IOException e) {
                l2c.printEAWarning(e.getMessage());
            }
        }
    }

    /**
     * Recalculates the fitness of this individual.
     *
     * @return the fitness of this individual
     */
    @Override
    protected final double calculateFitness() {
        // Evaluate using AIMSUN or Webster?
        if (!((EAInternalFTC) ea).isUseWebster()) {
            aimsunAverageDelay();
        } else {
            websterAverageDelay();
            // websterAvgDelayWithNoise();
        }

        return fitness;
    }

    @Override
    protected final Individual clone() {
        // Copy constraints
        final int[] constraintsCopy = new int[constraints.length];
        System.arraycopy(constraints, 0, constraintsCopy, 0, constraints.length);

        final IndividualInternalFTC clone = new IndividualInternalFTC(this.getSize(), ea, constraintsCopy);

        // Copy genes
        System.arraycopy(genes, 0, clone.genes, 0, genes.length);

        // Copy fitness
        clone.setFitness(fitness);
        clone.sigma = this.sigma;

        return clone;
    }

    /**
     * Converts from genotype (cycle time, relative durations) the phenotype
     * (absolute phase durations).
     *
     * @return absolute phase durations for non-interphases
     */
    final int[] decodeGenotype() {
        final int[] phenotype = new int[((EAInternalFTC) ea).getNumberOfNonInterphases()];

        // Sum min. and relative phase durations
        float sumOfRelativeDurations = 0;
        for (int i = 1; i < genes.length; i++) {
            sumOfRelativeDurations += genes[i];
        }

        if (sumOfRelativeDurations == 0) {
            sumOfRelativeDurations = 0.000001f;
        }

        final int minimalCycletime = getMinimalCycletime();
        final int phenotypeCycle = (int) Math.round(minimalCycletime + genes[0] * (MAX_CYCLETIME - minimalCycletime));

        // Phenotype phase durations
        int sumOfPhenoTypePhaseDurations = 0;
        for (int i = 0; i < phenotype.length; i++) {
            phenotype[i] = (int) Math.round(constraints[i + 1] + genes[i + 1] / sumOfRelativeDurations
                    * (phenotypeCycle - minimalCycletime));
            sumOfPhenoTypePhaseDurations += phenotype[i];
        }

        // Correct differences that might result from rounding
        final int difference = phenotypeCycle
                - (((EAInternalFTC) ea).getReferenceInterphasesDuration() + sumOfPhenoTypePhaseDurations);
        if (difference != 0) {
            for (int i = 0; i < phenotype.length; i++) {
                if (phenotype[i] + difference >= constraints[i]) {
                    phenotype[i] += difference;
                    break;
                }
            }
        }

        return phenotype;
    }

    @Override
    protected final Individual discreteRecombination(final Individual partner) {
        final int[] constraintsClone = new int[constraints.length];
        System.arraycopy(constraints, 0, constraintsClone, 0, constraints.length);

        final IndividualInternalFTC clone = new IndividualInternalFTC(this.getSize(), ea, constraintsClone);

        // Discrete recombination for genes (see [ES07])
        for (int i = 0; i < clone.getGenes().length; i++) {
            if (ea.getRandomNumberGenerator().nextUniform(0, 1) <= 0.5) {
                clone.getGenes()[i] = getGenes()[i];
            } else {
                clone.getGenes()[i] = partner.getGenes()[i];
            }
        }

        // Intermediary recombination for strategy parameter (see [ES07])
        clone.sigma = (this.sigma + partner.sigma) / 2;
        clone.resetFitness();

        return clone;
    }

    /**
     * Returns the cycle time of this individual (including interphase durations).
     *
     * @return cycle time of this individual
     */
    private int getCycleTime() {
        final int minimalCycletime = getMinimalCycletime();
        return (int) Math.round(minimalCycletime + genes[0] * (MAX_CYCLETIME - minimalCycletime));
    }

    @Override
    protected final void resetGenesToRandomValues() {
        final double mutationStep = ea.getConfig().getMutationStep();
        if (mutationStep == 0) {
            sigma = this.SIMGA_INIT;
        } else {
            sigma = mutationStep;
        }

        for (int i = 0; i < genes.length; i++) {
            genes[i] = ea.getRandomNumberGenerator().nextUniform(0, 1);
        }
    }

    @Override
    public final String toString() {
        final String[] genes = new String[this.genes.length];
        for (int i = 0; i < this.genes.length; i++) {
            genes[i] = String.format("%1.2f", this.genes[i]);
        }

        return Arrays.toString(genes) + " - " + this.sigma + " - "
                + String.format("%3d", getCycleTime()) + " - " + Arrays.toString(decodeGenotype()) + " - "
                + String.format("%3.4f", fitness);
    }

    /**
     * Calculates the average delay at an intersection based on Webster's
     * formula for the average delay in the approaches. See [Web58, SL97].
     */
    private void websterAverageDelay() {
        // Determine total demand at intersection
        final float totalDemand = ea.task.getTotalDemand();
        final int[] phenoType = decodeGenotype();
        float waitingTime = 0;

        final Collection<TurningData> turnDataCollection = ea.task.getTurningData().values();
        for (TurningData turningData : turnDataCollection) {
            float greenTimeForTurning = 0;

            for (int phaseId : turningData.getPhases()) {
                if (((EAInternalFTC) ea).getReferencePhaseTypes()[phaseId - 1]) {
                    // Interphase, get duration from reference
                    greenTimeForTurning += ((EAInternalFTC) ea).getReferencePhases()[phaseId - 1];
                } else {
                    // Non-interphase, get duration from individual
                    greenTimeForTurning += phenoType[((EAInternalFTC) ea).mapPhaseIdToGeneId(phaseId - 1)];
                }
            }

            float delay = websterAverageDelayPerTurn(turningData.getFlow(), greenTimeForTurning, getCycleTime(), turningData.getNumberOfLanes());
            waitingTime += turningData.getFlow() / totalDemand * delay;
        }

        setFitness(waitingTime);
    }

    /**
     * Calculates the average waiting time for a turning according to Webster's
     * formula.
     *
     * @param trafficFlow current flow (veh/h)
     * @param greenTime   effective green time for turning in seconds
     * @param cycleTime   cycle time in seconds
     * @param lanes       number of available lanes for the turning
     * @return average waiting time (in sec)
     */
    private float websterAverageDelayPerTurn(float trafficFlow, final float greenTime, final float cycleTime, final int lanes) {
        // Avoid division by zero
        if (trafficFlow == 0) {
            trafficFlow = 1;
        }

        final float saturationFlow = lanes * 1800;
        // Anteil der effektiven Grünzeit am Umlauf
        final float f = greenTime / cycleTime;
        float degreeOfSaturation = trafficFlow / (f * saturationFlow);

        // Prerequisite: degreeOfSaturation < 1
        if (degreeOfSaturation > 1) {
            degreeOfSaturation = .99f;
        }

        final double td = cycleTime * FastMath.pow(1 - f, 2) / (2 * (1 - trafficFlow / saturationFlow));

        return (float) (0.9 * (td + 1800 * FastMath.pow(degreeOfSaturation, 2) / (trafficFlow * (1 - degreeOfSaturation))));
    }
}
