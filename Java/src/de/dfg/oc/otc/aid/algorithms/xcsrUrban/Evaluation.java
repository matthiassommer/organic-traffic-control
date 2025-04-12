package de.dfg.oc.otc.aid.algorithms.xcsrUrban;

/**
 * Created by Anthony on 10.09.2014.
 */
public class Evaluation {
    private static int EVAL_STEPS = 100;
    private double fracCorrect;
    private double sysError;
    private double avgPopSize;
    private int datapoints = 0;
    private boolean finished;

    public Evaluation() {
        this.finished = false;
    }
    public Evaluation(int evalSteps) {
        this.EVAL_STEPS = evalSteps;
        this.finished = false;
    }

    public void updateEvaluation(boolean wasCorrect, double sysError, double popSize) {
        if (!finished && datapoints < EVAL_STEPS) {
            this.datapoints++;
            if (wasCorrect) {
                this.fracCorrect++;
            }
            this.sysError += sysError;
            this.avgPopSize += popSize;

            if (this.datapoints == EVAL_STEPS) {
                this.fracCorrect /= EVAL_STEPS;
                this.sysError /= EVAL_STEPS;
                this.avgPopSize /= EVAL_STEPS;

                this.finished = true;
            }
        }
    }

    double[] getEvalData() {
        return new double[]{this.fracCorrect, this.sysError, this.avgPopSize};
    }

    boolean evaluationCycleHasFinished() {
        return this.finished;
    }

    public String toString() {
        float sysErr = Math.round((this.sysError / 1000.0)* 100f)/100f;
        return this.fracCorrect + ";" + sysErr + ";" + this.avgPopSize;
    }
}
