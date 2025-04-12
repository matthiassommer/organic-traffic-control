package de.dfg.oc.otc.aid.algorithms.svm;

import de.dfg.oc.otc.aid.Incident;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Class for storing the detected incidents and classifiers as correct or false alarms.
 * Additionally, the not detected incidents are stored and written into files.
 * <p>
 * Created by Christoph Weiss on 22.06.2015.
 */
public class IncidentStorage implements Observer {
    /**
     * Container for all incidents raised by an algorithm.
     */
    @NotNull
    private final List<Incident> raisedIncidents;
    /**
     * Container for all incidents not detected/predicted by an algorithm.
     */
    @NotNull
    private final List<Incident> notDetectedIncidents;
    /**
     * File path where the incidents are stored.
     */
    protected final String filename;

    public IncidentStorage(String filename) {
        this.raisedIncidents = new ArrayList<>();
        this.notDetectedIncidents = new ArrayList<>();
        this.filename = filename;

        createNewFile(filename);
    }

    private void createNewFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }

        File file = new File(filename);
        // create folder
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }

        if (file.exists()) {
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public void storeIncident(Incident incident) {
        raisedIncidents.add(incident);
    }

    /**
     * Classify an incident as TRUE_POSITIVE (TP) or FALSE_POSITIVE (FP) and creates an object in notDetectedIncidents, if an incident occured but the algor. did not detect any (FN).
     * <p>
     * Attention: Incident is only identified by getStartTime(). For tentative incidents, a new incident
     * object has to be saved by storeIncident.
     * Attention: Dummy incident has only attributes EvaluationStatus.TRUE_POSITIVE, startTimeOfOccurence, and as Id
     * a -1. By the negative id the incident were identifed in file as not detected.
     *
     * @param timestep        Timestep which is to evaluated
     * @param incidentOccured if incident is occured or not
     */
    public void classifyIncident(float timestep, boolean incidentOccured) {
        Incident evaluatedIncident = null;
        for (Incident incident : raisedIncidents) {
            if (incident.getStartTime() == timestep) {
                if (incidentOccured) {
                    incident.setEvaluationStatus(Incident.EvaluationStatus.TRUE_POSITIVE);
                } else {
                    incident.setEvaluationStatus(Incident.EvaluationStatus.FALSE_POSITIVE);
                }
                evaluatedIncident = incident;
            }
        }

        // create new incident if no object was found
        if (incidentOccured && evaluatedIncident == null) {
            evaluatedIncident = new Incident();
            evaluatedIncident.setEvaluationStatus(Incident.EvaluationStatus.FALSE_NEGATIVE);
            evaluatedIncident.setStartTime(timestep);
            notDetectedIncidents.add(evaluatedIncident);
        }

        if (this.filename != null && evaluatedIncident != null) {
            writeInFile(evaluatedIncident);
        }
    }

    /**
     * Writes an DetectorDataValue to the end of a file.
     * Format is [getStartTime, getEvaluationStatus, getId, getEvaluationStatus]
     * Attention: getStartTime is in minutes, not in seconds
     * first getEvaluationStatus is -1 if Incident.EvaluationStatus.TRUE_POSITIVE or 1 otherwise.
     *
     * @param incident The Incident which is saved to the file
     */
    private void writeInFile(@NotNull Incident incident) {
        String line = (int) (incident.getStartTime() / 60f)
                + " " + (incident.getEvaluationStatus() == Incident.EvaluationStatus.TRUE_POSITIVE ? 1 : -1)
                + " " + incident.getEvaluationStatus() + "\n";

        try {
            File file = new File(filename);

            BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile(), true));
            bw.append(line);
            bw.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Check if incident occured at a certain time.
     *
     * @param time requested time
     * @return incident object or null
     */
    @Nullable
    public Incident getIncident(float time) {
        for (Incident incident : notDetectedIncidents) {
            if (incident.getStartTime() == time) {
                return incident;
            }
        }

        for (Incident incident : raisedIncidents) {
            if (incident.getStartTime() == time) {
                return incident;
            }
        }
        return null;
    }

    public void writeAll() {
        notDetectedIncidents.forEach(this::writeInFile);
        raisedIncidents.forEach(this::writeInFile);
    }

    @NotNull
    public List<Incident> getNotDetectedIncidents() {
        return notDetectedIncidents;
    }

    @NotNull
    public List<Incident> getRaisedIncidents() {
        return raisedIncidents;
    }

    @Override
    public void update(Observable o, Object obj) {
        storeIncident((Incident) obj);
    }
}
