package tests.aid.laSVM;

import de.dfg.oc.otc.aid.AIDMonitoringZone;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import forecasting.forecastMethods.AbstractForecastMethod;
import forecasting.forecastMethods.ForecastMethod;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import static org.junit.Assert.assertEquals;

/**
 * Created by weiti on 03.04.2015.
 */
public class LASVMTest {
    private Map<String, Object> parameter1;

    @BeforeClass
    public static void startLASVM() {
        DefaultParams.AID_ACTIVE = true;
        DefaultParams.AID_DEFAULT_ALGORITHM = "SVM";
    }

    @Before
    public void initLASVM() {
        for (OTCNode node : OTCManager.getInstance().getNetwork().getNodes()) {
            for (AbstractAIDAlgorithm algorithm : node.getAIDComponent().getAlgorithms()) {
                if (algorithm.getName().equals("SVM")) {
                    algorithm.setParameters((algorithm).getParameters());
                }
            }
        }
    }

    @Test
    public void updateCheckArchive() {
        OTCManager manager = OTCManager.getInstance();

        Observable obs = new Observable();
        DetectorDataValue object = new DetectorDataValue(0, new float[]{1, 1, 1, 1, 1, 1});

        // Action
        for (OTCNode node : manager.getNetwork().getNodes()) {
            for (AIDMonitoringZone zone : node.getAIDComponent().getMonitoringZones()) {
                zone.getAIDAlgorithm().update(obs, object);
            }
        }
    }

    @Test
    public void updateCheck() {
        OTCManager manager = OTCManager.getInstance();

        Observable obs = new Observable();
        DetectorDataValue object = new DetectorDataValue(0, new float[]{1, 1, 1, 1, 1, 1});

        // Action
        for (OTCNode node : manager.getNetwork().getNodes()) {
            for (AIDMonitoringZone zone : node.getAIDComponent().getMonitoringZones()) {
                zone.getAIDAlgorithm().update(obs, object);
            }
        }
    }

    @Test
    public void setParameter() {
        parameter1 = new HashMap<>();
        parameter1.put("C", 10.0);
        parameter1.put("gamma", 0.1);
        parameter1.put("timeStepSize", (float) 300);
        parameter1.put("timeStepBack", 10);
        parameter1.put("timeStepForward", 2);
        parameter1.put("onlyIncomingStreets", true);
        parameter1.put("placeholderValues", new float[]{0, 0, 0, 0, 0, 0, 0});
        parameter1.put("forecastMethod", ForecastMethod.CROSTON);
        parameter1.put("predictorValues", new int[]{3});
        parameter1.put("onlineLearning", true);
        parameter1.put("reportCongestionFile", "incidentArchive.csv");

        for (OTCNode node : OTCManager.getInstance().getNetwork().getNodes()) {
            for (AbstractAIDAlgorithm algorithm : node.getAIDComponent().getAlgorithms()) {
                if (algorithm.getName().equalsIgnoreCase("SVM"))
                    algorithm.setParameters(parameter1);
            }
        }

        for (OTCNode node : OTCManager.getInstance().getNetwork().getNodes()) {
            for (AbstractAIDAlgorithm algorithm : node.getAIDComponent().getAlgorithms()) {
                if (algorithm.getName().equalsIgnoreCase("SVM")) {
                    assertEquals(algorithm.getParameters(), parameter1);
                }
            }
        }
    }
}
