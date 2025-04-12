package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.layer0.tlc.Recall;
import de.dfg.oc.otc.layer0.tlc.TLCTypes;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerPhase;
import de.dfg.oc.otc.layer2.Layer2Exception;
import de.dfg.oc.otc.layer2.OptimisationResult;
import de.dfg.oc.otc.layer2.SocketConnection;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import de.dfg.oc.otc.manager.aimsun.Phase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evolutionary algorithm used when optimizing internal NEMA controllers.
 *
 * @author hpr
 */
class EAInternalNEMA extends EA {
    /**
     * Maps the id of a phase in AIMSUN to its position in the gene (interphases
     * are not coded in the genotype).
     */
    private final Map<Integer, Integer> phaseIdToGeneId = new HashMap<>();
    /*
    The Number of Phases
     */
    private int noPhases;
    private OTCManager manager;
    private AimsunJunction junction;

    /**
     * Phase durations of the reference controller used in AIMSUN (including
     * interphases that are not considered during optimization).
     */
    private int[] referencePhases = new int[0];
    /**
     * The number of non-interphases of each individual.
     */
    private int numberOfNonInterphases;

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
    EAInternalNEMA(final EAServer server) {
        super(server);
    }

    /**
     * Creates the population for this class.
     */
    @Override
    protected void createPopulation() {
        this.population.clear();
        l2c.printEAInfo("create Population");
        String recv = "";
        try {
            recv = this.socketConnection.recv();
        } catch (IOException e) {
            l2c.printEAWarning("IOException occurred: " + e.getMessage() +
                    this.getEaId());
        }

        if (recv.equals("INIT_DONE")) {
            manager = OTCManager.getInstance();
        } else {
            l2c.printEAWarning("Socket protocol error: Received " + recv
                    + ", expected INIT_DONE." + this.getEaId());
        }
        try {
            socketConnection = SocketConnection.getInstance(getEaId());

            // Receive turning ids
            socketConnection.send("WAITING_FOR_TURNINGS");
            String concatenatedIds = socketConnection.recv();
            String[] idStrings = concatenatedIds.split(" ");
            turningIds = new int[idStrings.length];
            for (int i = 0; i < idStrings.length; i++) {
                turningIds[i] = new Integer(idStrings[i]);
            }

            // Receive number of phases from Python/AIMSUN
            this.socketConnection.send("WAITING_FOR_PHASES");
            noPhases = new Integer(this.socketConnection.recv());
            initReferences(noPhases);

            for (int i = 0; i < noPhases; i++) {
                this.socketConnection.send("WAITING_NEXT_PHASE");
                final String[] receivedMessage = this.socketConnection.recv().split(" ");
                final int duration = new Float(receivedMessage[0]).intValue();
                final String isInterphase = receivedMessage[1];

                // Set phase duration and type
                referencePhases[i] = duration;
                if (isInterphase.equals("IP")) {
                    referencePhaseTypes[i] = true;
                } else {
                    phaseIdToGeneId.put(i, numberOfNonInterphases);
                    numberOfNonInterphases++;
                    referencePhaseTypes[i] = false;
                }
            }
            AimsunNetwork network = manager.getNetwork();
            junction = network.getJunction(this.task.getNodeID());
        } catch (NumberFormatException e) {
            l2c.printEAWarning(e.getMessage() + this.getEaId());
        } catch (IOException e) {
            l2c.printEAWarning("A problem occurred while reading from socket: "
                    + e.getMessage() + this.getEaId());
        }

        // Create population
        /*int[] lowerBounds = new int[5 * noPhases];
        int[] upperBounds = new int[5 * noPhases];
        for (int i = 0; i < noPhases; i++) {
            lowerBounds[i * 5] = 5;
            lowerBounds[i * 5 + 1] = 0;
            lowerBounds[i * 5 + 2] = 0;
            lowerBounds[i * 5 + 3] = 0;
            lowerBounds[i * 5 + 4] = 0;
            upperBounds[i * 5] = 20;
            upperBounds[i * 5 + 1] = 20;
            upperBounds[i * 5 + 2] = 20;
            upperBounds[i * 5 + 3] = 5;
            upperBounds[i * 5 + 4] = 5;
        }*/


        // Constraints for cycle time and min. phase durations
        final int[] constraints = new int[numberOfNonInterphases + 1];

        // cycle time (0 for unconstrained cycle time)
        constraints[0] = this.task.getCycleTimeConstraint();

        for (int i = 0; i < task.getEAConfig().getPopSize(); i++) {
            this.population.add(new IndividualInternalNEMA(5 * noPhases, this, constraints));
        }
    }

    @Override
    protected OptimisationResult createOptimsationResult() {
        List<Phase> phaseList = new ArrayList<>();
        final float[] maxGreens = new float[noPhases];
        final float[] maxInits = new float[noPhases];
        final float[] maxGaps = new float[noPhases];
        final float[] minGreens = new float[noPhases];
        final float[] extSteps = new float[noPhases];
        final float[] reductionDelays = new float[noPhases];
        List<Integer> phaseIds = this.junction.getPhaseIds();
        TrafficLightControllerPhase[] phases1 = new TrafficLightControllerPhase[noPhases];

        for (int i = 0; i < noPhases; i++) {
            phaseList.add(junction.getPhaseById(i));
        }

        final float[] maxgreens = new float[noPhases];

        for (int i = 0; i < noPhases; i++) {
            maxgreens[i] = phaseList.get(i).getDefaultMaximalDuration();
        }

        for (int i = 0; i < noPhases; i++) {
            phases1[i] = new TrafficLightControllerPhase(maxgreens[i], this.junction.getPhaseById(phaseIds.get(i)));
        }
        TrafficLightControllerParameters parameters = createTLCParameters();
        final Recall[] recalls = getRecalls(parameters, noPhases);
        int[] recallInts;

        for (int i = 0; i < noPhases; i++) {
            maxGreens[i] = phases1[i].getMaxGreenTime();
            maxInits[i] = phases1[i].getMaximimGreenTime();
            maxGaps[i] = phases1[i].getMaximumGap();
            minGreens[i] = phases1[i].getMinimumGreenTime();
            extSteps[i] = phases1[i].getExtensionStep();
            reductionDelays[i] = phases1[i].getReductionDelay();
        }

        List<Phase> phases = junction.getPhases();

        recallInts = getInts(recalls);

        // Check if results need to be returned
        if (eaServer.isLayer1Present()) {

            int[] phaseIDs = new int[noPhases];
            for (int i = 0; i < noPhases; i++) {
                phaseIDs[i] = phases.get(i).getId();
            }

            manager.generateNEMAforJunction(junction.getId(), phaseIDs, maxGreens, maxInits, recallInts, maxGaps,
                    minGreens, extSteps, reductionDelays);

            final float fitness = this.bestSolutionAllTime.fitness;


            final OptimisationResult result = new OptimisationResult(this.task.getNodeID(), this.task
                    .getTime(), this.task.getSituation(), parameters, fitness);
            this.l2c.printEAInfo(String.valueOf(result));

            return result;
        }
        throw new Layer2Exception("Couldn't generate OptimisationResult.");
    }

    private TrafficLightControllerParameters createTLCParameters() {
        float[] durations = getPhaseDurations(noPhases);
        final int[] phaseIDs = new int[noPhases];
        for (int i = 1; i <= noPhases; i++) {
            phaseIDs[i - 1] = i;
        }

        final int[] allIntegers = new int[noPhases + 1];
        allIntegers[0] = this.task.getNodeID();
        System.arraycopy(phaseIDs, 0, allIntegers, 1, noPhases);

        return new TrafficLightControllerParameters(TLCTypes.NEMA, allIntegers, durations, new String[0], referencePhaseTypes);
    }

    protected final int mapPhaseIdToGeneId(final int phaseId) {
        return phaseIdToGeneId.get(phaseId);
    }

    /**
     * Replace durations of non-interphases with optimized values.
     *
     * @param numberOfPhases
     * @return phase durations
     */
    private float[] getPhaseDurations(int numberOfPhases) {
        int j = 0;
        float[] durations = new float[numberOfPhases];
        int[] phenotype = ((IndividualInternalNEMA) this.bestSolutionAllTime).decodeGenotype();

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

    private void initReferences(int numberOfPhases) {
        referencePhases = new int[numberOfPhases];
        referencePhaseTypes = new boolean[numberOfPhases];
    }

    final int getNumberOfNonInterphases() {
        return numberOfNonInterphases;
    }

    private Recall[] getRecalls(final TrafficLightControllerParameters parameters, final int numUsedPhases) {
        final Recall[] recalls = new Recall[numUsedPhases];
        final int[] ids = parameters.getIds();
        for (int i = 0; i < numUsedPhases; i++) {
            recalls[i] = Recall.getRecallForId(ids[numUsedPhases + 1 + i]);
        }
        return recalls;
    }

    private int[] getInts(Recall[] recalls) {

        int[] tmp = null;

        for (int i = 0; i < recalls.length; i++) {
            if (recalls[i] == Recall.disable) {
                tmp[i] = 0;
            } else if (recalls[i] == Recall.no) {
                tmp[i] = 1;
            } else if (recalls[i] == Recall.min) {
                tmp[i] = 2;
            } else if (recalls[i] == Recall.max) {
                tmp[i] = 3;
            }
        }
        return tmp;
    }
}
