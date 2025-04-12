package tests.evaluation.aid.laSVM;

import de.dfg.oc.otc.aid.algorithms.svm.IncidentStorage;
import de.dfg.oc.otc.tools.FileUtilities;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

/**
 * Parameter study for hyperparameter C and Gamma of the SVM.
 *
 * @author Matthias Sommer.
 */
public class TestParameterCundGamma {
    private final int maxC = 10;
    private final float maxGamma = 0.9f;

    @Test
    public void runS637() {
        run("S637", "2448");
    }

    @Test
    public void runS86() {
        run("S86", "366");
    }

    private void run(String station, String detector) {
        String filename = EvaluationUtilities.EVALUATION_FOLDER + "C und gamma\\" + station + "Result.csv";
        FileUtilities.createNewFile(filename);

        Map<String, Object> parameter = EvaluationUtilities.getParameter();

        for (int gamma = 1; gamma <= maxGamma * 10; gamma++) {
            for (double c = 1; c <= maxC; c += 1) {
                parameter.replace("C", c);
                parameter.replace("gamma", gamma * 0.1);

                IncidentStorage storage = EvaluationUtilities.runEvaluation(station, detector, parameter);

                float[] result = EvaluationUtilities.getMetrics(storage);
                EvaluationUtilities.appendLineToFile(c + " " + gamma * 0.1 + " " + Arrays.toString(result), filename);
            }
            EvaluationUtilities.appendLineToFile(" ", filename);
        }
    }
}
