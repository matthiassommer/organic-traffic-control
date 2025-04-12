package de.dfg.oc.otc.aid.algorithms.xcsrUrban.performance_component;


import de.dfg.oc.otc.aid.algorithms.xcsrUrban.RClassifier;

/**
 * Created by Anthony on 09.09.2014.
 */
public class PredictionArray {
    private final double[] pa;

    public PredictionArray(Matching match, int numAct) {
        pa = new double[numAct];

        for (int i = 0; i < this.pa.length; i++) {
            this.pa[i] = Double.NaN;
        }

        for (int i = 0; i < this.pa.length; i++) {
            double sum_pF = 0.0;
            double sum_F = 0.0;
            for (RClassifier cl : match.getMatchSet()) {
                try {
                    if (cl.action == i) {
                        sum_pF += (cl.prediction * cl.fitness);
                        sum_F += cl.fitness;
                    }
                } catch (NullPointerException e) {
                    System.err.println(e.getMessage());
                }
            }
            pa[i] = sum_pF / sum_F;
        }
    }

    public final double[] getPA() {
        return this.pa;
    }
}
