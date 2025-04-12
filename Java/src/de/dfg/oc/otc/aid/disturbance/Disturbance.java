package de.dfg.oc.otc.aid.disturbance;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Class represents a disturbance within the OTC-system (road closures,
 * temporary decrease in capacity, or oversaturation). A disturbance is either
 * on a section or a turning.
 *
 * @author tomforde
 */
public class Disturbance implements Comparable<Disturbance> {
    /**
     * Disturbance affects following sections.
     */
    private final List<Integer> sectionIDs;
    /**
     * Disturbance affects following turnings.
     */
    private final List<Integer> turningIDs;
    /**
     * A stativ malus assigned as degree for the disturbance impact.
     */
    private final float staticDegreeValue;
    private OTCNode intersection;
    /**
     * Type of the disturbance.
     */
    private DisturbanceType type;
    /**
     * Disturbance affects a link with x degree (0 to 1).
     */
    private float degree;
    /**
     * Disturbance ends at simulation second z.
     */
    private float end;
    /**
     * Disturbance starts at simulation second y.
     */
    private float start;
    /**
     * Incident linked with Disturbance.
     */
    private Incident incident;
    /**
     * Disturbance ends at simulation second z.
     */
    private float lastCalculation;
    Disturbance() {
        this.sectionIDs = new ArrayList<>(1);
        this.turningIDs = new ArrayList<>(1);
        this.lastCalculation = 0;
        this.staticDegreeValue = DefaultParams.DIST_STATIC_DEGREE;
        if (this.staticDegreeValue > 1) {
            throw new IllegalArgumentException("staticDegreeValue must be within [0;1], was " + this.staticDegreeValue);
        }
    }

    public OTCNode getIntersection() {
        return intersection;
    }

    public void setIntersection(OTCNode intersection) {
        this.intersection = intersection;
    }

    final void addLinkID(final boolean isSection, final int id) {
        if (isSection) {
            addSectionID(id);
        } else {
            addTurningID(id);
        }
    }

    private void addSectionID(final int id) {
        if (!sectionIDs.contains(id)) {
            sectionIDs.add(id);
        }
    }

    private void addTurningID(final int id) {
        if (!turningIDs.contains(id)) {
            turningIDs.add(id);
        }
    }

    /**
     * Is a given section (specified via ID) affected by this disturbance?
     *
     * @param sectionID Identifier of the link
     * @return boolean disturbed?
     */
    final boolean affectsLink(final int sectionID) {
        List<Integer> list = sectionIDs;

        if (list.isEmpty()) {
            return false;
        }

        for (Integer id : list) {
            if (id == sectionID) {
                return true;
            }
        }

        return false;
    }

    /**
     * Comparable-Interface: Used to sort disturbances using their begin-time.
     */
    public final int compareTo(@NotNull final Disturbance otherDisturbance) {
        if (Float.isNaN(otherDisturbance.start) || Float.isNaN(this.start)) {
            System.err.println("Cannot compare disturbances - Float.NaN!");
        } else if (this.start < otherDisturbance.start) {
            return -1;
        } else if (this.start > otherDisturbance.start) {
            return 1;
        }
        return 0;
    }

    /**
     * Degree of disturbance between 0 and 1.
     *
     * @return degree of disturbance
     */
    public final float getDegree() {
        return degree;
    }

    final void setDegree(final float degree) {
        this.degree = degree;
    }

    /**
     * Method generates a complete description of the current
     * {@link Disturbance} object.
     *
     * @return description
     */
    public final String getDescription() {
        final StringBuilder sb = new StringBuilder();

        if (!Float.isNaN(start)) {
            sb.append("start at ").append(this.start).append(", ");
        } else {
            sb.append("no defined start, ");
        }

        if (!Float.isNaN(end)) {
            sb.append("end at ").append(this.end).append(", ");
        } else {
            sb.append("no defined end, ");
        }

        if (!Float.isNaN(degree)) {
            sb.append("degree: ").append(degree).append(". ");
        } else {
            sb.append("no defined degree. ");
        }

        if (type == DisturbanceType.INCIDENT) {
            if (incident == null) {
                sb.append("No incident linked. ");
            } else {
                sb.append("Linked incident: ").append(incident.getDescription()).append(", ");
            }
        }

        if (turningIDs.isEmpty()) {
            sb.append("No turnings affected. ");
        } else {
            sb.append("Affects turnings: ");
            for (int i : turningIDs) {
                sb.append(i).append(", ");
            }
        }

        if (sectionIDs.isEmpty()) {
            sb.append("No sections affected. ");
        } else {
            sb.append("Affects sections: ");
            for (int i : sectionIDs) {
                sb.append(i).append(" ");
            }
        }

        return sb.toString();
    }

    /**
     * Method used to calculate degree from several variables.
     *
     * @param calculationMethod defines how to calculate the degree
     *                          NONE               = No Calculation
     *                          STATIC             = Static degree
     *                          SPEED              = Calculation with average Speed
     *                          OCCUPANCY          = Calculation with Occupancy
     *                          SPEEDOCCUPANCY  = Combined average Speed and Occupancy
     */
    void calculateDegree(final DegreeType calculationMethod) {
        Incident.IncidentType type = incident.getType();

        if (type == Incident.IncidentType.DETECTOR_INCIDENT) {
            switch (calculationMethod) {
                case NONE:
                    degree = 0;
                    break;
                case STATIC:
                    degree = staticDegreeValue;
                    break;
                case SPEED:
                    degree = calculateSpeedFactor();
                    break;
                case OCCUPANCY:
                    degree = calculateOccupancyFactor();
                    break;
                case SPEEDOCCUPANCY:
                    degree = calculateSpeedFactor() * calculateOccupancyFactor();
                    break;
            }
        }
    }

    /**
     * Method used to calculate speed_factor for degree.
     * Takes the measured speed values from the associated detectors.
     * speed_factor := 1 - speed_avg / speed_limit
     * <p>
     * if( speed_avg > speed_limit) then
     * speed_factor := 1.0f
     *
     * @return The calculated factor
     */
    private float calculateSpeedFactor() {
        Section affectedSection = OTCManager.getInstance().getNetwork().getSection(sectionIDs.get(0));
        float speed_limit = affectedSection.getSpeedlimit();

        float speed_sum = 0;
        for (Detector detector : affectedSection.getDetectors()) {
            speed_sum += detector.getValue(2).getValue();
        }

        float speed_avg = speed_sum / affectedSection.getDetectors().size();
        return 1 - Math.min(speed_avg / speed_limit, 1);
    }


    /**
     * Method used to calculate occupancy_factor for degree.
     * Takes the temporal occupancy from the associated detectors.
     *
     * @return The calculated factor
     */
    private float calculateOccupancyFactor() {
        Section affectedSection = OTCManager.getInstance().getNetwork().getSection(sectionIDs.get(0));

        float maxOccupancy = 0;
        for (Detector detector : affectedSection.getDetectors()) {
            maxOccupancy = Math.max(detector.getValue(3).getValue() / 100f, maxOccupancy);
        }
        return Math.min(maxOccupancy, 1);
    }

    public final float getEnd() {
        return end;
    }

    final void setEnd(final float end) {
        this.end = end;
    }

    public final float getStart() {
        return start;
    }

    final void setStart(final float start) {
        this.start = start;
    }

    public final DisturbanceType getType() {
        return type;
    }

    public final void setType(DisturbanceType type) {
        this.type = type;
    }

    Incident getLinkIncident() {
        return incident;
    }

    void linkIncident(Incident incident) {
        this.incident = incident;
    }

    final float getLastCalculation() {
        return lastCalculation;
    }

    final void setLastCalculation(final float time) {
        this.lastCalculation = time;
    }
}
