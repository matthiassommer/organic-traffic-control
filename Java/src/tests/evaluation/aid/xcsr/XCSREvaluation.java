package tests.evaluation.aid.xcsr;

import de.dfg.oc.otc.aid.algorithms.xcsr.environments.CongestionDetectionEnvironment;

/**
 * Created by oc6admin on 03.02.2016.
 */
public class XCSREvaluation {
    public static void main(String[] args) {
        CongestionDetectionEnvironment env = new CongestionDetectionEnvironment();
        //AIDTrafficDataReader.addLabelsToCSVFile();
        env.setUseLastPrediction(false);

        env.run(true, false, false, false, "COUNT");
        env.run(true, false, false, true, "COUNT+DENSITY");
        env.run(true, false, true, true, "COUNT+DENSITY+OCCUPANCY");
        env.run(true, true, true, true, "COUNT+DENSITY+OCCUPANCY+SPEED");
        env.run(true, true, false, true, "COUNT+DENSITY+SPEED");
        env.run(true, false, true, false, "COUNT+OCCUPANCY");
        env.run(true, true, true, false, "COUNT+OCCUPANCY+SPEED");
        env.run(true, true, false, false, "COUNT+SPEED");
        env.run(false, false, false, true, "DENSITY");
        env.run(false, false, true, true, "DENSITY+OCCUPANCY");
        env.run(false, true, true, true, "DENSITY+OCCUPANCY+SPEED");
        env.run(false, true, false, true, "DENSITY+SPEED");
        env.run(false, false, true, false, "OCCUPANCY");
        env.run(false, true, true, false, "OCCUPANCY+SPEED");
        env.run(false, true, false, false, "SPEED");
    }
}
