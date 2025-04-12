package de.dfg.oc.otc.aid.algorithms.xcsr;

import de.dfg.oc.otc.aid.algorithms.xcsr.discovery_component.Covering;
import de.dfg.oc.otc.aid.algorithms.xcsr.discovery_component.GA;
import de.dfg.oc.otc.aid.algorithms.xcsr.environments.ContinuousEnvironment;
import de.dfg.oc.otc.aid.algorithms.xcsr.performance_component.ActionSelection;
import de.dfg.oc.otc.aid.algorithms.xcsr.performance_component.Matching;
import de.dfg.oc.otc.aid.algorithms.xcsr.performance_component.PredictionArray;
import de.dfg.oc.otc.aid.algorithms.xcsr.reinforcement_component.Reinforcement;
import de.dfg.oc.otc.tools.FileUtilities;
import de.dfg.oc.otc.tools.MathFunctions;
import tests.evaluation.aid.AIDTrafficDataReader;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Anthony on 08.09.2014.
 */
public class XCSR {
    private final ContinuousEnvironment env;
    private final Population population;
    private final List<Evaluation> overallEvaluation;
    private PredictionArray pa;
    private Evaluation evaluation;
    private int gaCounter = 0;
    private int coveringCounter = 0;
    private int problemCounter = 0;
    private int a_exec;
    private Matching lastMatching;
    private int exploreProblem;
    private Situation sigma_t;
    /**
     * Exports the system error and fraction correct of the XCSR.
     */
    private PrintStream psXCSRPerformance = null;
    /**
     * Exports the performance log messages.
     */
    private PrintStream psXCSRLog = null;
    private String outputFolder;

    public XCSR(ContinuousEnvironment environment, String folder) {
        XCSRConstants.setSeed(1578935852);
        this.env = environment;
        this.population = new Population(XCSRConstants.maxPopSize);
        this.evaluation = new Evaluation();
        this.overallEvaluation = new ArrayList<>();
        this.outputFolder = folder;
    }

    public int getProblemCounter() {
        return problemCounter;
    }

    public void startExperiment(int experiment, int trialsPerExperiment) {
        initFileWriters(this.outputFolder, experiment);

        this.exploreProblem = 1;
        for (this.problemCounter = 0; this.problemCounter < trialsPerExperiment; this.problemCounter++) {
            boolean trainingFinished = problemCounter > AIDTrafficDataReader.NUMBER_TRAINING_DAYS * AIDTrafficDataReader.NUMBER_DATA_PER_DAY;
            if (trainingFinished) {
                this.exploreProblem = 0;
            }

            runMainLoop();
        }
        printOverallEvaluation();
    }

    private void runMainLoop() {
        boolean explore = (this.exploreProblem == 1);
        this.sigma_t = new Situation(env.resetState());
        this.lastMatching = new Matching(sigma_t, this.population, env.getNrActions());

        //Discovery Component: Covering, create a classifier for each missing action
        boolean[] missingActions = this.lastMatching.getMissingActionsInMatchSet();
        for (int i = 0; i < missingActions.length; i++) {
            if (!missingActions[i]) {
                RClassifier classifier = Covering.create(sigma_t, i, XCSRConstants.theta_mna);
                this.population.addClassifier(classifier);
                this.lastMatching = new Matching(sigma_t, this.population, env.getNrActions());
                this.coveringCounter++;
            }
        }

        if (this.lastMatching.getMatchSet().isEmpty()) {
            System.out.println("[M] empty");
        }

        this.pa = new PredictionArray(this.lastMatching, env.getNrActions());

        ActionSelection as;
        //Action-Selection-Regime
        if (explore) {
            as = new ActionSelection(this.pa, ActionSelection.ACTION_SELECTION_REGIME.ROULETTE_WHEEL);
        } else {
            as = new ActionSelection(this.pa, ActionSelection.ACTION_SELECTION_REGIME.BEST_ACTION_WINNER);
        }
        this.a_exec = as.getActionToExecute();

        //Discovery Component: GA
        if (explore) {
            GA ga = new GA(lastMatching.generateActionSet(this.a_exec), problemCounter, this.env.getNrActions());
            if (ga.run()) {
                this.gaCounter++;
                this.population.addClassifiers(ga.getOffspring());
            }
        }

        int payoff = (int) env.executeAction(a_exec);
        receiveReward(payoff);
    }

    private void receiveReward(int reward) {
        Reinforcement reinforcement = new Reinforcement(reward, this.a_exec, this.lastMatching);
        reinforcement.doReinforcement();

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

    private void writeEvaluationData() {
        psXCSRPerformance.println(this.problemCounter + "\t" + this.evaluation.toString());
    }

    public void setSeedForExperiment(int expNr) {
        XCSRConstants.setSeed(SeedGenerator.getTrueRandomSeedForExperiment(expNr));
    }

    private void initFileWriters(String folder, int experiment) {
        try {
            FileUtilities.createNewFile(folder + "\\performance_exp" + experiment + ".txt");
            psXCSRPerformance = new PrintStream(new FileOutputStream(folder + "\\performance_exp" + experiment + ".txt"), true);
            psXCSRPerformance.println("Index\tFractionCorrect\tSystemError\tPopulationSize");

            FileUtilities.createNewFile(folder + "\\statistics_exp" + experiment + ".txt");
            psXCSRLog = new PrintStream(new FileOutputStream(folder + "\\statistics_exp" + experiment + ".txt"), true);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private void printOverallEvaluation() {
        List<Float> fracCorrects = new ArrayList<>();
        List<Float> sysErrors = new ArrayList<>();
        List<Float> popSizes = new ArrayList<>();

        double overallFracCorrect = 0.0;
        double overallSysError = 0.0;
        double overallAvgPopSize = 0.0;

        for (Evaluation eval : this.overallEvaluation) {
            fracCorrects.add((float) eval.getEvalData()[0]);
            overallFracCorrect += eval.getEvalData()[0];

            sysErrors.add((float) eval.getEvalData()[1] / env.getMaxPayoff());
            overallSysError += eval.getEvalData()[1];

            popSizes.add((float) eval.getEvalData()[2]);
            overallAvgPopSize += eval.getEvalData()[2];
        }

        psXCSRLog.println("Metric \t Mean \t StdDev. \t Conf.Interval");

        double meanFracCorrect = overallFracCorrect / overallEvaluation.size();
        float stdDevFracCorrect = MathFunctions.standardDeviation(fracCorrects, (float) meanFracCorrect);
        double[] confIntFraCorrect = MathFunctions.getConfidenceInterval(0.05, fracCorrects.size() - 1, stdDevFracCorrect, meanFracCorrect);
        psXCSRLog.println("Overall Fraction Correct:\t" + meanFracCorrect + "\t" + stdDevFracCorrect + "\t" + Arrays.toString(confIntFraCorrect));

        double meanSysError = overallSysError / overallEvaluation.size() / env.getMaxPayoff();
        float stdDevSysError = MathFunctions.standardDeviation(sysErrors, (float) meanSysError);
        double[] confIntSysError = MathFunctions.getConfidenceInterval(0.05, sysErrors.size() - 1, stdDevSysError, meanSysError);
        psXCSRLog.println("Overall System Error:\t" + meanSysError + "\t" + stdDevSysError + "\t" + Arrays.toString(confIntSysError));

        double meanPopSize = overallAvgPopSize / overallEvaluation.size();
        float stdDevPopSize = MathFunctions.standardDeviation(popSizes, (float) meanPopSize);
        double[] confIntPopSize = MathFunctions.getConfidenceInterval(0.05, popSizes.size() - 1, stdDevPopSize, meanPopSize);
        psXCSRLog.println("Overall Avg. Population:\t" + meanPopSize + "\t" + stdDevPopSize + "\t" + Arrays.toString(confIntPopSize));

        psXCSRLog.println("Number of applied COVERING OPs:\t" + this.coveringCounter + "\t \t");
        psXCSRLog.println("Number of applied GA OPs:\t" + this.gaCounter + "\t \t");
    }
}
