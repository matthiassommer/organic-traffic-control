package de.dfg.oc.otc.aid;

import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents an incident.
 */
public class Incident {
    /**
     * Incident description template for type JUNCTION_INCIDENT.
     */
    private static final Format junctionTemplate =
            new MessageFormat("Junction Incident\nID: {0,number,#}\nJunction-ID: {1}\nReport Time: {2}\nStart of occurrence: {3}\nEnd of occurrence: {4}\nDuration: {5}");
    /**
     * Incident description template for type DETECTOR_INCIDENT.
     */
    private static final Format detectorTemplate =
            new MessageFormat("Detector Incident\nID: {0}\nSection-ID: {1,number,#}\nDetector Stations: {2},{3}\nReport Time: {4}\nStart of occurrence: {5}\nEnd of occurrence: {6}\nDuration: {7}");
    /**
     * Incident description template for type SECTION_INCIDENT.
     */
    private static final Format sectionTemplate =
            new MessageFormat("Section Incident\nID: {0,number,#}\nSection-ID: {1}\nReport Time: {2}\nStart of occurrence: {3}\nEnd of occurrence: {4}\nDuration: {5}");
    /**
     * Associated junction ID.
     */
    private int junctionID;
    /**
     * Associated monitoring zone ID.
     */
    private int monitoringZoneID;
    private String detectorID;
    /**
     * Associated section ID.
     */
    private int sectionID;
    /**
     * Pair id of the upstream detector.
     */
    private String upstreamDetectorPair;
    /**
     * Pair id of the downstream detector.
     */
    private String downstreamDetectorPair;
    /**
     * Time (seconds from simulation start) when the incident has occurred.
     */
    private float startTime;
    /**
     * Time (seconds from simulation start) when the incident has been resolved.
     */
    private float endTime;
    /**
     * Time (seconds from simulation start) when the incident has been reported by the algorithm.
     */
    private float reportTime;
    /**
     * Determines the evaluation status of the incident.
     */
    private EvaluationStatus evaluationStatus;
    /**
     * Defines the type of the incident.
     */
    private IncidentType incidentType;
    /**
     * Shows whether the incident is confirmed or not.
     */
    private boolean isConfirmed;

    public Incident() {
        incidentType = IncidentType.DETECTOR_INCIDENT;
        evaluationStatus = EvaluationStatus.NOT_EVALUATED;
    }

    public IncidentType getType() {
        return this.incidentType;
    }

    public void setType(IncidentType incidentType) {
        this.incidentType = incidentType;
    }

    /**
     * Returns the id of the junction at which the incident has occurred.
     */
    public int getJunctionID() {
        return junctionID;
    }

    /**
     * Sets the id of the junction at which the incident has occurred.
     */
    public void setJunctionID(int junctionID) {
        this.junctionID = junctionID;
    }

    /**
     * Returns the id of the section in which the incident has occurred.
     */
    public int getSectionID() {
        return this.sectionID;
    }

    /**
     * Sets the id of the section at which the incident has occurred.
     */
    public void setSectionID(int sectionID) {
        this.sectionID = sectionID;
    }

    /**
     * Returns the id monitoring zone in which this incident has occurred.
     */
    public int getMonitoringZoneID() {
        return monitoringZoneID;
    }

    /**
     * Sets the monitoring zone in which this incident has occurred.
     */
    public void setMonitoringZoneID(int monitoringZoneID) {
        this.monitoringZoneID = monitoringZoneID;
    }

    /**
     * Sets the id of the downstream detector pair which has detected this
     * incident.
     */
    public void setDownstreamDetectorPair(String pairID) {
        this.downstreamDetectorPair = pairID;
    }

    public float getEndTime() {
        return endTime;
    }

    public void setEndTime(float endTime) {
        this.endTime = endTime;
    }

    /**
     * Returns the id of the upstream detector pair which has detected this
     * incident.
     */
    public String getUpstreamDetectorPair() {
        return this.upstreamDetectorPair;
    }

    /**
     * Sets the id of the upstream detector pair which has detected this
     * incident.
     */
    public void setUpstreamDetectorPair(String pairID) {
        this.upstreamDetectorPair = pairID;
    }

    public float getReportTime() {
        return reportTime;
    }

    public void setReportTime(float reportTime) {
        this.reportTime = reportTime;
    }

    public float getStartTime() {
        return startTime;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public EvaluationStatus getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(EvaluationStatus evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    /**
     * Returns the incident description.
     */
    public final String getDescription() {
        Collection<Object> params = new ArrayList<>();

        switch (this.incidentType) {
            case DETECTOR_INCIDENT:
                params.add(this.detectorID);
                params.add(this.sectionID);
                params.add(this.upstreamDetectorPair);
                params.add(this.downstreamDetectorPair);
                params.add(this.reportTime);
                params.add(this.startTime);
                params.add(this.endTime);
                params.add(getDuration());
                return detectorTemplate.format(params.toArray());
            case JUNCTION_INCIDENT:
                params.add(this.junctionID);
                params.add(this.reportTime);
                params.add(this.startTime);
                params.add(this.endTime);
                params.add(getDuration());
                return junctionTemplate.format(params.toArray());
            case SECTION_INCIDENT:
                params.add(this.sectionID);
                params.add(this.reportTime);
                params.add(this.startTime);
                params.add(this.endTime);
                params.add(getDuration());
                return sectionTemplate.format(params.toArray());
        }

        return "";
    }

    /**
     * Returns the duration of the incident or 0 if the end time is not set.
     */
    private float getDuration() {
        if (this.endTime > 0) {
            return this.endTime - this.startTime;
        }

        return 0;
    }

    /**
     * Returns if the incident is confirmed or not.
     */
    public boolean isConfirmed() {
        return isConfirmed;
    }

    /**
     * Sets the status of the incident.
     */
    public void setConfirmed(boolean isConfirmed) {
        this.isConfirmed = isConfirmed;
    }

    public void setDetectorID(String detectorID) {
        this.detectorID = detectorID;
    }

    /**
     * Defines the evaluation status of an incident.
     */
    public enum EvaluationStatus {
        /**
         * the number of roads that are predicted to be congested and actually are congested
         */
        TRUE_POSITIVE,
        /**
         * the number of roads that are predicted to be congested but traffic flows freely
         */
        FALSE_POSITIVE,
        /**
         * the number of roads that are predicted to be on free flow and traffic flows freely
         */
        TRUE_NEGATIVE,
        /**
         * the number of roads that are predicted to be on free flow but actually are congested
         */
        FALSE_NEGATIVE,
        NOT_EVALUATED
    }

    /**
     * Defines the type of the incident. Through that it is possible to
     * identify, which distinguish are provided in the incident (E.g. the type
     * DETECTOR_INCIDENT means that the incident was identified by detectors and
     * therefore contains information about the ids of those detectors).
     */
    public enum IncidentType {
        JUNCTION_INCIDENT, SECTION_INCIDENT, DETECTOR_INCIDENT
    }
}
