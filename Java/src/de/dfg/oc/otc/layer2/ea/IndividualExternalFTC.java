package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.Phase;

import java.io.IOException;
import java.util.List;

/**
 * Represents individuals which are used when optimizing external fixed-time
 * controllers.
 *
 * @author hpr
 */
class IndividualExternalFTC extends Individual {
    /**
     * Creates an individual of the given size.
     *
     * @param size the size of the new individual
     */
    IndividualExternalFTC(int size, EA ea, int[] constraints) {
        this.constraints = constraints;
        this.genes = new double[size];

        if (this.constraints[0] != 0) {
            final int minimalCycletime = getMinimalCycletime();
            this.genes[0] = (this.constraints[0] - minimalCycletime) / (this.MAX_CYCLETIME - minimalCycletime);
        }

        this.ea = ea;
    }

    /**
     * Performs a one point crossover of this individual with the individual
     * given as parameter.
     *
     * @return the individual resulting from the crossover
     */
    @Override
    public IndividualExternalFTC discreteRecombination(Individual partner) {
        // Copy constraints
        final int[] constraintsCopy = new int[constraints.length];
        System.arraycopy(constraints, 0, constraintsCopy, 0, constraints.length);

        IndividualExternalFTC temp = (IndividualExternalFTC) partner;
        int pos = ea.getRandomNumberGenerator().nextInt(0, getSize() - 1);
        IndividualExternalFTC child = new IndividualExternalFTC(this.getSize(),
                ea, constraintsCopy);

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

    }

    /**
     * Recalculates the fitness of this individual.
     *
     * @return the fitness of this individual
     */
    @Override
    public double calculateFitness() {
        // Create an FTC
        try {
            // translate phase durations
            int j = 0;
            int k = 0;
            List<Phase> phases = ((EAExternalFTC) ea).getJunction().getPhases();
            float[] durations = new float[((EAExternalFTC) ea).getNumPhases()];

            for (int i = 0; i < ((EAExternalFTC) ea).getNumPhases(); i++) {
                if (phases.get(i).isInterphase()) {
                    durations[i] = ((EAExternalFTC) ea).getInterphaseDurations()[k];
                    k++;
                } else {
                    durations[i] = (float) genes[j];
                    j++;
                }
            }

            int[] phaseIDs = new int[phases.size()];
            for (int i = 0; i < phases.size(); i++) {
                phaseIDs[i] = phases.get(i).getId();
            }

            // setup new FTC
            OTCManager.getInstance().generateFTCforJunction(((EAExternalFTC) ea).getJunction().getId(), phaseIDs, durations);
        } catch (Exception e1) {
            l2c.printEAWarning(e1.getMessage() + this.ea.getEaId());
        }

        ea.resetDB();

        // Start simulation
        ea.socketConnection.send("NEW_IND");

        double fitness = Double.NaN;

        //wait for AIMSUN
        try {
            String receivedStr = ea.socketConnection.recv();
            if (receivedStr.equals("READY")) {
                // read fitness value from database
                fitness = getFitness(ea.task.getReplicationId(), ea.turningIds);

                // Print individual to console
                l2c.printEAInfo(this.toString() + this.ea.getEaId());
            } else {
                l2c.printEAWarning("Socket protocol error: Received " + receivedStr + ", expected READY.");
            }
        } catch (IOException e) {
            l2c.printEAWarning("A problem occurred while reading from socket: " + e.getMessage());
        } catch (Exception e) {
            l2c.printEAWarning(e.getMessage());
        }
        return fitness;
    }

    /**
     * Return an exact copy of this individual.
     *
     * @return an exact copy of this individual
     */
    @Override
    public IndividualExternalFTC clone() throws CloneNotSupportedException {
        // Copy constraints
        final int[] constraintsCopy = new int[constraints.length];
        System.arraycopy(constraints, 0, constraintsCopy, 0, constraints.length);

        IndividualExternalFTC clone = new IndividualExternalFTC(this.getSize(),
                ea, constraintsCopy);
        System.arraycopy(genes, 0, clone.genes, 0, genes.length);
        clone.setFitness(fitness);
        return clone;
    }

}
