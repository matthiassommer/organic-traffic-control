package tests.evaluation.aid.laSVM;

import de.dfg.oc.otc.aid.algorithms.svm.IncidentStorage;
import de.dfg.oc.otc.tools.FileUtilities;
import org.junit.Test;

import java.util.Map;

/**
 * Parameter study for the time step back parameter of the SVM.
 *
 * @author Matthias Sommer.
 */
public class TestTimeStepsBackward {
    @Test
    public void runS637() {
        run("S637", "2448");
    }

    @Test
    public void runS86() {
        run("S86", "366");
    }

    public void run(String station, String detector) {
        String filename = EvaluationUtilities.EVALUATION_FOLDER + "TimestepsBackward\\" + station + "Results.csv";
        FileUtilities.createNewFile(filename);

        Map<String, Object> parameter = EvaluationUtilities.getParameter();

        for (int timeStepsBackward = 1; timeStepsBackward <= 10; timeStepsBackward++) {
            parameter.replace("timeStepsBackward", timeStepsBackward);

            IncidentStorage storage = EvaluationUtilities.runEvaluation(station, detector, parameter);

            float[] result = EvaluationUtilities.getMetrics(storage);
            EvaluationUtilities.appendLineToFile(result, filename);
        }
    }
}

