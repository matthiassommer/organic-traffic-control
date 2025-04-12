package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.layer0.tlc.TLCTypes;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer2.Layer2Exception;
import de.dfg.oc.otc.layer2.OptimisationResult;
import de.dfg.oc.otc.layer2.SocketConnection;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Evolutionary algorithm used when optimizing internal fixed-time controllers.
 *
 * @author hpr
 */
class EAInternalFTC extends EA {
    /**
     * Maps the id of a phase in AIMSUN to its position in the gene (interphases
     * are not coded in the genotype).
     */
    private final Map<Integer, Integer> phaseIdToGeneId = new HashMap<>();
    /**
     * The number of non-interphases of each individual.
     */
    private int numberOfNonInterphases;
    /**
     * Sum of the reference controller's interphase durations.
     */
    private int referenceInterphasesDuration;

    /**
     * Sum of the reference controller's non-interphase durations.
     */
    private int referenceNonInterphasesDuration;

    /**
     * Phase durations of the reference controller used in AIMSUN (including
     * interphases that are not considered during optimization).
     */
    private int[] referencePhases = new int[0];

    /**
     * Entries are {@code true} iff the corresponding phase is an
     * interphase.
     */
    private boolean[] referencePhaseTypes = new boolean[0];

    /**
     * Creates a new evolutionary algorithm.
     *
     * @param server a connection between this EA and Layer 1
     */
    EAInternalFTC(final EAServer server) {
        super(server);
    }

    @Override
    protected final OptimisationResult createOptimsationResult() {
        // Check if results need to be returned
        if (eaServer.isLayer1Present()) {
            final float fitness = this.bestSolutionAllTime.fitness;

            TrafficLightControllerParameters ftc = createTLCParameters();

            final OptimisationResult result = new OptimisationResult(this.task.getNodeID(), this.task
                    .getTime(), this.task.getSituation(), ftc, fitness);
            this.l2c.printEAInfo(String.valueOf(result));

            return result;
        }
        throw new Layer2Exception("Couldn't generate OptimisationResult.");
    }

    private TrafficLightControllerParameters createTLCParameters() {
        final int numberOfPhases = this.referencePhases.length;
        float[] durations = getPhaseDurations(numberOfPhases);

        final int[] phaseIDs = new int[numberOfPhases];
        for (int i = 1; i <= numberOfPhases; i++) {
            phaseIDs[i - 1] = i;
        }

        final int[] allIntegers = new int[numberOfPhases + 1];
        allIntegers[0] = this.task.getNodeID();
        System.arraycopy(phaseIDs, 0, allIntegers, 1, numberOfPhases);

        return new TrafficLightControllerParameters(TLCTypes.FIXEDTIME,
                allIntegers, durations, new String[0], referencePhaseTypes);
    }

    /**
     * Replace durations of non-interphases with optimized values.
     * @return phase durations
     */
    private float[] getPhaseDurations(int numberOfPhases) {
        int j = 0;
        float[] durations = new float[numberOfPhases];
        int[] phenotype = ((IndividualInternalFTC) this.bestSolutionAllTime).decodeGenotype();

        for (int i = 0; i < numberOfPhases; i++) {
            // If non-interphase use genes from individual
            if (!referencePhaseTypes[i]) {
                durations[i] = phenotype[j];
                j++;
                // else (i.e. if interphase) keep original phase duration.
            } else {
                durations[i] = referencePhases[i];
            }
        }
        return durations;
    }

    protected final void createPopulation() {
        createPopulationWithRelativeCoding();
    }

    /**
     * Creates the population for this class using a relative coding. See [BW05]
     * for details!
     */
    private void createPopulationWithRelativeCoding() {
        this.population.clear();
        receiveReferenceData();
        final int[] referenceDurations = determineReferenceNonInterphasesDurations();

        // Constraints for cycle time and min. phase durations
        final int[] constraints = new int[numberOfNonInterphases + 1];

        // cycle time (0 for unconstrained cycle time)
        constraints[0] = this.task.getCycleTimeConstraint();

        for (int i = 1; i < numberOfNonInterphases + 1; i++) {
            final int minimalPhaseDurations = Math.min(referenceDurations[i - 1], 5);
            constraints[i] = minimalPhaseDurations;
        }

        // Create individuals
        for (int i = 0; i < this.task.getEAConfig().getPopSize(); i++) {
            this.population.add(new IndividualInternalFTC(numberOfNonInterphases + 1, this, constraints));
        }
    }

    /**
     * Returns the non-interphase durations of the reference controller.
     *
     * @return the non-interphase durations of the reference controller
     */
    private int[] determineReferenceNonInterphasesDurations() {
        if (referencePhases.length == 0 || referencePhaseTypes.length == 0
                || referencePhases.length != referencePhaseTypes.length) {
            final String warning = "Phase durations and types do not match: " + Arrays.toString(referencePhases) + " "
                    + Arrays.toString(referencePhaseTypes);
            this.l2c.printEAWarning(warning);
        }

        // Replace one individual with reference solution
        int j = 0;
        final int[] referenceDurations = new int[numberOfNonInterphases];
        for (int i = 0; i < referencePhases.length; i++) {
            // If non interphase
            if (!referencePhaseTypes[i]) {
                referenceDurations[j] = referencePhases[i];
                j++;
            }
        }

        return referenceDurations;
    }

    final int getNumberOfNonInterphases() {
        return numberOfNonInterphases;
    }

    /**
     * Returns the sum of the reference controller's interphase durations.
     *
     * @return the sum of the reference controller's interphase durations
     */
    final int getReferenceInterphasesDuration() {
        return referenceInterphasesDuration;
    }

    final int[] getReferencePhases() {
        return referencePhases;
    }

    final boolean[] getReferencePhaseTypes() {
        return referencePhaseTypes;
    }

    final boolean isUseWebster() {
        return getConfig().isUseWebster();
    }

    final int mapPhaseIdToGeneId(final int phaseId) {
        return phaseIdToGeneId.get(phaseId);
    }

    /**
     * Receives the durations and types of all phases of the reference TLC used
     * by AIMSUN. Determines the number of non interphases. The result is stored
     * in {@code phaseDurations}, {@code phaseTypes}, and
     * {@code numberOfGenes}.
     */
    private void receiveReferenceData() {
        // Reset numberOfNonInterfaces (important, if task list contains several tasks)
        numberOfNonInterphases = 0;

        try {
            this.socketConnection = SocketConnection.getInstance(getEaId());

            // Receive turning ids
            this.socketConnection.send("WAITING_FOR_TURNINGS");
            final String concatenatedIds = this.socketConnection.recv();
            final String[] idStrings = concatenatedIds.split(" ");

            int[] ids = new int[idStrings.length];
            for (int i = 0; i < idStrings.length; i++) {
                ids[i] = new Integer(idStrings[i]);
            }
            this.turningIds = ids;

            // Receive number of phases
            this.socketConnection.send("WAITING_FOR_PHASES");
            final int numberOfPhases = new Integer(this.socketConnection.recv());
            initReferences(numberOfPhases);

            for (int i = 0; i < numberOfPhases; i++) {
                this.socketConnection.send("WAITING_NEXT_PHASE");

                final String[] receivedMessage = this.socketConnection.recv().split(" ");
                final int duration = new Float(receivedMessage[0]).intValue();
                final String isInterphase = receivedMessage[1];

                // Set phase duration and type
                referencePhases[i] = duration;
                if (isInterphase.equals("IP")) {
                    referencePhaseTypes[i] = true;
                    referenceInterphasesDuration += duration;
                } else {
                    phaseIdToGeneId.put(i, numberOfNonInterphases);
                    numberOfNonInterphases++;
                    referenceNonInterphasesDuration += duration;
                    referencePhaseTypes[i] = false;
                }
            }
        } catch (NumberFormatException | IOException e) {
            this.l2c.printEAWarning(e.getMessage());
        }
    }

    private void initReferences(int numberOfPhases) {
        referencePhases = new int[numberOfPhases];
        referencePhaseTypes = new boolean[numberOfPhases];
        referenceInterphasesDuration = 0;
        referenceNonInterphasesDuration = 0;
    }
}
