package de.dfg.oc.otc.aid.evaluation;

import de.dfg.oc.otc.aid.Incident;

/**
 * Custom listener used for evaluation and calibration actions at the aid gui.
 */
public interface IncidentEvaluationListener {
    /**
     * This method is called every time the evaluation status from an incident
     * is changed.
     *
     * @param incident        which has been evaluated
     * @param firstEvaluation Incident has been evaluated for the first time
     */
    void incidentEvaluated(Incident incident, boolean firstEvaluation);
}
