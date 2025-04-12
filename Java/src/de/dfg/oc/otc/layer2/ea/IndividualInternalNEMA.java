package de.dfg.oc.otc.layer2.ea;

import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Represents individuals which are used when optimizing internal NEMA
 * controllers.
 *
 * @author hpr
 */
class IndividualInternalNEMA extends Individual {
    private static final Logger log = Logger.getLogger(IndividualInternalNEMA.class);
    /**
     * Initial value for self-adaptive mutation step size
     */
    private final double SIMGA_INIT = Math.random();

    /**
     * Creates an individual of the given size.
     */
    IndividualInternalNEMA(int size, EA ea, final int[] constraints) {
        this.constraints = constraints;
        this.genes = new double[size];

        if (this.constraints[0] != 0) {
            final int minimalCycletime = getMinimalCycletime();
            this.genes[0] = (this.constraints[0] - minimalCycletime) / (this.MAX_CYCLETIME - minimalCycletime);
        }

        this.ea = ea;
    }

    /**
     * Recalculates the fitness of this individual.
     *
     * @return the fitness of this individual
     */
    @Override
    public double calculateFitness() {
        ea.resetDB();

        sendInidividualToAimsun(this.genes);

        return getFitnessFromDB();
    }

    /**
     * Receive and set fitness from AIMSUN when simulation is done. Read objective function.
     */
    private double getFitnessFromDB() {
        double fitness = Double.NaN;

        try {
            // Wait till simulator is ready
            String received = ea.socketConnection.recv();
            if (received.equals("SIM_DONE")) {
                // Read objective function value
                fitness = getFitness(ea.task.getReplicationId(), ea.turningIds);
                // Print individual to console
                l2c.printEAInfo(this.toString() + this.ea.getEaId());
            } else {
                l2c.printEAWarning("Socket protocol error: Received " + received + ", expected SIM_DONE.");
            }
        } catch (IOException e) {
            log.error("An IOException occurred: ", e);
        }

        return fitness;
    }

    /**
     * Sends the absolute phase durations (phenotype) to AIMSUN.
     */
    private void sendInidividualToAimsun(double[] genes) {
        ea.socketConnection.send("NEW_IND");
        for (double gene : genes) {
            try {
                String message = ea.socketConnection.recv();
                if (message.equals("NEXT_ALLELE")) {
                    ea.socketConnection.send(String.valueOf(gene));
                } else {
                    l2c.printEAWarning("Socket protocol error: Received " + message + ", expected NEXT_ALLELE.");
                }
            } catch (Exception e) {
                l2c.printEAWarning("An Exception occurred: " + e.getMessage());
            }
        }
    }

    /**
     * Returns an exact copy of this individual.
     *
     * @return an exact copy of this individual
     */
    @Override
    public IndividualInternalNEMA clone() throws CloneNotSupportedException {
        // Copy constraints
        final int[] constraintsCopy = new int[constraints.length];
        System.arraycopy(constraints, 0, constraintsCopy, 0, constraints.length);

        IndividualInternalNEMA clone = new IndividualInternalNEMA(this.getSize(),
                this.ea, constraintsCopy);

        System.arraycopy(genes, 0, clone.genes, 0, genes.length);
        clone.setFitness(fitness);

        return clone;
    }

    /**
     * Performs a one point crossover of this individual with the individual
     * given as parameter.
     *
     * @return the individual resulting from the crossover
     */
    @Override
    protected Individual discreteRecombination(Individual partner) {
        IndividualInternalNEMA temp = (IndividualInternalNEMA) partner;

        int pos = ea.getRandomNumberGenerator().nextInt(0, getSize() - 1);

        final int[] constraintsClone = new int[constraints.length];
        System.arraycopy(constraints, 0, constraintsClone, 0, constraints.length);
        IndividualInternalNEMA child = new IndividualInternalNEMA(this.getSize(), ea, constraintsClone);

        for (int i = 0; i < child.getGenes().length; i++) {
            if (i <= pos) {
                child.getGenes()[i] = getGenes()[i];
            } else {
                child.getGenes()[i] = temp.getGenes()[i];
            }
        }
        return child;
    }

    @Override
    protected void resetGenesToRandomValues() {
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

    /*
    Converts the phenotype from genotype
    */
    final int[] decodeGenotype() {
        final int[] phenotype;
        phenotype = new int[((EAInternalNEMA) ea).getNumberOfNonInterphases()];

        // Sum min. and relative phase durations
        double sumOfRelativeDurations = 0;
        for (int i = 1; i < genes.length; i++) {
            sumOfRelativeDurations += genes[i];
        }

        if (sumOfRelativeDurations == 0) {
            sumOfRelativeDurations = 0.000001;
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
}