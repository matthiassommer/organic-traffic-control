package de.dfg.oc.otc.aid.algorithms.xcsrUrban;

import com.sun.corba.se.spi.orb.Operation;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.discovery_component.Covering;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.discovery_component.GA;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.environments.CongestionDetectionEnvironment;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.performance_component.ActionSelection;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.performance_component.Matching;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.performance_component.PredictionArray;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.reinforcement_component.Reinforcement;
import de.dfg.oc.otc.aid.evaluation.CongestionMetrics;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.tools.FileUtilities;
import de.dfg.oc.otc.tools.MathFunctions;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import tests.evaluation.aid.xcsrUrban.LoadDataXCSRU;

import java.io.*;
import java.util.*;

/**
 * Created by Anthony on 08.09.2014.
 * Edited by Dietmar on 24.04.2017
 */
public class XCSRUrban extends AbstractAIDAlgorithm {

    //    private final CongestionDetectionEnvironment env;
    private Population population;                //
    private List<Evaluation> overallEvaluation;
    private PredictionArray pa;
    private Evaluation evaluation;
    private int gaCounter = 0;
    private int coveringCounter = 0;
    private int problemCounter = 0;
    private int a_exec;
    private Matching lastMatching;
    private int exploreProblem;
    public int TP = 0;
    public int FP = 0;
    public int TN = 0;
    public int FN = 0;
    /**
     * Exports the system error and fraction correct of the XCSRUrban.
     */
    private PrintStream psPerformance = null;
    /**
     * Exports the performance log messages.
     */
    private PrintStream psStatistics = null;

    /**
     * Exports the complex congestion/classification error metrics.
     */
    private PrintStream psAllMetrics = null;


    public PrintStream psIncidents = null;

    /**
     * Exports values for the confusions matrix (TP,FP,FN,TN).
     */
//    private PrintStream psConfusionMatrix = null;
    private String outputFolder;
    public XCSRUrbanParameters parameters;
    private CongestionDetectionEnvironment env;
    private boolean initialized = false;

//    public LinkedHashSet<String> detectorPairs = new LinkedHashSet<>();

    /**
     * Contains the latest occupancy and speed values of all observed detector
     * pairs. First element in the queue is the oldest, last is the newest
     */
    public final TreeMap<String, float[]> detectorPairValues;


    /**
     * used in Parameter evaluation
     * @param env
     * @param folder
     * @param parameters
     */
    public XCSRUrban(CongestionDetectionEnvironment env, String folder, XCSRUrbanParameters parameters) {
//        setObservedDetectorCount(2);
        this.parameters = parameters;
        this.parameters.setSeed(1578935852);
        this.env = env;
        this.population = new Population(this.parameters, this.parameters.maxPopSize);
        this.evaluation = new Evaluation(4000);
        this.overallEvaluation = new ArrayList<>();
        this.outputFolder = folder;
        this.detectorPairValues = new TreeMap<>();
    }

    /**
     * Called from AIDAlgorithmFactory
     */
    public XCSRUrban()
    {
        boolean untrainedPopulation = false;
        this.evaluation = new Evaluation(50);
        this.overallEvaluation = new ArrayList<>();
        this.outputFolder = "..\\Studentische Arbeiten\\PM Sach\\Evaluation\\3 Auswertung\\XCSRURBAN" + (untrainedPopulation ? "_untrained\\" : "\\");
        this.detectorPairValues = new TreeMap<>();

        try
        {
            this.env = new CongestionDetectionEnvironment();
            env.init(this, false, true, true, false, null, true, outputFolder);

            if (!untrainedPopulation && deserializePopulation("..\\Studentische Arbeiten\\PM Sach\\Evaluation\\2 Parameterstudie\\AID\\optimal\\0\\0_population.dat")) {
                this.parameters = this.getPopulation().parameters;
                env.parameters = this.getPopulation().parameters;
                this.exploreProblem = 0;
            }
            else {
                System.out.println("Population laden nicht gewünscht oder nicht erfolgreich. Fange bei 0 an.");
                this.parameters = env.parameters;
                this.population = new Population(this.parameters, this.parameters.maxPopSize);
                this.exploreProblem = 1;
            }

            // in previously exported csv traffic data no higher values than 65 for speed and 101 for occupancy were recorded. Based on this experience, arbitrary values are selected here
            env.setMaxValues(0.0, 0.0, 100, 100, 200, 200, 0.0, 0.0);


            this.parameters.setSeed(1578935853);
//        this.startExperiment(i + 1, (this.data.length - this.timeStepOffset * LoadDataXCSRU.SITUATION_LENGTH) / LoadDataXCSRU.SITUATION_LENGTH);   //trialsPerExperiment = Number of feature fectors per run

//        this.trainingPhaseFinished = false;

            TP = 0;
            FP = 0;
            TN = 0;
            FN = 0;
        }
        catch (Exception e)
        {
            System.out.println("EXCEPTION: " + e.toString());
        }
    }

    @Override
    public void finalizeInitialization()
    {
        if (initialized)
            return;
        else
            initialized = true;

        super.finalizeInitialization();

        env.evalFolder += OTCManager.getInstance().getReplicationID() + "\\" + this.identifier + "\\0_";         // a prefix for each evaluation data folder
        env.initFileWriters();
        initFileWriters(this.outputFolder + OTCManager.getInstance().getReplicationID() + "\\" + this.identifier + "\\", 0);

        // print the parameters used
        env.psParameters.println(this.parameters.printParameters());

    }

    public int getProblemCounter() {
        return problemCounter;
    }



    public void startExperiment(int experiment, int trialsPerExperiment)
    {
        TP = 0;
        FP = 0;
        TN = 0;
        FN = 0;
        initFileWriters(this.outputFolder, experiment);

        this.exploreProblem = 1;
        for (this.problemCounter = 0; this.problemCounter < trialsPerExperiment; this.problemCounter++) // for every feature vector of the current day
        {
            boolean end = problemCounter == trialsPerExperiment - 1;

            if (end || problemCounter > LoadDataXCSRU.NUMBER_TRAINING_DAYS * LoadDataXCSRU.NUMBER_DATA_PER_DAY) {  // training finished?
                this.exploreProblem = 0;
            }

            runMainLoop();

            // Check if the data of one day was processed.
            int diff = (LoadDataXCSRU.NUMBER_DATA_PER_DAY * LoadDataXCSRU.SITUATION_LENGTH) - trialsPerExperiment;   // the latter is less due to the forecast bias (=6)
            if ((problemCounter + diff) % LoadDataXCSRU.NUMBER_DATA_PER_DAY == 0 || end)
            {
                if (this.exploreProblem == 0 || end)   // this runs for each day after the training phase
                {
                    // After each day, print evaluation
                    writeAllMetrics(false);
                }
            }
        }
//        System.out.println("finished at " + this.problemCounter);

        psPerformance.close();
        psStatistics.close();
        psAllMetrics.close();
//        psIncidents.close();
//        psConfusionMatrix.close();
    }

    private void runMainLoop() {
        boolean explore = (this.exploreProblem == 1);
        Situation sigma_t = new Situation(env.resetState());
        this.lastMatching = new Matching(sigma_t, this.population, env.getNrActions());

        //Discovery Component: Covering, create a classifier for each missing action
        boolean[] missingActions = this.lastMatching.getMissingActionsInMatchSet();
        for (int i = 0; i < missingActions.length; i++) {
            if (!missingActions[i]) {
                RClassifier classifier = Covering.create(this.parameters, sigma_t, i, this.parameters.theta_mna);
                this.population.addClassifier(classifier);
                this.coveringCounter++;
            }
        }

        // * SOMETIMES THIS DOES NOT MATCH DESPITE JUST HAVING ADDED A CLASSIFIER
        this.lastMatching = new Matching(sigma_t, this.population, env.getNrActions());

        if (this.lastMatching.getMatchSet().isEmpty()) {    // * happens when this.lastMatching matchSize = 0
            System.out.println("[M] empty");
            // * AN EMPTY MATCH SET WOULD LEAD TO EXCEPTIONS IN AS/GA so just return and don't do reinforcement
            // * VERY VERY UGLY workaround....
            return;
        }

        this.pa = new PredictionArray(this.lastMatching, env.getNrActions());

        //Action-Selection-Regime
        ActionSelection as;
        if (explore) {
            as = new ActionSelection(this.parameters, this.pa, ActionSelection.ACTION_SELECTION_REGIME.ROULETTE_WHEEL);
        } else {
            as = new ActionSelection(this.parameters, this.pa, ActionSelection.ACTION_SELECTION_REGIME.BEST_ACTION_WINNER);
        }
        this.a_exec = as.getActionToExecute();

        //Discovery Component: GA
        if (explore) {
            GA ga = new GA(this.parameters, lastMatching.generateActionSet(this.a_exec), problemCounter, this.env.getNrActions());
            if (ga.run()) {
                this.gaCounter++;
                this.population.addClassifiers(ga.getOffspring());
            }
        }

        int payoff = (int) env.executeAction(a_exec);
        receiveReward(payoff);
    }

    private void receiveReward(int reward) {
        Reinforcement reinforcement = new Reinforcement(this.parameters, reward, this.a_exec, this.lastMatching);
        reinforcement.doReinforcement();

        // Do evaluation after training phase finished only
        if (this.exploreProblem == 0 || env.operationMode == OperationMode.AIMSUN_CLASSIFICATION || env.operationMode == OperationMode.OFFLINE_TRAINING_ONLY)
        {
            if (this.evaluation.evaluationCycleHasFinished()) {
                writeEvaluationData();
                this.overallEvaluation.add(this.evaluation);
                this.evaluation = new Evaluation();
            }
            this.evaluation.updateEvaluation(
                    env.wasCorrect(),
                    Math.abs(reward - this.pa.getPA()[this.a_exec]),
                    this.population.getRealSize()
            );
        }
    }

    private void writeEvaluationData() {
        psPerformance.println(this.problemCounter + ";" + this.evaluation.toString());
    }

    public void setSeedForExperiment(int expNr) {
        this.parameters.setSeed(SeedGenerator.getTrueRandomSeedForExperiment(expNr));
    }

    /**
     *
     * @param folder
     * @param experiment add this to the filenames as a prefix when you have more than one EVALUATION_FOLDERs.
     */
    private void initFileWriters(String folder, int experiment) {
        try {
            String performanceFilename = folder + experiment + "_Performance.csv";
            FileUtilities.createNewFile(performanceFilename);
            psPerformance = new PrintStream(new FileOutputStream(performanceFilename), true);
            psPerformance.println("Step;FractionCorrect;SystemError;PopulationSize");

            String statisticsFilename = folder + experiment + "_Statistics.csv";
            FileUtilities.createNewFile(statisticsFilename);
            psStatistics = new PrintStream(new FileOutputStream(statisticsFilename), true);
            psStatistics.println("Metric;Mean;StdDev.;Conf.IntervalLow;Conf.IntervalHigh");

            String metricFilename = folder + experiment + "_All_Metrics.csv";
            FileUtilities.createNewFile(metricFilename);
            psAllMetrics = new PrintStream(new FileOutputStream(metricFilename), true);
            psAllMetrics.println("F-measure;MCC;Precision;Accuracy;Specificity;Sensitivity;TP;TN;FP,FN");

//            String confusionFilename = folder + experiment + "_ConfusionMatrix.csv";
//            FileUtilities.createNewFile(confusionFilename);
//            psConfusionMatrix = new PrintStream(new FileOutputStream(confusionFilename), true);
//            psConfusionMatrix.println("TP;FP;TN;FN");

//            String incidents = folder + experiment + "_Incidents.csv";
//            FileUtilities.createNewFile(incidents);
//            psIncidents = new PrintStream(new FileOutputStream(incidents), true);
//            psIncidents.println("Time;InciActive;InciDetected");


        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private void writeAllMetrics(boolean runOnce) {
        printOverallEvaluation();
        this.overallEvaluation = new ArrayList<>();

        String tpfp = CongestionMetrics.fmeasure(TP, FP, FN) +
                ";" + CongestionMetrics.mcc(TP, TN, FP, FN) +
                ";" + CongestionMetrics.precision(TP, FP) +
                ";" + CongestionMetrics.accuracy(TP, TN, FP, FN) +
                ";" + CongestionMetrics.specificity(TN, FP) +
                ";" + CongestionMetrics.sensitivity(TP, FN) +
                ";" + TP +
                ";" + TN +
                ";" + FP +
                ";" + FN;

        // print the metrics
        psAllMetrics.println(tpfp);

        if (!runOnce) {
            TP = 0;
            FP = 0;
            TN = 0;
            FN = 0;
        }
    }

    private void printOverallEvaluation() {
        List<Float> fracCorrects = new ArrayList<>();
        List<Float> sysErrors = new ArrayList<>();
        List<Float> popSizes = new ArrayList<>();

        double overallFracCorrect = 0.0;
        double overallSysError = 0.0;
        double overallAvgPopSize = 0.0;

        if (overallEvaluation.size() > 0)
        {
            for (Evaluation eval : this.overallEvaluation) {
                fracCorrects.add((float) eval.getEvalData()[0]);
                overallFracCorrect += eval.getEvalData()[0];

                sysErrors.add((float) eval.getEvalData()[1] / env.getMaxPayoff());
                overallSysError += eval.getEvalData()[1];

                popSizes.add((float) eval.getEvalData()[2]);
                overallAvgPopSize += eval.getEvalData()[2];
            }

            double meanFracCorrect = overallFracCorrect / overallEvaluation.size();
            float stdDevFracCorrect = MathFunctions.standardDeviation(fracCorrects, (float) meanFracCorrect);
            double[] confIntFraCorrect = MathFunctions.getConfidenceInterval(0.05, fracCorrects.size() - 1, stdDevFracCorrect, meanFracCorrect);
            psStatistics.println("Overall Fraction Correct:;" + meanFracCorrect + ";" + stdDevFracCorrect + ";" + confIntFraCorrect[0] + ";" + confIntFraCorrect[1]);

            double meanSysError = overallSysError / overallEvaluation.size() / env.getMaxPayoff();
            float stdDevSysError = MathFunctions.standardDeviation(sysErrors, (float) meanSysError);
            double[] confIntSysError = MathFunctions.getConfidenceInterval(0.05, sysErrors.size() - 1, stdDevSysError, meanSysError);
            psStatistics.println("Overall System Error:;" + meanSysError + ";" + stdDevSysError + ";" + confIntSysError[0] + ";" + confIntSysError[1]);

            double meanPopSize = overallAvgPopSize / overallEvaluation.size();
            float stdDevPopSize = MathFunctions.standardDeviation(popSizes, (float) meanPopSize);
            double[] confIntPopSize = MathFunctions.getConfidenceInterval(0.05, popSizes.size() - 1, stdDevPopSize, meanPopSize);
            psStatistics.println("Overall Avg. Population:;" + meanPopSize + ";" + stdDevPopSize + ";" + confIntPopSize[0] + ";" + confIntPopSize[1]);
        }
        else
        {
            psStatistics.println("No evaluations");
        }
        psStatistics.println("Number of applied COVERING OPs:;" + this.coveringCounter + "; ;");
        psStatistics.println("Number of applied GA OPs:;" + this.gaCounter + "; ;");
    }

    public Population getPopulation()
    {
        return population;
    }

    public void setPopulation(Population pop)
    {
        population = pop;
    }

    public void log(String msg)
    {
        super.log(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void newDetectorData(AbstractDetectorGroup detectorGroup, DetectorDataValue detectorPairValue) {
        try {
            float time = detectorPairValue.getTime();
            if (time > this.warmupTime) {
                String pairID = detectorGroup.getId();

//                if (detectorPairValues.size() < getRequiredDetectorPairCount()) {
//                    detectorPairValues.putIfAbsent(pairID, detectorPairValue.getValues());
//                }

                // Create field for new detector pair values
                this.detectorPairValues.putIfAbsent(pairID, detectorPairValue.getValues());

                // At the end of each iteration execute algorithm
                boolean enoughMeasurements = detectorPairValues.size() == this.observedDetectorCount;
                boolean nextIteration = time % this.executionInterval + this.simulationStepSize >= this.executionInterval;

                if (nextIteration && enoughMeasurements) {
                    // currently supports two detectorPairs only
                    env.currentSituationAimsun = new double[LoadDataXCSRU.SITUATION_LENGTH];
                    env.currentSituationAimsun[LoadDataXCSRU.TIME] = time;
                    env.currentSituationAimsun[LoadDataXCSRU.DENSITY1] = detectorPairValues.firstEntry().getValue()[DetectorCapabilities.DENSITY];
                    env.currentSituationAimsun[LoadDataXCSRU.DENSITY2] = detectorPairValues.lastEntry().getValue()[DetectorCapabilities.DENSITY];
                    env.currentSituationAimsun[LoadDataXCSRU.OCCUPANCY1] = detectorPairValues.firstEntry().getValue()[DetectorCapabilities.OCCUPANCY];
                    env.currentSituationAimsun[LoadDataXCSRU.OCCUPANCY2] = detectorPairValues.lastEntry().getValue()[DetectorCapabilities.OCCUPANCY];
                    env.currentSituationAimsun[LoadDataXCSRU.SPEED1] = detectorPairValues.firstEntry().getValue()[DetectorCapabilities.SPEED];
                    env.currentSituationAimsun[LoadDataXCSRU.SPEED2] = detectorPairValues.lastEntry().getValue()[DetectorCapabilities.SPEED];
                    env.currentSituationAimsun[LoadDataXCSRU.VOLUME1] = detectorPairValues.firstEntry().getValue()[DetectorCapabilities.COUNT];
                    env.currentSituationAimsun[LoadDataXCSRU.VOLUME2] = detectorPairValues.lastEntry().getValue()[DetectorCapabilities.COUNT];
                    env.currentSituationAimsun[LoadDataXCSRU.SITUATION_LENGTH - 1] = Float.NaN;

                    prepareAndRunAlgorithm(time);

                    detectorPairValues.clear();
                    env.currentSituationAimsun = null;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("EXCEPTION: " + e.toString());
        }
    }

    /**
     * Prepares the algorithm execution
     *
     * @param time of the algorithm execution
     */
    protected void prepareAndRunAlgorithm(float time)
    {
        algorithmApplied();
        problemCounter++;
        runMainLoop();
    }

    /**
     * Is called at the end of the simulation to print evaluation
     */
    @Override
    public void finish()
    {
        writeAllMetrics(true);
        psPerformance.close();
        psAllMetrics.close();
        psStatistics.close();
//        psIncidents.close();
    }

    /**
     * Returns the name of the algorithm (used by the {@link de.dfg.oc.otc.aid.algorithms.AIDAlgorithmFactory}
     * for creating instances).
     */
    public String getName()
    {
        return "XCSRUrban";
    }

    public Map<String, Object> getParameters()
    {
        Map<String, Object> params = new HashMap<>();
        params.put("maxPopSize", parameters.maxPopSize);
        params.put("theta_mna", parameters.theta_mna);
        params.put("minPhenotypeValue", parameters.minPhenotypeValue);
        params.put("maxPhenotypeValue", parameters.maxPhenotypeValue);
        params.put("alpha", parameters.alpha);
        params.put("beta", parameters.beta);
        params.put("delta", parameters.delta);
        params.put("nu", parameters.nu);
        params.put("theta_GA", parameters.theta_GA);
        params.put("epsilon_0", parameters.epsilon_0);
        params.put("theta_del", parameters.theta_del);
        params.put("pX", parameters.pX);
        params.put("pM", parameters.pM);
        params.put("P_dontcare", parameters.P_dontcare);
        params.put("predictionErrorReduction", parameters.predictionErrorReduction);
        params.put("fitnessReduction", parameters.fitnessReduction);
        params.put("theta_sub", parameters.theta_sub);
        params.put("predictionIni", parameters.predictionIni);
        params.put("predictionErrorIni", parameters.predictionErrorIni);
        params.put("fitnessIni", parameters.fitnessIni);
        return params;
    }

    /**
     * Defines the minimal number of detector pairs which are required for this
     * algorithm to function properly.
     *
     * @return Required number of detector pairs
     */
    public int getRequiredDetectorPairCount()
    {
        return 2;
    }

    public boolean isStateMappedToIncident(int state)
    {
        System.out.println("UNKNOWN STATE");
        throw new NotImplementedException();
    }


    public void serializePopulation(String dir)
    {
        ObjectOutputStream oos = null;
        FileOutputStream fout;
        try
        {
            fout = new FileOutputStream(dir + "population.dat", true);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(getPopulation());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if(oos != null)
            {
                try
                {
                    oos.close();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }

    public boolean deserializePopulation(String file)
    {
        Population deserializedPop;
        try
        {
            FileInputStream inputFileStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputFileStream);
            deserializedPop = (Population)objectInputStream.readObject();
            objectInputStream.close();
            inputFileStream.close();
            setPopulation(deserializedPop);
            return true;
        }
        catch(ClassNotFoundException e)
        {
            System.out.println("Exception beim Laden der Population");
            e.printStackTrace();
        }
        catch(IOException i)
        {
            System.out.println("Exception beim Laden der Population");
            i.printStackTrace();
        }
        return false;
    }
}
