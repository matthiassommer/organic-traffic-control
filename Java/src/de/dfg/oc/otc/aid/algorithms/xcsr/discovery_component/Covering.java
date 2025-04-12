package de.dfg.oc.otc.aid.algorithms.xcsr.discovery_component;


import de.dfg.oc.otc.aid.algorithms.xcsr.RClassifier;
import de.dfg.oc.otc.aid.algorithms.xcsr.Situation;
import de.dfg.oc.otc.aid.algorithms.xcsr.XCSRConstants;

/**
 * Created by Anthony on 09.09.2014.
 */
public abstract class Covering {
    //Standard Covering
    public static RClassifier create(Situation sigma_t, int numActions) {
        return new RClassifier(
                sigma_t,
                sigma_t.getProblemCounter(),
                (int) (numActions * XCSRConstants.drand()),
                XCSRConstants.predictionIni,
                XCSRConstants.predictionErrorIni,
                XCSRConstants.fitnessIni);
    }

    //Standard Covering: Create classifiers for missing action
    public static RClassifier create(Situation sigma_t, int action, int numActions) {
        return new RClassifier(
                sigma_t,
                sigma_t.getProblemCounter(),
                action,
                XCSRConstants.predictionIni,
                XCSRConstants.predictionErrorIni,
                XCSRConstants.fitnessIni);
    }
}
