package de.dfg.oc.otc.aid.algorithms.xcsrUrban.discovery_component;


import de.dfg.oc.otc.aid.algorithms.xcsrUrban.RClassifier;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.Situation;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrbanParameters;

/**
 * Created by Anthony on 09.09.2014.
 */
public abstract class Covering {
    private XCSRUrbanParameters constants;
    //Standard Covering
    public static RClassifier create(XCSRUrbanParameters constants, Situation sigma_t, int numActions) {
        return new RClassifier(
                constants,
                sigma_t,
                sigma_t.getProblemCounter(),
                (int) (numActions * constants.drand()),
                constants.predictionIni,
                constants.predictionErrorIni,
                constants.fitnessIni);
    }

    //Standard Covering: Create classifiers for missing action
    public static RClassifier create(XCSRUrbanParameters constants, Situation sigma_t, int action, int numActions) {
        return new RClassifier(
                constants,
                sigma_t,
                sigma_t.getProblemCounter(),
                action,
                constants.predictionIni,
                constants.predictionErrorIni,
                constants.fitnessIni);
    }
}
