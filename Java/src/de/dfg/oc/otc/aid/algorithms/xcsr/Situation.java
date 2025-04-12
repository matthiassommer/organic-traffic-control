package de.dfg.oc.otc.aid.algorithms.xcsr;

/**
 * Created by Anthony on 09.09.2014.
 */
public class Situation {
    private final double[] sigma_t;
    private int problemCounter;

    public Situation(double[] sigma_t) {
        this.sigma_t = sigma_t;
    }

    public int getProblemCounter() {
        return problemCounter;
    }

    public double[] getFeatureVector() {
        return this.sigma_t;
    }
}
