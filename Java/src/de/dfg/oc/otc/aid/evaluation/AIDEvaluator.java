package de.dfg.oc.otc.aid.evaluation;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.Incident.EvaluationStatus;
import de.dfg.oc.otc.aid.Incident.IncidentType;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManager;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Offers functions to evaluate implemented AID algorithms.
 */
public class AIDEvaluator {
    /**
     * Logger for the evaluation results.
     */
    private static Logger log;
    /**
     * List of raised incident alarms.
     */
    private final List<Incident> incidentAlarms;
    /**
     * List of evaluated incident alarms.
     */
    private final List<Incident> evaluatedIncidents;
    /**
     * Map with evaluated incident as key and list of corresponding detected
     * incidents as value.
     */
    private final Map<Incident, List<Incident>> alarmToIncidentsMap;
    /**
     * Time interval which is used to determine if two incidents correspond
     * to each other.
     */
    private final int timeSensitivity;
    /**
     * Count for the number of algorithm applications.
     */
    private int algorithmApplicationsCount;

    public AIDEvaluator() {
        this.incidentAlarms = new ArrayList<>();
        this.evaluatedIncidents = new ArrayList<>();
        this.alarmToIncidentsMap = new HashMap<>();
        this.timeSensitivity = DefaultParams.AID_EVALUATION_TIME_SENSITIVITY;

        this.algorithmApplicationsCount = 0;
    }

    /**
     * Calculates the false alarm count (FAC).
     */
    public int getFalseAlarmCount() {
        int FAC = 0;

        findCorrespondingIncidents();

        for (Incident detectedIncident : this.incidentAlarms) {
            if (!correspondsToEvaluatedIncident(detectedIncident)) {
                FAC++;
            }
        }

        return FAC;
    }

    /**
     * Calculates the false alarm rate (FAR) according to algorithm
     * applications in percent.
     * <p>
     * {@code FAR = (No. of false Alarms / Total No. of algorithm applications)}
     *
     * @return False alarm rate (FAR) according to algorithm applications
     */
    public float getFalseAlarmRateApplications() {
        if (this.algorithmApplicationsCount > 0) {
            return getFalseAlarmCount() / this.algorithmApplicationsCount * 100;
        }
        return 0;
    }

    /**
     * Calculates the false alarm rate (FAR) according to the total number of
     * detected incidents in percent.
     * <p>
     * FAR = No. of false Alarms / Total No. of incident-free cases
     *
     * @return False alarm rate FAR=FP/(TN+FP)
     */
    public float getFalseAlarmRateIncidents() {
        float DIC = getDetectedIncidentsCount();

        if (DIC > 0) {
            return getFalseAlarmCount() / DIC * 100;
        }
        return 0;
    }

    /**
     * Returns the number of actual incidents.
     */
    public int getTruePositiveAlarms() {
        int count = 0;

        for (Incident incident : this.evaluatedIncidents) {
            if (incident.getEvaluationStatus() == EvaluationStatus.TRUE_POSITIVE) {
                count++;
            }
        }

        return count;
    }

    /**
     * Calculates the detection rate (DR) in percent.
     * DR = number of correctly detected incidents / number of all incidents x 100.
     *
     * @return Detection rate DR=TP/(TP+FN)
     */
    public float getDetectionRate() {
        findCorrespondingIncidents();

        if (!this.alarmToIncidentsMap.isEmpty()) {
            return (getTruePositiveAlarms() / this.alarmToIncidentsMap.size()) * 100;
        }
        return 100;
    }

    /**
     * Calculates the average time to detection (in seconds) of an incident (ATTD).
     */
    public float getAverageTimeToDetect() {
        float totalDetectionTime = 0;

        findCorrespondingIncidents();

        for (Map.Entry<Incident, List<Incident>> incidentListEntry : this.alarmToIncidentsMap.entrySet()) {
            float incidentDetectionTime = 0;

            for (Incident detectedIncident : incidentListEntry.getValue()) {
                incidentDetectionTime += Math.abs(incidentListEntry.getKey().getStartTime() - detectedIncident.getReportTime());
            }

            // Calculate average for corresponding incidents
            totalDetectionTime += incidentDetectionTime / incidentListEntry.getValue().size();
        }

        // Calculate average over all detected incidents
        if (!this.alarmToIncidentsMap.isEmpty()) {
            return totalDetectionTime / this.alarmToIncidentsMap.size();
        }

        return 0;
    }

    /**
     * Check if there are one or more detected incidents which correspond to an
     * evaluated incident.
     */
    private void findCorrespondingIncidents() {
        this.alarmToIncidentsMap.clear();

        this.evaluatedIncidents.stream().filter(incident -> incident.getEvaluationStatus() == EvaluationStatus.TRUE_POSITIVE)
                .forEach(evaluatedIncident -> {
                    for (Incident detectedIncident : this.incidentAlarms) {
                        checkDetectedIncident(evaluatedIncident, detectedIncident);
                    }
                });
    }

    /**
     * Checks if two incidents are identical and adds them to the list of
     * corresponding incidents if this is the case.
     *
     * @param evaluatedIncident Incident which actually has occurred
     * @param detectedIncident  Incident which has been detected by the algorithm
     */
    private void checkDetectedIncident(Incident evaluatedIncident, Incident detectedIncident) {
        if (compareIncidents(evaluatedIncident, detectedIncident)) {
            // Create key for correct incident if it doesn't exist
            this.alarmToIncidentsMap.putIfAbsent(evaluatedIncident, new ArrayList<>());

            // Add corresponding detected incident to the list
            this.alarmToIncidentsMap.get(evaluatedIncident).add(detectedIncident);
        }
    }

    /**
     * Compares two incidents with each other. Returns true if both incidents
     * belong to the same junction and have almost identical start times.
     *
     * @param evaluatedIncident Incident which actually has occurred
     * @param detectedIncident  Incident which has been detected by the algorithm
     * @return {@code true} if incidents correspond with each other,
     * {@code false} otherwise
     */
    private boolean compareIncidents(Incident evaluatedIncident, Incident detectedIncident) {
        boolean junctionSame = evaluatedIncident.getJunctionID() == detectedIncident.getJunctionID();
        boolean sectionSame = evaluatedIncident.getSectionID() == detectedIncident.getSectionID();
        boolean upstreamDetectorSame = evaluatedIncident.getUpstreamDetectorPair().equals(detectedIncident.getUpstreamDetectorPair());
        boolean downstreamDetectorSame = evaluatedIncident.getUpstreamDetectorPair().equals(detectedIncident.getUpstreamDetectorPair());
        boolean startTimeSimilar = Math.abs(evaluatedIncident.getStartTime() - detectedIncident.getStartTime()) <= timeSensitivity;

        if (evaluatedIncident.getType() == IncidentType.JUNCTION_INCIDENT && detectedIncident.getType() == IncidentType.JUNCTION_INCIDENT) {
            return junctionSame && startTimeSimilar;
        } else if (evaluatedIncident.getType() == IncidentType.DETECTOR_INCIDENT
                && detectedIncident.getType() == IncidentType.DETECTOR_INCIDENT) {
            return junctionSame && sectionSame && upstreamDetectorSame && downstreamDetectorSame && startTimeSimilar;
        } else if (evaluatedIncident.getType() == IncidentType.SECTION_INCIDENT
                && detectedIncident.getType() == IncidentType.SECTION_INCIDENT) {
            return junctionSame && sectionSame && startTimeSimilar;
        }

        return false;
    }

    /**
     * Helper method which checks if an incidents corresponds to an evaluated
     * incident.
     *
     * @param incident which has been detected
     * @return {@code true} if it corresponds to evaluated incident,
     * {@code false} otherwise
     */
    private boolean correspondsToEvaluatedIncident(Incident incident) {
        for (List<Incident> incidents : this.alarmToIncidentsMap.values()) {
            if (incidents.contains(incident)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Increases the number of algorithm applications. Should be called every
     * time the algorithm is applied.
     */
    public void algorithmApplied() {
        this.algorithmApplicationsCount++;
    }

    public int getDetectedIncidentsCount() {
        return this.incidentAlarms.size();
    }

    /**
     * Notifies the evaluator about a newly detected incident.
     */
    public void addDetectedIncident(Incident incident) {
        this.incidentAlarms.add(incident);
    }

    public Iterable<Incident> getIncidentAlarms() {
        return this.incidentAlarms;
    }

    /**
     * Returns the number of evaluated incidents.
     */
    public int getEvaluatedIncidentsCount() {
        return this.evaluatedIncidents.size();
    }

    /**
     * Notifies the evaluator about a newly evaluated incident.
     */
    public void addEvaluatedIncident(Incident incident) {
        this.evaluatedIncidents.add(incident);
    }

    public void setLogger(Logger logger) {
        log = logger;
    }

    /**
     * Logs the current evaluation results.
     */
    public void logResults() {
        log.info("============");
        log.info("Current Time: " + OTCManager.getInstance().getTime() + " s");
        log.info("ATTD: " + getAverageTimeToDetect() + " s");
        log.info("DR: " + getDetectionRate() + "%");
        log.info("FAR (Algorithm Applications): " + getFalseAlarmRateApplications() + "%");
        log.info("FAR (Detected Incidents): " + getFalseAlarmRateIncidents() + "%");
        log.info("Actual Incidents Count: " + getTruePositiveAlarms());
        log.info("Detected Incidents Count: " + getDetectedIncidentsCount());
        log.info("FP: " + getFalseAlarmCount());
        log.info("FN: " + (getTruePositiveAlarms() - (getDetectedIncidentsCount() - getFalseAlarmCount())));
        log.info("Algorithm Applications: " + this.algorithmApplicationsCount);
    }
}
