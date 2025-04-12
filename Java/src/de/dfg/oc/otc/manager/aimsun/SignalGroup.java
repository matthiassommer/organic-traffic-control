package de.dfg.oc.otc.manager.aimsun;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a Signalgroup consisting of different turnings.
 * All traffic lights of these turnings show at all times the same light.
 */
public class SignalGroup {
    /**
     * ID of phase in AIMSUN.
     */
    private final int phaseID;
    /**
     * List of associated turnings.
     */
    private final List<Turning> turnings = new ArrayList<>(3);

    /**
     * Erzeugt eine neue Signalgruppe.
     *
     * @param id Id der Phase zur Zuordnung zu einer im Simulator vorhandenen
     *           Signalgruppe.
     */
    SignalGroup(final int id) {
        this.phaseID = id;
    }

    /**
     * Add a turning to this signal group.
     */
    final void addTurning(final Turning turning) {
        this.turnings.add(turning);
    }

    public final int getId() {
        return phaseID;
    }

    /**
     * Returns the number of lanes assigned to this signal group.
     *
     * @return number of lanes assigned to this signal group
     */
    public final int getNumberOfLanes() {
        if (turnings.isEmpty()) {
            return 0;
        }

        int minLane = Integer.MAX_VALUE;
        int maxLane = Integer.MIN_VALUE;

        for (Turning turning : turnings) {
            minLane = Math.min(turning.getFirstLaneOrigin(), minLane);
            maxLane = Math.max(turning.getLastLaneOrigin(), maxLane);
        }
        return maxLane - minLane + 1;

    }

    /**
     * Gibt die Anzahl der Abbiegebeziehungen f�r diese Signalgruppe zur�ck.
     *
     * @return die Anzahl der Abbiegebeziehungen f�r diese Signalgruppe
     */
    public final int getNumberOfTurnings() {
        return turnings.size();
    }

    final String getSimpleDescription() {
        String output = "<h3>SignalGroup " + phaseID + " (" + turnings.size()
                + (turnings.size() == 1 ? " Turning)</h3>" : " Turnings)</h3>");
        output = output
                .concat("<table><tr><th>Id</th><th>From</th><th>To</th><th>Traffic Type</th><th>Relevant Detectors</th></tr>");

        for (Turning turning : turnings) {
            output = output.concat(turning.getDescription());
        }

        output = output.concat("</table>");

        return output;
    }

    /**
     * Return a list of the turnings associated with this signal group.
     *
     * @return associated turnings
     */
    public final List<Turning> getTurnings() {
        return turnings;
    }

    @Override
    public String toString() {
        String output = "SignalGroup " + phaseID + ", " + turnings.size() + " Turning IDs:";

        for (Turning turning : turnings) {
            output = output.concat(turning.getId() + " ");
        }

        return output;
    }
}
