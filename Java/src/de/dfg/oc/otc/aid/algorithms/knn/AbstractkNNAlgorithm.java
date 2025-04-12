package de.dfg.oc.otc.aid.algorithms.knn;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.layer1.observer.ForecastAdapter;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author alexandermartel Base class for both the kNN and fuzzy kNN algorithm
 */
public abstract class AbstractkNNAlgorithm extends AbstractAIDAlgorithm {
    /**
     * The number of neighbors.
     */
    final int k = 4;
    final EuclideanDistance euclidianDistance;
    /**
     * Determines whether Count of {@link DetectorDataValue#values} is used
     */
    private static final boolean useCount = false;
    /**
     * Determines whether Speed of {@link DetectorDataValue#values} is used
     */
    private static final boolean useSpeed = true;
    /**
     * Determines whether Occupied Time Percentage of {@link DetectorDataValue#values} is used
     */
    private static final boolean useOccupancy = true;
    /**
     * Determines whether Density of {@link DetectorDataValue#values} is used
     */
    private static final boolean useDensity = true;
    /**
     * Contains the classified (incident or not) trainingSets for each
     * {@link AbstractDetectorGroup} of the
     * {@link AbstractAIDAlgorithm#getMonitoringZone()}, with
     * {@link AbstractDetectorGroup#getId()} being the Key.
     */
    private final Map<String, List<KNNDetectorDataValue>> trainingSets;
    /**
     * An absolute path to the folder containing the training data.
     */
    private final String pathToTrainingData = ".\\AIMSUN_MODELL\\MA_Hamburg_Billstedt_6";
    private final Map<String, List<Float>> countValues;
    private final Map<String, List<Float>> speedValues;
    private final Map<String, List<Float>> occupancyValues;
    private final Map<String, List<Float>> densityValues;
    /**
     * Contains an array of {@link ForecastAdapter} (one for each value used in the featureArray) for each
     * {@link AbstractDetectorGroup} of the
     * {@link AbstractAIDAlgorithm#getMonitoringZone()}, with
     * {@link AbstractDetectorGroup#getId()} being the Key.
     */
    private Map<String, ForecastAdapter[]> forecastAdapters;
    /**
     * Determines whether the kNN algorithm should forecast incidents (by working with forecast value).
     */
    private boolean forecast = false;
    private int forecastInterval = 60;

    /**
     * Constructor. Retrieves the k parameter from settings.
     */
    AbstractkNNAlgorithm() {
        this.simulationStepSize = 300;

        trainingSets = new HashMap<>();
        euclidianDistance = new EuclideanDistance();

        countValues = new HashMap<>();
        densityValues = new HashMap<>();
        occupancyValues = new HashMap<>();
        speedValues = new HashMap<>();

        if (forecast) {
            forecastAdapters = new HashMap<>();
        }
    }

    /**
     * Converts an array of float values to an array of double values.
     * EuclidianDistance requires double values and DetectorDataValue returns
     * float values.
     *
     * @param input array containing float values
     * @return array containing double values
     */
    double[] convertFloatsToDoubles(float[] input) {
        if (input == null) {
            return null;
        }

        int length = 7;
        double[] output = new double[length];
        for (int i = 0; i < length; i++) {
            output[i] = input[i];
        }
        return output;
    }

    /**
     * Converts the feature array {@link DetectorDataValue#values} to doubles and checks which fields should be
     * used via {@link AbstractkNNAlgorithm#useCount},{@link AbstractkNNAlgorithm#useDensity} and so on.
     * Unused fields will have value 0.
     */
    static double[] getFeatureArrayOfDetectorData(DetectorDataValue dtv) {
        double count = 0, speed, otp, density;
        if (useCount) {
            count = dtv.getValues()[DetectorCapabilities.COUNT];
        }
        if (useSpeed) {
            speed = dtv.getValues()[DetectorCapabilities.SPEED];
        }
        if (useOccupancy) {
            otp = dtv.getValues()[DetectorCapabilities.OCCUPANCY];
        }
        if (useDensity) {
            density = dtv.getValues()[DetectorCapabilities.DENSITY];
        }

        return new double[]{count, 0, speed, otp, 0, density, 0};
    }

    @Override
    public void finalizeInitialization() {
        //loading the trainingData is only possible if the monitoring zone has been set
        loadTrainingData(pathToTrainingData);
    }

    /**
     * Loads the training data in the given directory.
     * Each detector has its own training file.
     * The filename must be of the following pattern: "<MonitoringZoneID>_<DetectorID>.csv"
     *
     * @param folderPath Absolute folderPath to folder containing training data
     */
    private void loadTrainingData(String folderPath) {
        if (folderPath.trim().isEmpty()) {
            System.out.println("kNN-Algorithm is missing training data!");
        }

        File trainingDataDir = new File(folderPath);
        if (!trainingDataDir.isDirectory()) {
            System.out.println("kNN-Algorithm parameter 'trainingDataPath' must be a directory");
        }

        //get all csv files in dir
        File[] filesInTrainingDir = trainingDataDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (filesInTrainingDir == null || filesInTrainingDir.length < 1) {
            System.out.println("kNN-Algorithm is missing training data!");
            return;
        }

        for (File file : filesInTrainingDir) {
            //get filename without extend
            String fileName = file.getName().toLowerCase();
            int pos = fileName.lastIndexOf(".");
            if (pos > 0) {
                fileName = fileName.substring(0, pos);
            }
            String[] splitName = fileName.split("_");
            if (splitName.length < 1) {
                continue;
            }

            String monitorZoneId = splitName[0];
            String detectorId = splitName[1];

            //check if the trainingData is for our monitoring zone
            if (!monitorZoneId.equals(String.valueOf(getMonitoringZone().getId()))) {
                continue;
            }
            if (!getMonitoringZone().getMonitoredDetectorPairs().stream().anyMatch(x -> String.valueOf(x.getId()).equals(detectorId))) {
                continue;
            }
            List<KNNDetectorDataValue> trainingData = null;

            try {
                trainingData = KNNDetectorDataValueImporter.readTrainingData(new FileInputStream(file));
            } catch (IOException ex) {
                System.out.print(ex.getMessage());
            }

            if (trainingData != null) {
                trainingSets.put(detectorId, trainingData);
            }
        }
    }

    /**
     * Adds a classified trainingSet for a specified AbstractDetectorGroup
     *
     * @param detectorGroupIdentifier The id of the AbstractDetectorGroup
     * @param trainingSet             A List of {@link KNNDetectorDataValue}
     */
    public void addTrainingSet(String detectorGroupIdentifier, List<KNNDetectorDataValue> trainingSet) {
        this.trainingSets.putIfAbsent(detectorGroupIdentifier, trainingSet);
    }

    /**
     * Gets a trainingSet for an AbstractDetectorGroup
     *
     * @param groupIdentifier The id of the AbstractDetectorGroup
     * @return The classified trainingSet
     */
    List<KNNDetectorDataValue> getTrainingListForDetectorGroupIdentifier(String groupIdentifier) {
        return trainingSets.get(groupIdentifier);
    }

    /**
     * Gets the k-nearest neighbors of a {@link DetectorDataValue} instance.
     * Distance measure is EuclidianDistance.
     *
     * @param detectorGroupIdentifier The id of the AbstractDetectorGroup, needed to retrieve the
     *                                corresponding trainingSet
     * @param instance                The DetectorDataValue instance
     * @param k                       The number of neighbors to retrieve
     * @return The list of k-nearest {@link KNNNeighbor}
     */
    List<KNNNeighbor> getKNearestNeighbors(String detectorGroupIdentifier, DetectorDataValue instance, int k) {
        List<KNNDetectorDataValue> trainingSet = trainingSets.get(detectorGroupIdentifier);
        if (trainingSet == null || trainingSet.size() < k) {
            return null;
        }

        return getNeighborsOfTrainingSet(trainingSet, instance, k);
    }

    /**
     * Gets the k-nearest neighbors of a {@link DetectorDataValue} instance.
     * Distance measure is EuclidianDistance.
     *
     * @param trainingSet The trainingSet to retrieve neighbors from
     * @param instance    The DetectorDataValue instance
     * @param k           The number of neighbors to retrieve
     * @return The list of k-nearest {@link KNNNeighbor}
     */
    List<KNNNeighbor> getNeighborsOfTrainingSet(List<KNNDetectorDataValue> trainingSet, DetectorDataValue instance, int k) {
        float biggestDistance = -1;
        int idxBiggestDistance = -1;

        List<KNNNeighbor> neighbors = new ArrayList<>();
        Map<KNNDetectorDataValue, Float> computedDistances = new HashMap<>();

        for (int i = 0; i < trainingSet.size(); i++) {
            float distance = (float) euclidianDistance.compute(getFeatureArrayOfDetectorData(instance), trainingSet.get(i).getFeatureArray());

            // init: fill list until its size is k
            if (i < k) {
                computedDistances.put(trainingSet.get(i), distance);
                if (distance > biggestDistance) {
                    biggestDistance = distance;
                    idxBiggestDistance = i;
                }
            } else if (distance < biggestDistance) {
                // found an instance with a
                // smaller distance than the one in the
                // neighbor set with the highest distance
                computedDistances.remove(trainingSet.get(idxBiggestDistance));
                computedDistances.put(trainingSet.get(i), distance);

                biggestDistance = -1;

                for (Entry<KNNDetectorDataValue, Float> entry : computedDistances.entrySet()) {
                    // after altering the list, check for the new neighbor with
                    // the highest distance
                    float entryDist = entry.getValue();
                    if (entryDist > biggestDistance) {
                        biggestDistance = entryDist;
                        idxBiggestDistance = trainingSet.indexOf(entry.getKey());
                    }
                }
            }
        }

        // create KNNNeighbors, which also contain the distance (used when
        // there's a tie using kNN's majority vote)
        for (Entry<KNNDetectorDataValue, Float> entry : computedDistances.entrySet()) {
            KNNNeighbor neighbor = new KNNNeighbor(entry.getKey(), entry.getValue());
            neighbors.add(neighbor);
        }

        return neighbors;
    }

    /**
     * If an incident is found, check if the incident already exists and is
     * labeled as such. If yes, set the state to INCIDENT_CONTINUING Set to
     * INCIDENT_OCCURED otherwise and create the {@link Incident} instance.
     *
     * @param pairIdentifier String combining the ids of two detector pairs
     *                       (upstreamID;downstreamID)
     */
    void incidentOccurred(String pairIdentifier, float time) {
        /*
      Use a simple congestion report without the DetectorGroup as Identifier
      or with Detectorgroup.
     */
        boolean reportSimpleCongestion = false;
        if (reportSimpleCongestion) {
            Incident incident = new Incident();
            incident.setStartTime(time);
            incident.setConfirmed(false);
            setChanged();
            notifyObservers(incident);
        } else {
            if (algorithmStates.get(pairIdentifier) == KNNState.INCIDENT_CONFIRMED.ordinal()) {
                algorithmStates.put(pairIdentifier, KNNState.INCIDENT_CONTINUING.ordinal());
            } else {
                if (algorithmStates.get(pairIdentifier) == KNNState.INCIDENT_FREE.ordinal()) {
                    algorithmStates.put(pairIdentifier, KNNState.INCIDENT_CONFIRMED.ordinal());
                    // if algorithm is in use by weighted combination, let it handle the
                    // incident reporting
                    // we wont need the incident object in this case
                    if (!isUsedInCombination()) {
                        if (this.forecast) {
                            time += this.forecastInterval;
                        }

                        Incident incident = createIncident(time, pairIdentifier, true);
                        tentativeIncidents.put(pairIdentifier, incident);
                        DisturbanceManager.getInstance().confirmDisturbance(incident, getNode());

                        notifyObservers(incident);
                    }
                }
            }
        }
    }

    protected void incidentFree(float time, String pairIdentifier) {
        algorithmStates.put(pairIdentifier, KNNState.INCIDENT_FREE.ordinal());
        // if algorithm is in use by weighted combination, let it handle the incident reporting
        if (!isUsedInCombination()) {
            super.incidentFree(time, pairIdentifier);
        }
    }

    /**
     * {@inheritDoc} Calculates the pairIdentifier of the DetectorGroup in
     * combination with the preceding DetectorGroup in the MonitorZone via
     * {@link AbstractkNNAlgorithm#getPairIdentifier(AbstractDetectorGroup)} and
     * calls
     * {@link AbstractkNNAlgorithm#executekNNAlgorithm(String, String, DetectorDataValue)}
     */
    @Override
    protected void newDetectorData(AbstractDetectorGroup detectorGroup, DetectorDataValue detectorValue) {
        //no trainingData for detectorPair
        if (!this.trainingSets.containsKey(detectorGroup.getId())) {
            return;
        }

        float time = detectorValue.getTime();

        if (time > warmupTime) {
            if (forecast) {
                DetectorDataValue forecastDTV = getForecastArray(detectorGroup, detectorValue);
                if (forecastDTV != null) {
                    detectorValue = forecastDTV;
                } else return;
            }

            fillDetectorDataQueues(detectorGroup.getId(), detectorValue);

            if (detectorValue.getTime() % this.executionInterval + this.simulationStepSize >= this.executionInterval) {
                String pairIdentifier = getPairIdentifier(detectorGroup);

                DetectorDataValue avgDetectorDataValue = new DetectorDataValue(detectorValue.getTime(), getCurrentDetectorValuesAverage(detectorGroup.getId()));
                reset();
                executekNNAlgorithm(pairIdentifier, detectorGroup.getId(), avgDetectorDataValue);
            }
        }
    }

    private void reset() {
        occupancyValues.clear();
        speedValues.clear();
        densityValues.clear();
        countValues.clear();
    }

    private void fillDetectorDataQueues(String id, DetectorDataValue dtv) {
        if (useCount) {
            countValues.putIfAbsent(id, new ArrayList<>());
            countValues.get(id).add(dtv.getValues()[DetectorCapabilities.COUNT]);
        }
        if (useDensity) {
            densityValues.putIfAbsent(id, new ArrayList<>());
            densityValues.get(id).add(dtv.getValues()[DetectorCapabilities.DENSITY]);
        }
        if (useOccupancy) {
            occupancyValues.putIfAbsent(id, new ArrayList<>());
            occupancyValues.get(id).add(dtv.getValues()[DetectorCapabilities.OCCUPANCY]);
        }
        if (useSpeed) {
            speedValues.putIfAbsent(id, new ArrayList<>());
            speedValues.get(id).add(dtv.getValues()[DetectorCapabilities.SPEED]);
        }
    }

    private float[] getCurrentDetectorValuesAverage(String id) {
        float count = 0, density, otp, speed;

        if (useCount) {
            count = getCurrentDetectorValueAverage(id, DetectorCapabilities.COUNT);
        }
        if (useDensity) {
            density = getCurrentDetectorValueAverage(id, DetectorCapabilities.DENSITY);
        }
        if (useOccupancy) {
            otp = getCurrentDetectorValueAverage(id, DetectorCapabilities.OCCUPANCY);
        }
        if (useSpeed) {
            speed = getCurrentDetectorValueAverage(id, DetectorCapabilities.SPEED);
        }

        return new float[]{count, 0, speed, otp, 0, density, 0};
    }

    private float getCurrentDetectorValueAverage(String id, int feature) {
        List<Float> values = null;

        switch (feature) {
            case DetectorCapabilities.COUNT:
                values = countValues.get(id);
                break;
            case DetectorCapabilities.DENSITY:
                values = densityValues.get(id);
                break;
            case DetectorCapabilities.OCCUPANCY:
                values = occupancyValues.get(id);
                break;
            case DetectorCapabilities.SPEED:
                values = speedValues.get(id);
                break;
        }

        if (values != null && !values.isEmpty()) {
            float sum = 0;
            for (float value : values) {
                sum += value;
            }
            return sum / values.size();
        }

        return 0;
    }

    /**
     * Creates ForecastAdapters for the {@link AbstractDetectorGroup} if needed, adds the featureArray of the current DetectorDataValue for forecast and tries to get a forecast.
     * Used fields in featureArray are considered via {@link AbstractkNNAlgorithm#useCount}, {@link AbstractkNNAlgorithm#useDensity} etc.
     *
     * @return DetectorDataValue with forecast values (or null if forecast is not available)
     */
    private DetectorDataValue getForecastArray(AbstractDetectorGroup detectorGroup, DetectorDataValue detectorValue) {
        if (forecastAdapters.containsKey(detectorGroup.getId())) {
            ForecastAdapter[] groupAdapters = forecastAdapters.get(detectorGroup.getId());
            boolean validForecast;
            float[] forecastFeatureArray = new float[7];

            if (useCount) {
                validForecast = getForecastForFeature(groupAdapters, detectorValue, forecastFeatureArray, 0);
            }
            if (useSpeed) {
                validForecast = getForecastForFeature(groupAdapters, detectorValue, forecastFeatureArray, 2);
            }
            if (useOccupancy) {
                validForecast = getForecastForFeature(groupAdapters, detectorValue, forecastFeatureArray, 3);
            }
            if (useDensity) {
                validForecast = getForecastForFeature(groupAdapters, detectorValue, forecastFeatureArray, 5);
            }

            if (validForecast) {
                return new DetectorDataValue(detectorValue.getTime(), forecastFeatureArray);
            }
        } else {
            forecastAdapters.put(detectorGroup.getId(), new ForecastAdapter[7]);
            ForecastAdapter[] groupAdapters = forecastAdapters.get(detectorGroup.getId());

            if (useCount) {
                groupAdapters[0] = createNewForecastAdapter(detectorValue, 0);
            }
            if (useSpeed) {
                groupAdapters[2] = createNewForecastAdapter(detectorValue, 2);
            }
            if (useOccupancy) {
                groupAdapters[3] = createNewForecastAdapter(detectorValue, 3);
            }
            if (useDensity) {
                groupAdapters[5] = createNewForecastAdapter(detectorValue, 5);
            }
        }
        return null;
    }

    private ForecastAdapter createNewForecastAdapter(DetectorDataValue detectorValue, int i) {
        ForecastAdapter forecastAdapter = new ForecastAdapter(this.forecastInterval);
        forecastAdapter.addValueForForecast(detectorValue.getTime(), detectorValue.getValues()[i]);
        return forecastAdapter;
    }

    private boolean getForecastForFeature(ForecastAdapter[] groupAdapters, DetectorDataValue detectorValue, float[] forecastFeatureArray, int i) {
        ForecastAdapter forecastAdapter = groupAdapters[i];
        forecastAdapter.addValueForForecast(detectorValue.getTime(), detectorValue.getValues()[i]);
        float forecast = forecastAdapter.getForecast(this.forecastInterval);
        if (!Float.isNaN(forecast)) {
            forecastFeatureArray[i] = forecast;
            return true;
        }
        return false;
    }

    /**
     * Calculates the pairIdentifier of an AbstractDetectorGroup in combination
     * with the preceding AbstractDetectorGroup in the MonitorZone
     *
     * @param detectorGroup The {@link AbstractDetectorGroup} we need the pairIdentfier
     *                      for
     * @return The pairIdentifier
     */
    private String getPairIdentifier(AbstractDetectorGroup detectorGroup) {
        int indexOfPredecessor = monitoringZone.getMonitoredDetectorPairs().indexOf(detectorGroup) - 1;
        if (indexOfPredecessor > -1) {
            String idOfPredecessor = monitoringZone.getMonitoredDetectorPairs().get(indexOfPredecessor).getId();
            return getPairIdentifier(detectorGroup.getId(), idOfPredecessor);
        }
        return null;

    }

    /**
     * Actual execution of the (fuzzy) kNN algorithm
     *
     * @param pairIdentifier    The pairIdentifier, needed for reporting incidents
     * @param groupIdentifier   The groupIdentifier, the {@link AbstractDetectorGroup#getId()}
     *                          , to get the trainingSet for the detector
     * @param detectorDataValue The new data of the AbstractDetectorGroup
     */
    protected abstract void executekNNAlgorithm(String pairIdentifier, String groupIdentifier, DetectorDataValue detectorDataValue);

    /**
     * kNN is not using this method.
     */
    @Override
    protected void prepareAndRunAlgorithm(float time) {
    }

    @Override
    public int getRequiredDetectorPairCount() {
        return 1;
    }

    @Override
    public boolean isStateMappedToIncident(int state) {
        return state == KNNState.INCIDENT_CONFIRMED.ordinal() || state == KNNState.INCIDENT_CONTINUING.ordinal();
    }

    public void setForecast(boolean forecast) {
        this.forecast = forecast;
    }

    public void setForecastInterval(int forecastInterval) {
        this.forecastInterval = forecastInterval;
    }

    /**
     * States used by the both the kNN and Fuzzy-kNN algorithm.
     */
    enum KNNState {
        /**
         * The algorithm has found no incident.
         */
        INCIDENT_FREE,
        /**
         * The algorithm has confirmed an incident.
         */
        INCIDENT_CONFIRMED,
        /**
         * The algorithm has confirmed an incident.
         */
        INCIDENT_CONTINUING
    }
}
