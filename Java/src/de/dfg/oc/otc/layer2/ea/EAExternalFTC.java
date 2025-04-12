package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer2.Layer2Exception;
import de.dfg.oc.otc.layer2.OptimisationResult;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.*;

import java.io.IOException;
import java.util.List;

/**
 * Evolutionary algorithm used when optimizing external fixed-time controllers.
 *
 * @author hpr
 */
class EAExternalFTC extends EA {
    private OTCManager manager;

    /**
     * The junction object whose signal timings will be optimized.
     */
    private AimsunJunction junction;

    /**
     * Number of phases for junction under consideration
     */
    private int numPhases;

    /**
     * Duration of interphases
     */
    private float[] interphaseDurations;

    /**
     * Creates a new evolutionary algorithm.
     *
     * @param server {@code Server} that created this EA
     */
    public EAExternalFTC(final EAServer server) {
        super(server);
    }

    @Override
    protected OptimisationResult createOptimsationResult() {
        throw new Layer2Exception("Couldn't generate OptimisationResult. Not implemented");
    }

    @Override
    protected void createPopulation() {
        this.population.clear();

        String recv = "";
        try {
            recv = this.socketConnection.recv();
        } catch (IOException e) {
            l2c.printEAWarning("IOException occurred: " + e.getMessage() + getEaId());
        }

        if (recv.equals("INIT_DONE")) {
            manager = OTCManager.getInstance();
            AimsunNetwork network = manager.getNetwork();

            if (network.getNumControlledJunctions() == 0) {
                l2c.printEAWarning("Network contains no controlled junction." + getEaId());
            } else {
                // Get junction based on optimization task
                junction = network.getJunction(this.task.getNodeID());
                numPhases = junction.getNumPhases();
                // Number of non interphases for junction under consideration
                int numNonInterphases = junction.getNumNonInterphases();

                // Store durations of interphases
                List<Phase> phases = junction.getPhases();
                interphaseDurations = new float[numPhases - numNonInterphases];
                int j = 0;
                for (Phase phase : phases) {
                    if (phase.isInterphase()) {
                        interphaseDurations[j] = phase.getDefaultDuration();
                        j++;
                    }
                }

                // Annahme: Es sollen nur Turnings ber�cksichtigt werden,
                // die motorisierte Fahrzeuge bedienen.
                List<Turning> turnings = junction.getTurnings(TrafficType.INDIVIDUAL_TRAFFIC);
                turningIds = new int[2 * turnings.size()];
                for (int i = 0; i < turnings.size(); i++) {
                    turningIds[2 * i] = turnings.get(i).getInSection().getId();
                    turningIds[2 * i + 1] = turnings.get(i).getOutSection().getId();
                }

                // Create population
                final int[] constraints = new int[numNonInterphases + 1];
                // cycle time (0 for unconstrained cycle time)
                constraints[0] = this.task.getCycleTimeConstraint();
                for (int i = 1; i < numNonInterphases + 1; i++) {
                    final int minimalPhaseDurations = (int) Math.min(interphaseDurations[i - 1], 5);
                    constraints[i] = minimalPhaseDurations;
                }
                for (int i = 0; i < task.getEAConfig().getPopSize(); i++) {
                    IndividualExternalFTC ind = new IndividualExternalFTC(numNonInterphases, this, constraints);
                    this.population.add(ind);
                }
            }
        } else {
            l2c.printEAWarning("Socket protocol error: Received " + recv
                    + ", expected INIT_DONE." + getEaId());
        }
    }

    /**
     * Returns optimization results (as {@code OptResult}) to Layer 1.
     * Data is transferred via RMI.
     */
    protected void returnResultsToLayer1() {
        // Create TLC parameter object
        // determine optimized durations for non interphases
        float value = new Double(bestSolution.fitness).floatValue();
        float[] durations = new float[numPhases];

        int j = 0;
        int k = 0;
        List<Phase> phases = junction.getPhases();
        for (int i = 0; i < numPhases; i++) {
            if (phases.get(i).isInterphase()) {
                durations[i] = interphaseDurations[k];
                k++;
            } else {
                durations[i] = (float) bestSolution.genes[j];
                j++;
            }
        }

        int[] phaseIDs = new int[phases.size()];
        for (int i = 0; i < phases.size(); i++) {
            phaseIDs[i] = phases.get(i).getId();
        }

        // setup new FTC
        manager.generateFTCforJunction(junction.getId(), phaseIDs, durations);

        TrafficLightControllerParameters par = junction.getActiveTLC().getParameters();

        OptimisationResult res = new OptimisationResult(task.getNodeID(), task
                .getTime(), task.getSituation(), par, value);

        eaServer.returnOptimisationResult(res);
    }

    public int getNumPhases() {
        return numPhases;
    }

    float[] getInterphaseDurations() {
        return interphaseDurations;
    }

    public AimsunJunction getJunction() {
        return junction;
    }
}