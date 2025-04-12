package de.dfg.oc.otc.aid.algorithms.apid;

import de.dfg.oc.otc.aid.AimsunPolicyStatus;
import de.dfg.oc.otc.aid.evaluation.CongestionMetrics;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.tools.FileUtilities;

import java.io.*;

/**
 * Created by Dietmar on 28.04.2017.
 */
public class APIDAutoIncidentDetection extends APIDAlgorithm {

    /**
     * Confusion Matrix values for non-GUI usage
     */
    private int TP = 0;
    private int TN = 0;
    private int FP = 0;
    private int FN = 0;

    /**
     * The output folder for metrics
     */
    private String evalFolder = "..\\Studentische Arbeiten\\PM Sach\\Evaluation\\3 Auswertung\\APIDAutoIncidentDetection\\";

    /**
     * Output print stream for metrics
     */
    private PrintStream psMetrics;
    private PrintStream psIncidents;
    private PrintStream psState;

    private boolean initialized = false;

    public APIDAutoIncidentDetection()
    {
        super();
    }

    /**
     * Override the inherited method implementing a guard because it's often called twice
     */
    @Override
    public void finalizeInitialization()
    {
        if (initialized)
            return;
        else
            initialized = true;

        super.finalizeInitialization();
        this.evalFolder += OTCManager.getInstance().getReplicationID() + "\\" + this.identifier + "\\";
        initFileWriters();

        if (this.identifier.equals("977978979980")) {
//            APIDParameterSelector.setParams(this);

            int replication = OTCManager.getInstance().getReplicationID();
            int start = 10003055;
            if (start <= replication && replication < start+30)
            {
                BufferedReader br;
                try {
                    br = new BufferedReader(new FileReader("idx.txt"));
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                        line = br.readLine();
                    }
                    String content = sb.toString().replace("\r", "").replace("\n", "");
                    br.close();
                    int idx = Integer.parseInt(content);
                    APIDParameterSelector.setParams(this, idx);
                }
                catch (Exception e)
                {
                    System.out.println("EXCEPTION: " + e.toString());
                }
//                APIDParameters p = new APIDParameters(false, false, true, 80.0f, -0.2f, 0.4f, -1.3f, -1.5f, 50.0f, 50.0f, 80.0f, 0.0f, 20.8f, 0.8f, 0.1f);
//                p.setParameters(this);
            }
            else
            {
                // leave default APID params which are from the paper
            }
        }

        // speed up simulation (without clicking in GUI)
        OTCManager.getInstance().setLayer2Busy(false);

        println("Initialized " + OTCManager.getInstance().getReplicationID() + " with id " + this.identifier);
    }

    /**
     * Initialise file writers for metrics
     */
    private void initFileWriters() {
        try {
            String allMetrics = this.evalFolder + "0_All_Metrics.csv";
//            System.out.println(fileName);
            FileUtilities.createNewFile(allMetrics);
            psMetrics = new PrintStream(new FileOutputStream(allMetrics), true);
//            psMetrics.println("FScore;MCC;TP;TN;FP;FN");
            psMetrics.println("F-measure;MCC;Precision;Accuracy;Specificity;Sensitivity;TP;TN;FP;FN");

            String incidents = this.evalFolder + "0_Incidents.csv";
            FileUtilities.createNewFile(incidents);
            psIncidents = new PrintStream(new FileOutputStream(incidents), true);
            psIncidents.println("Time;InciActive;InciDetected");

            String state = this.evalFolder + "0_State.csv";
            FileUtilities.createNewFile(state);
            psState = new PrintStream(new FileOutputStream(state), true);
            psState.println("Time;InternalState");

        } catch (FileNotFoundException e) {
            println(e.getMessage());
        }
    }

    @Override
    protected void executeAlgorithm(float time, String pairIdentifier, String upstreamDetectorID, String downstreamDetectorID) {
        super.executeAlgorithm(time, pairIdentifier, upstreamDetectorID, downstreamDetectorID);
        APIDState currentState = algorithmStates.get(pairIdentifier);
        psState.println(time + ";" + currentState);
    }

    /**
     * Is called at the end of the simulation to print evaluation
     */
    @Override
    public void finish() {
        String tpfp = CongestionMetrics.fmeasure(TP, FP, FN) +
                ";" + CongestionMetrics.mcc(TP, TN, FP, FN) +
                ";" + CongestionMetrics.precision(TP, FP) +
                ";" + CongestionMetrics.accuracy(TP, TN, FP, FN) +
                ";" + CongestionMetrics.specificity(TN, FP) +
                ";" + CongestionMetrics.sensitivity(TP, FN) +
                ";" + TP +
                ";" + TN +
                ";" + FP +
                ";" + FN +
                ";" + getParametersCSV();

        // print the metrics
        psMetrics.println(tpfp);

        psMetrics.close();
        psIncidents.close();
        psState.close();
    }

    /**
     * Sets the algorithm state to confirmed and reports a new incident for a
     * given detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    @Override
    protected void incidentConfirmed(float time, String pairIdentifier) {
        super.incidentConfirmed(time, pairIdentifier);
        this.reportIncident(time, true);
    }

    /**
     * Sets the algorithm state to free and removes tentative incidents for a
     * given detector pair combination.
     *
     * @param time           of the algorithm execution
     * @param pairIdentifier String combining the ids of two detector pairs
     */
    @Override
    protected void incidentFree(float time, String pairIdentifier) {
        super.incidentFree(time, pairIdentifier);
        this.reportIncident(time, false);
    }

    /**
     * Determines the values for the confusion matrix and retrieve the actual status of incident causing policies
     * @param incidentDetected Whether APID detected an incident
     */
    private void reportIncident(float time, boolean incidentDetected)
    {
        // get the real incident status from Aimsun API
        boolean incidentPolicyActive = false;
        for (AbstractDetectorGroup pair : getMonitoringZone().getMonitoredDetectorPairs())
        {
            if (AimsunPolicyStatus.isAnyPolicyActive(pair.getId()))
            {
                incidentPolicyActive = true;
                break;
            }
        }

        // check condition and prediction (confusion matrix)
        if (incidentPolicyActive && incidentDetected)
        {
            TP++;
            println("CLASSIFIED CORRECTLY congestion");
        }
        else if (!incidentPolicyActive && incidentDetected)
        {
            FP++;
            println("CLASSIFIED INCORRECTLY congestion!");
        }
        else if (incidentPolicyActive && !incidentDetected)
        {
            FN++;
            println("CLASSIFIED NOT CORRECTLY free!");
        }
        else if (!incidentPolicyActive && !incidentDetected)
        {
            TN++;
            println("CLASSIFIED CORRECTLY free");
        }

        psIncidents.println(time + ";" + incidentPolicyActive + ";" + incidentDetected);
    }

    public String getParametersCSV()                // 40.0;-0.2;0.2;-1.7;-2.0;50.0;50.0;5.5;0.2;45.0;0.8;0.8
    {
        String s = this.MEDIUM_TRAFFIC_DETECTION_ENABLED + ";";
        s += this.COMPRESSION_WAVE_TEST_ENABLED + ";";
        s += this.PERSISTENCE_TEST_ENABLED + ";";
        s += this.TH_MEDIUM_TRAFFIC + ";";
        s += this.TH_INC_CLR + ";";
        s += this.TH_PT + ";";
        s += this.TH_CW1 + ";";
        s += this.TH_CW2 + ";";
        s += this.PERSISTENCE_TEST_PERIOD + ";";
        s += this.COMPRESSION_WAVE_TEST_PERIOD + ";";
        s += this.TH_ID1 + ";";
        s += this.TH_ID2 + ";";
        s += this.TH_ID3 + ";";
        s += this.TH_MED_ID1 + ";";
        s += this.TH_MED_ID2;
        return s;
    }

    private void println(String str)
    {
        if (this.identifier.equals("977978979980"))
            System.out.println(str);
    }
}
