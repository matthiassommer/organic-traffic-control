package de.dfg.oc.otc.aid.algorithms.svm;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.KernelSVM;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.classifier.LaSVMI;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel.GaussianKernel;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel.typed.DoubleGaussL2;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.type.TrainingSample;
import de.dfg.oc.otc.layer1.observer.ForecastAdapter;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import forecasting.DefaultForecastParameters;
import forecasting.forecastMethods.AbstractForecastMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tests.evaluation.aid.AIDTrafficDataReader;

import java.util.*;

/**
 * Created by Christoph Weiss on 03.03.2015.
 * Class for congestion prediction based on a SVM.
 * <p>
 * Size of one Timestep is internal used. Extern there can be other timestep size possible. All incoming data are
 * normalized to the timestepSize and compute the mean.
 * <p>
 * TimestepForward is the timespan, in which the prediction is made. Prediction is only made, if all data of current
 * timestep span is collected. So if timeStepForward == 1, the "Prediction is not really a prediction, but rather
 * a congestion detection. TimeStepForward == 2 means, the Prediction is from now + 2 * timeStepSize beginning.
 * <p>
 * There are two Constructors available: first constructor is the default with no transfer parameter. This is commonly
 * used by the OTC System. The second constructor get all parameter in the Parameterlist and
 * initialise the algorithm by using this parameter. On both applies: you can restart and reinitialize
 * the algorithm by using the "setParameters(Map<String, Object> parameters)" method. In there, the full state is
 * reset and the algorithm is initialise with the new parameter.
 * <p>
 * The featurevector length can be precomputed by ((timeStepsBack * #TimeSeriesValues) * #neighboredMonitoringZones
 * + #PredictorValue). The longer the size of the featurevector, the longer is computing time and more trainingdata
 * are needed for good results.
 * <p>
 * https://github.com/davidpicard/jkernelmachines/tree/master
 */
public class SVM extends AbstractAIDAlgorithm {
    /**
     * True, if standalone version is running.
     */
    private final boolean isStandalone;
    /**
     * Standard Detector Data Values which are used, if no DetectorValues are measured.
     */
    private final float[] standardDetectorDataValues = {0, 0, 45, 0, 0, 0, 0};
    /**
     * Configuration of the main parameters of the SVM.
     */
    @Nullable
    private Map<String, Object> parameters;
    /**
     * The kernel of the SVM.
     */
    private KernelSVM svm;
    /**
     * Archive for storing the Detector Data Values from the Detectors.
     */
    private DetectorDataStorage detectorDataStorage;
    /**
     * Congestion Detector for online learning. Determine, if the street is congested or not.
     */
    private CongestionClassificator simpleCongestionClassificator;
    private CongestionClassificator.Definition congestionDefinition;
    /**
     * Storage for raised Incidents.
     */
    private IncidentStorage incidentStorage;
    /**
     * The position in the DetectorDataValue.values used by the forecast Modules.
     */
    private int[] forecastCapabilities;
    /**
     * For each DetectorCapability exists one forecast module.
     */
    private ForecastAdapter[] forecastModules = {};
    private float timeStepSize;
    /**
     * How many time steps back are regarded by the forecasting.
     */
    private int timeStepsBackward;
    /**
     * Point in time (number of steps) to forecast.
     * For 0, the algorithm classifies the current timeslot.
     */
    private int timeStepsForward;
    /**
     * Contains the identifier for each DetectorCapability that is used to build the featurevector.
     */
    private int[] detectorCapabilities;
    /**
     * Enable or disable online learning.
     */
    private boolean onlineLearning;
    private boolean reportCongestion;

    /**
     * Initialize the SVM, the DetectorData Archive, the DecisionMaker and compute the regarded MonitoringZones.
     * Use the standard Parameters.
     */
    public SVM() {
        isStandalone = false;
        initialise(getParameters());
    }

    /**
     * Alternative Constructor for evaluation classes.
     */
    public SVM(Map<String, Object> parameters) {
        isStandalone = true;
        initialise(parameters);
    }

    /**
     * Initialize parameters, the svm and the kernel.
     *
     * @param parameter Parameter for running the LaSVM class
     */
    private void initialise(Map<String, Object> parameter) {
        setParameters(parameter);

        float freeFlowSpeed = this.standardDetectorDataValues[DetectorCapabilities.SPEED];
        if (!isStandalone) {
            freeFlowSpeed = determineMinimumFreeFlowSpeed();
        }
        simpleCongestionClassificator = new CongestionClassificator(congestionDefinition, freeFlowSpeed);

        detectorDataStorage = new DetectorDataStorage(this.timeStepsBackward + this.timeStepsForward, timeStepSize, standardDetectorDataValues, forecastCapabilities.length);
    }

    /**
     * Inherit of AbstractAlgorithm Interface.
     * React on new Data by the DetectorGroup or the CongestionDecisionMaker.
     *
     * @param obs Observable object
     * @param obj Sent object
     */
    @Override
    public void update(Observable obs, Object obj) {
        DetectorDataValue detectorValue = (DetectorDataValue) obj;

        boolean newValueInserted = saveDetectorDataAndForecastInArchive(detectorValue);
        if (newValueInserted) {
            // norm time in time step size
            float currentTimeStep = detectorValue.getTime() - detectorValue.getTime() % timeStepSize;
            float previousTimestep = currentTimeStep - timeStepSize;

            // current time classifies only the result. Learning time has to go timeStepsForward and an
            // additional step back, because then the values for the last timeslot are averaged.
            float learningTimeStep = previousTimestep - timeStepSize * timeStepsForward;

            float[] detectorData = detectorValue.getValues();
            for (int i = 0; i < forecastModules.length; i++) {
                int pos = forecastCapabilities[i];
                forecastModules[i].addValueForForecast(currentTimeStep, detectorData[pos]);
            }

            // if enough detector data saved -> svm can start
            if (learningTimeStep - timeStepsBackward * timeStepSize >= 0) {
                onlineLearning(learningTimeStep, previousTimestep);

                prepareAndRunAlgorithm(previousTimestep);
            }
        }
    }

    private void onlineLearning(float learningTimeStep, float previousTimestep) {
        if (detectorDataStorage.getSecondOldestValue() == null) {
            return;
        }

        boolean congested = simpleCongestionClassificator.analyze(detectorDataStorage.getSecondOldestValue());

        // evaluate the classified incidents
        if (reportCongestion) {
            incidentStorage.classifyIncident(previousTimestep, congested);
        }

        if (onlineLearning) {
            double[] featureVector = createFeatureVector(learningTimeStep);
            TrainingSample sample = new TrainingSample<>(featureVector, congested ? AIDTrafficDataReader.ClassLabels.CONGESTION.getLabel() : AIDTrafficDataReader.ClassLabels.NO_CONGESTION.getLabel());
            svm.train(sample);
        }
    }

    /**
     * Function request the prediction values from the Prediction Components and save it together in the detectorDataStorage
     *
     * @param detectorValue The DetectorValue to be saved
     * @return If an new time period ist starting and all sensor values of the last period are accumulated
     */
    private boolean saveDetectorDataAndForecastInArchive(DetectorDataValue detectorValue) {
        float[] forecasts = new float[this.forecastModules.length];
        for (int i = 0; i < this.forecastModules.length; i++) {
            forecasts[i] = this.forecastModules[i].getForecast(1);
        }

        return this.detectorDataStorage.addValue(detectorValue, forecasts);
    }

    private void initializeForecastModules(List<AbstractForecastMethod> forecastMethods) {
        if (forecastMethods != null && !forecastMethods.isEmpty()) {
            forecastModules = new ForecastAdapter[forecastCapabilities.length];
            for (int i = 0; i < forecastModules.length; i++) {
                forecastModules[i] = new ForecastAdapter(1);
            }
        }
    }

    /**
     * Determine minium free flow speed over the whole monitoring zone.
     * http://www.fhwa.dot.gov/publications/research/operations/tft/chap2.pdf
     * http://www.aboutcivil.org/free-flow-speed-ffs.html
     */
    private float determineMinimumFreeFlowSpeed() {
        float freeFlowSpeed = Float.MAX_VALUE;
        for (Section section : this.getMonitoringZone().getMonitoredLink().getMonitoredSections()) {
            if (freeFlowSpeed > section.getSpeedlimit()) {
                freeFlowSpeed = section.getSpeedlimit();
            }
        }
        this.standardDetectorDataValues[DetectorCapabilities.SPEED] = freeFlowSpeed;

        return freeFlowSpeed;
    }

    /**
     * Inherit from Super Class. Predict congestion by computed time series.
     *
     * @param time Timestamp when the DetectorDataValue is measured
     */
    @Override
    protected void prepareAndRunAlgorithm(float time) {
        algorithmApplied();

        double[] featureVector = createFeatureVector(time);
        if (svm.valueOf(featureVector) < 0 && this.reportCongestion) {
            // create new incident and throw it
            float timeOfIncident = time + timeStepsForward * timeStepSize;
            if (isStandalone) {
                saveIncident(timeOfIncident);
            } else {
                reportCongestion(timeOfIncident);
            }
        }
    }

    /**
     * Creates an Incident Object for each DetectorPair in the MonitoringZone and calls the super.reportIncident method.
     * Report an incident over all track section (sections between the DetectorGroups) in this monitoring zone
     *
     * @param time Timestamp when the DetectorDataValue is measured
     */
    private void reportCongestion(float time) {
        String previousDetectorPairID = null;

        for (AbstractDetectorGroup detectorPair : monitoringZone.getMonitoredDetectorPairs()) {
            String detectorPairID = detectorPair.getId();
            // Skip first detector since it has no predecessor
            if (previousDetectorPairID != null) {
                // detectorPairID = upstream
                // previousDetectorPairID = downstream
                String pairIdentifier = getPairIdentifier(detectorPairID, previousDetectorPairID);

                Incident incident = createIncident(time, pairIdentifier, false);
                reportIncident(incident);

                incidentStorage.storeIncident(incident);
            }
            previousDetectorPairID = detectorPairID;
        }
    }

    /**
     * Save incident in incidentStorage
     *
     * @param time of incident occurence
     */
    private void saveIncident(float time) {
        Incident incident = new Incident();
        incident.setStartTime(time);
        incidentStorage.storeIncident(incident);
    }

    /**
     * Create a time series feature vector specified by the timestamp and neighbord MonitoringZones. Used by the SVM
     * for prediction.
     *
     * @param time Timestamp where the timeseries starts
     * @return the featureVector
     */
    @NotNull
    private double[] createFeatureVector(float time) {
        List<Double> featureVectorList = new ArrayList<>();

        // Values from the own DetectorDataArchive
        float[] featureVectorOwn = this.detectorDataStorage.getFeatureVector(time, timeStepsBackward, detectorCapabilities);
        for (double feature : featureVectorOwn) {
            featureVectorList.add(feature);
        }

        double[] featureArray = new double[featureVectorList.size()];
        int i = 0;
        for (Double value : featureVectorList) {
            featureArray[i++] = value != null ? value : Double.NaN;
        }

        return featureArray;
    }

    @NotNull
    @Override
    public String getName() {
        return "SVM";
    }

    @Nullable
    /**
     * Important: all values have to be specified.
     * [0]:   C             (double) SVM Failure weight
     * [1]:   gamma         (double) Gauss Kernel parameter
     * [2]:   timeStepSize  (float)  time Step size in seconds
     * [3]:   timeStepBackward  (int) how many time Steps back are regarded
     * [4]:   timeStepForward   (int) for which time Step forward the prediction is made
     * [5]:   detectorCapabilities (int[])  which DetectorData are used to build the timeseries
     * [6]:   neighboredStreets (int) if neighbored MonitoringZones are regarded.
     * 0: no MonitoringZones,1: only incoming MonitoringZones,2: incoming and outgoing MonitoringZones
     * [7]:   standardDetectorDataValues        (float[]) Standardvalues are used, if no Traffic Data at a time is measured.
     * [8]:   forecastMethods   (List<ForecastMethodType>)   Type of the forecast methods, which are used
     * [9]:   forecastCapabilities    (int[]) Which types of forecastCapabilities are used.
     * [14]:  onlineLearning    (boolean) If the svm has to learn or not learn
     * [15]:  reportCongestion  (boolean) if the congestion is to reported or not.
     * [16]:  reportCongestionFilepath  (String) Where the congestions are saved.
     */
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> defaultParameter = new HashMap<>();
        defaultParameter.put("C", 1.0);
        defaultParameter.put("gamma", 0.1);
        defaultParameter.put("timeStepSize", 300);
        defaultParameter.put("timeStepsBackward", 8);
        defaultParameter.put("timeStepsForward", 1);
        defaultParameter.put("detectorCapabilities", new int[]{DetectorCapabilities.SPEED});
        defaultParameter.put("forecastMethods", DefaultForecastParameters.DEFAULT_FORECAST_METHODS);
        defaultParameter.put("forecastCapabilities", new int[]{});
        defaultParameter.put("onlineLearning", false);
        defaultParameter.put("reportCongestion", true);
        defaultParameter.put("reportCongestionFile", null);
        defaultParameter.put("congestionDefinition", CongestionClassificator.Definition.MnDOT);
        return defaultParameter;
    }

    public void setParameters(@Nullable Map<String, Object> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("SVM parameters are null");
        }

        this.parameters = parameters;

        GaussianKernel kernel = new DoubleGaussL2();
        kernel.setGamma((double) this.parameters.get("gamma"));
        svm = new LaSVMI<>(kernel);
        svm.setC((double) this.parameters.get("C"));

        this.timeStepSize = (float) this.parameters.get("timeStepSize");
        this.timeStepsBackward = (int) this.parameters.get("timeStepsBackward");
        this.timeStepsForward = (int) this.parameters.get("timeStepsForward");
        this.detectorCapabilities = (int[]) this.parameters.get("detectorCapabilities");

        this.forecastCapabilities = (int[]) this.parameters.get("forecastCapabilities");
        initializeForecastModules((List<AbstractForecastMethod>) this.parameters.get("forecastMethods"));

        this.onlineLearning = (boolean) this.parameters.get("onlineLearning");

        this.reportCongestion = (boolean) this.parameters.get("reportCongestion");
        this.incidentStorage = new IncidentStorage((String) this.parameters.get("reportCongestionFile"));
        this.congestionDefinition = (CongestionClassificator.Definition) this.parameters.get("congestionDefinition");
    }

    @Override
    public int getRequiredDetectorPairCount() {
        return 1;
    }

    @Override
    public boolean isStateMappedToIncident(int state) {
        return false;
    }

    public void setReportCongestion(boolean activate) {
        this.reportCongestion = activate;
    }

    /**
     * Train the SVM with a batch of training data.
     * PCS is executed before the training.
     */
    public void offlineLearning(List<DetectorDataValue> detectorValues) {
        List<TrainingSample<double[]>> samples = new ArrayList<>();

        for (DetectorDataValue value : detectorValues) {
            int congested = value.isCongested() ? AIDTrafficDataReader.ClassLabels.CONGESTION.getLabel() : AIDTrafficDataReader.ClassLabels.NO_CONGESTION.getLabel();
            if (!value.isAnnotatedData()) {
                congested = simpleCongestionClassificator.analyze(value) ? AIDTrafficDataReader.ClassLabels.CONGESTION.getLabel() : AIDTrafficDataReader.ClassLabels.NO_CONGESTION.getLabel();
            }

            this.detectorDataStorage.addValue(value, new float[]{});
            double[] featureVector = createFeatureVector(value.getTime());
            samples.add(new TrainingSample<>(featureVector, congested));
        }

        svm.train(samples);

        this.detectorDataStorage.clear();
    }

    public IncidentStorage getIncidentStorage() {
        return incidentStorage;
    }

    public void resetIncidentStorage() {
        this.incidentStorage = new IncidentStorage((String) this.parameters.get("reportCongestionFile"));
    }
}
