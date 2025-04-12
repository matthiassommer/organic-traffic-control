package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer2.OptimisationTask;
import de.dfg.oc.otc.layer2.SocketConnection;
import de.dfg.oc.otc.layer2.gui.Layer2Console;
import org.apache.log4j.Logger;
import org.jfree.data.xy.XYSeries;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Collection;

/**
 * Manages the socket connection to a remote host a displays information in a graphical view.
 *
 * @author Matthias Sommer.
 */
class SocketConnector {
    private static final Logger log = Logger.getLogger(SocketConnector.class);
    /**
     * Communication socket.
     */
    SocketConnection socketConnection;
    /**
     * Graphical console.
     */
    Layer2Console l2c;
    /**
     * Seed currently used for AIMSUN simulations.
     */
    int aimsunSeed = -1;
    /**
     * Series of best fitness values for progress display.
     */
    private XYSeries bestSolutions;
    /**
     * Series of average fitness values for progress display.
     */
    private XYSeries averageFitness;

    /**
     * Sets up the contact to the GUI.
     */
    void setupGUI() {
        this.l2c = Layer2Console.getInstance();
        this.bestSolutions = l2c.getBestSeries();
        this.averageFitness = l2c.getAverageSeries();
    }

    void updateConsole(LocalTime startTime) {
        int duration = Duration.between(startTime, LocalTime.now()).getNano();
        l2c.printEAInfo("DURATION (millisec.) " + duration / 1000000);
    }

    final int getAimsunSeed() {
        return aimsunSeed;
    }

    /**
     * Sets the random seed for AIMSUN.
     */
    final void sendAIMSUNSeed(int aimsunSeed) {
        try {
            // New generation starts
            // send new replication seed to AIMSUN
            socketConnection.send("NEW_GEN");
            String recv = socketConnection.recv();
            if (!recv.equals("NEW_GEN_RECV")) {
                l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected NEW_GEN_RECV.");
                log.warn("Socket protocol error: Received " + recv + ", expected NEW_GEN_RECV.");
            }

            socketConnection.send(String.valueOf(aimsunSeed));
            recv = socketConnection.recv();
            if (!recv.equals("SEED_SET")) {
                l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected SEED_SET.");
                log.warn("Socket protocol error: Received " + recv + ", expected SEED_SET.");
            }

            l2c.printEAInfo("Seed set to " + aimsunSeed);
        } catch (IOException e) {
            l2c.printEAWarning(e.getMessage());
            log.warn(e.getMessage());
        }
    }

    /**
     * Saves the console and the chart to a log file.
     */
    void saveLogs(String filenamePrefix, OptimisationTask task) {
        this.l2c.writeEAConsoleToFile(new File("logs/" + filenamePrefix + "_OptLog_" + task.getTaskID() + ".txt"));
        this.l2c.saveChartPanel(new File("logs/" + filenamePrefix + "_OptLog_" + task.getTaskID() + ".png"));
    }

    void sendNewSimulationDuration(final int duration) {
        try {
            socketConnection.send("NEW_SIMDUR");
            String recv = socketConnection.recv();
            if (!recv.equals("NEW_SIMDUR_RECV")) {
                l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected NEW_SIMDUR_RECV.");
                log.warn("Socket protocol error: Received " + recv + ", expected NEW_SIMDUR_RECV.");
            }

            socketConnection.send(String.valueOf(duration));
            recv = socketConnection.recv();
            if (!recv.equals("SIMDUR_SET")) {
                l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected SIMDUR_SET.");
                log.warn("Socket protocol error: Received " + recv + ", expected SIMDUR_SET.");
            }

            l2c.printEAInfo("[setSimulationDuration] -> " + duration);
        } catch (IOException e) {
            l2c.printEAWarning(e.getMessage());
            log.warn(e.getMessage());
        }
    }

    /**
     * Prints the best solution and the average fitness to the console.
     *
     * @param afterSim status before generation start or after generation end?
     */
    void printStatus(final boolean afterSim, Collection<Individual> population, OptimisationTask task, int generationCounter, int aimsunSeed, Individual bestSolution, Individual bestSolutionAllTime) {
        boolean drawChart = DefaultParams.L2_DRAWCHART;
        if (!afterSim) {
            l2c.printEAInfo("Generation " + generationCounter);

            if (drawChart) {
                l2c.setChartTitle("Node " + task.getNodeID());
            }
        } else {
            // Add best solution to chart
            if (drawChart && bestSolutions != null) {
                if (!Double.isInfinite(bestSolution.fitness)) {
                    bestSolutions.add(generationCounter, bestSolution.fitness);
                }
            }
            l2c.printEAInfo("BEST " + bestSolutionAllTime);

            // calculate avg. fitness
            float avgFitness = 0;
            if (!population.isEmpty()) {
                for (Individual individual : population) {
                    avgFitness += individual.fitness;
                }
                avgFitness /= (float) population.size();
            }

            // add to chart
            if (drawChart && averageFitness != null && !Float.isInfinite(avgFitness)) {
                averageFitness.add(generationCounter, avgFitness);
            }
        }
    }

    void initGUI(OptimisationTask task) {
        // Reset chart and console
        l2c.resetChartSeries();
        l2c.clearEAConsole();

        // Get hostname
        try {
            l2c.printEAInfo("HOST " + InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e1) {
            l2c.printEAWarning(e1.getMessage());
            log.warn(e1.getMessage());
        }
        l2c.printEAInfo(String.valueOf(task));
    }

    /**
     * Sends the {@code OptTask} data via socket to Python/AIMSUN.
     */
    void sendOptimisationTask(OptimisationTask task, int eaId) {
        try {
            socketConnection = SocketConnection.getInstance(eaId);
            sendFileNameOfTask(task, eaId);
            sendReplicationID(task.getReplicationId());
            sendTime(task.getTime());
            sendNodeID(task.getNodeID());
            sendSimulationDuration(task.getEAConfig().getSimulationDuration());
            sendWarmupDuration(task.getEAConfig().getWarmUpDuration());
            sendTrafficSituation(task.getSituation());
            sendSectionIDs(task.getSectionIDsForSituation());
        } catch (IOException e) {
            l2c.printEAWarning(e.getMessage());
        }
    }

    private void sendSectionIDs(int[] sectionIDs) throws IOException {
        for (int sectionId : sectionIDs) {
            socketConnection.send(String.valueOf(sectionId));
            String recv = socketConnection.recv();
            if (!recv.equals("SECTION_ID_OK")) {
                l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected SECTION_ID_OK.");
                log.warn("Socket protocol error: Received " + recv + ", expected SECTION_ID_OK.");
            }
        }

        socketConnection.send("SECTION_IDS_DONE");

        String recv = socketConnection.recv();
        if (!recv.equals("SECTION_IDS_DONE_OK")) {
            l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected SECTION_IDS_DONE_OK");
            log.warn("Socket protocol error: Received " + recv + ", expected SECTION_IDS_DONE_OK");
        }
    }

    /**
     * Send traffic situation (if available, otherwise determined by point in time)
     */
    private void sendTrafficSituation(float[] situation) throws IOException {
        for (float aSituation : situation) {
            socketConnection.send(String.valueOf(aSituation));
            String recv = socketConnection.recv();
            if (!recv.equals("SITUATION_ENTRY_OK")) {
                l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected SITUATION_ENTRY_OK.");
                log.warn("Socket protocol error: Received " + recv + ", expected SITUATION_ENTRY_OK.");
            }
        }

        socketConnection.send("SITUATION_DONE");
        String recv = socketConnection.recv();
        if (!recv.equals("SITUATION_DONE_OK")) {
            l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected SITUATION_DONE_OK.");
            log.warn("Socket protocol error: Received " + recv + ", expected SITUATION_DONE_OK.");
        }
    }

    private void sendWarmupDuration(int warmUpDuration) throws IOException {
        socketConnection.send(String.valueOf(warmUpDuration));
        String recv = socketConnection.recv();
        if (!recv.equals("WARMDUR_OK")) {
            l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected WARMDUR_OK.");
            log.warn("Socket protocol error: Received " + recv + ", expected WARMDUR_OK.");
        }
    }

    private void sendSimulationDuration(int simulationDuration) throws IOException {
        socketConnection.send(String.valueOf(simulationDuration));
        String recv = socketConnection.recv();
        if (!recv.equals("SIMDUR_OK")) {
            l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected SIMDUR_OK.");
            log.warn("Socket protocol error: Received " + recv + ", expected SIMDUR_OK.");
        }
    }

    private void sendNodeID(int nodeID) throws IOException {
        socketConnection.send(String.valueOf(nodeID));
        String recv = socketConnection.recv();
        if (!recv.equals("NODE_ID_OK")) {
            l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected NODE_ID_OK.");
            log.warn("Socket protocol error: Received " + recv + ", expected NODE_ID_OK.");
        }
    }

    /**
     * Send point in time (i.e. seconds passed since simulation start)
     * This is needed to determine the relevant traffic demand.
     */
    private void sendTime(float time) throws IOException {
        socketConnection.send(String.valueOf(time));
        String recv = socketConnection.recv();
        if (!recv.equals("TIME_OK")) {
            l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected TIME_OK.");
            log.warn("Socket protocol error: Received " + recv + ", expected TIME_OK.");
        }
    }

    private void sendReplicationID(int replicationId) throws IOException {
        socketConnection.send(String.valueOf(replicationId));
        String recv = socketConnection.recv();
        if (!recv.equals("REPLICATION_ID_OK")) {
            l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected REPLICATION_ID_OK.");
            log.warn("Socket protocol error: Received " + recv + ", expected REPLICATION_ID_OK.");
        }
    }

    private void sendFileNameOfTask(OptimisationTask task, int eaId) throws IOException {
        String angFileName = "NONE";
        if (task.hasFileData()) {
            angFileName = "layer2Tasks/" + task.getNodeID() + "_" + eaId + ".ang";

            final File networkFile = new File(angFileName);

            final File parentDirectory = networkFile.getParentFile();
            if (parentDirectory == null) {
                parentDirectory.mkdirs();
            }

            task.writeTo(new File(angFileName));
        }

        socketConnection.send(angFileName);
        String recv = socketConnection.recv();

        if (!recv.equals("ANGFILE_OK")) {
            l2c.printEAWarning("Socket protocol error: Received " + recv + ", expected ANGFILE_OK.");
            log.warn("Socket protocol error: Received " + recv + ", expected ANGFILE_OK.");
        }
    }
}
