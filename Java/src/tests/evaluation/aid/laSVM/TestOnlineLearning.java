package tests.evaluation.aid.laSVM;

import de.dfg.oc.otc.aid.algorithms.svm.IncidentStorage;
import de.dfg.oc.otc.tools.FileUtilities;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

/**
 * This Test evaluates the Performance of the algorithm's prediction by using the online learning component.
 * Online Learning takes long: appr. 7 sec per trained value!
 */
public class TestOnlineLearning {
    @Test
    public void runS637Online() {
        run("S637", "2448", true);
    }

    @Test
    public void runS86Online() {
        run("S86", "366", true);
    }

    @Test
    public void runS637() {
        run("S637", "2448", false);
    }

    @Test
    public void runS86() {
        run("S86", "366", false);
    }

    private void run(String station, String detector, boolean onlineLearning) {
        String fileNameAppendix = "Result.csv";
        if (onlineLearning) {
            fileNameAppendix = "onlineLearningResult.csv";
        }

        String filename = EvaluationUtilities.EVALUATION_FOLDER + "OnlineLearning\\" + station + fileNameAppendix;
        FileUtilities.createNewFile(filename);

        Map<String, Object> parameter = EvaluationUtilities.getParameter();
        parameter.replace("onlineLearning", onlineLearning);

        EvaluationUtilities.setEndDateTesting(1);

        IncidentStorage storage = EvaluationUtilities.runEvaluation(station, detector, parameter);

        float[] result = EvaluationUtilities.getMetrics(storage);
        EvaluationUtilities.appendLineToFile(Arrays.toString(result), filename);
    }
}


