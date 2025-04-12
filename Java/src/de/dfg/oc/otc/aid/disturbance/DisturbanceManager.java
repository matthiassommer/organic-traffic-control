package de.dfg.oc.otc.aid.disturbance;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer1.observer.Layer1Observer;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.TrafficType;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class is responsible for managing disturbances within the traffic network.
 * Disturbances are e.g. road closures, temporary decreases in capacity, or
 * oversaturation.
 * <p>
 * As it is not possible to send disturbances from Aimsun, that's how we simulate them.
 */
public final class DisturbanceManager {
    private static final Logger log = Logger.getLogger(DisturbanceManager.class);
    private static final DisturbanceManager INSTANCE = new DisturbanceManager();
    /**
     * Currently active disturbances.
     */
    private final List<Disturbance> activeDisturbances;
    /**
     * All disturbances received within the simulation time.
     */
    private final List<Disturbance> disturbances;
    /**
     * Former disturbances which are not active anymore.
     */
    private final List<Disturbance> history;
    /**
     * Defines how to calculate the degree of this disturbance.
     */
    private DegreeType degreeCalculationMethod = DegreeType.NONE;
    /**
     * Is TLC change active?
     */
    private final int TLC_CHANGE_ACTIVE;
    /**
     * Are tentative Incidents allowed to change TLC?
     */
    private final boolean useTentativeIncidents;
    /**
     * Last time-step the DM has been performed.
     */
    private float lastTimeStep;
    /**
     * Defines interval (in simulation steps) to calculate degree
     */
    private float CALCULATION_INTERVAL = 60;

    private DisturbanceManager() {
        this.disturbances = new ArrayList<>(2);
        this.activeDisturbances = new ArrayList<>(2);
        this.history = new ArrayList<>(2);

        try {
            degreeCalculationMethod = DegreeType.valueOf(DefaultParams.DIST_DEGREE_CALCULATION_METHOD);
        } catch (IllegalArgumentException e) {
            log.error("Unknown degreeCalculationMethod");
        }

        CALCULATION_INTERVAL = DefaultParams.DIST_CALCULATION_INTERVAL;
        TLC_CHANGE_ACTIVE = DefaultParams.DIST_TLC_CHANGE_ACTIVE;
        useTentativeIncidents = DefaultParams.DIST_USE_TENTATIVE_INCIDENTS;
    }

    public static DisturbanceManager getInstance() {
        return INSTANCE;
    }

    public List<Disturbance> getDisturbances() {
        return disturbances;
    }

    /**
     * Method used to query DM, if there is a disturbance known for a specific
     * link (turning or section) or not.
     *
     * @param linkID The identifier of the link
     * @return The set of disturbances known for the link - null otherwise
     */
    public List<Disturbance> getDisturbancesForLink(final int linkID) {
        return activeDisturbances.stream().filter(disturbance -> disturbance.affectsLink(linkID)).map(disturbance -> disturbance).collect(Collectors.toList());
    }

    /**
     * Method performed at each simulation step: Activates and deactivates
     * disturbances.
     */
    public void step(final float currentTime) {
        if (!DefaultParams.DIST_ACTIVE) {
            return;
        }

        // check, if Disturbance is finished
        Iterator<Disturbance> it = activeDisturbances.iterator();
        while (it.hasNext()) {
            Disturbance disturbance = it.next();
            float end = disturbance.getEnd();

            // Wenn Disturbance abgelaufen, weiterreichen in die History!
            if (!Float.isNaN(end) && end <= currentTime) {
                history.add(disturbance);
                it.remove();
            }

            // Calculate new degree for this interval
            if (disturbance.getType() == DisturbanceType.INCIDENT && currentTime - disturbance.getLastCalculation() >= CALCULATION_INTERVAL) {
                disturbance.setDegree(calculateDegree(disturbance.getIntersection()));

                //  disturbance.calculateDegree(degreeCalculationMethod);
                disturbance.setLastCalculation(currentTime);
            }
        }

        // activate Disturbance message
        Iterator<Disturbance> it2 = disturbances.iterator();
        while (it2.hasNext()) {
            Disturbance disturbance = it2.next();
            if (disturbance.getStart() <= currentTime) {
                activeDisturbances.add(disturbance);
                it2.remove();
            } else {
                break;
            }
        }

        // update time stamp
        lastTimeStep = currentTime;
    }

    /**
     * Method used to notify DM of a new Disturbance.
     */
    private void submitDisturbance(final Disturbance newDisturbance) {
        if (!Float.isNaN(newDisturbance.getEnd())
                && (newDisturbance.getEnd() <= lastTimeStep || newDisturbance.getEnd() <= newDisturbance.getStart())) {
            return;
        }

        disturbances.add(newDisturbance);
        log.info("Received new newDisturbance: " + newDisturbance.getDescription());
    }

    /**
     * Method used to build a Disturbance object from a new Incident.
     */
    private Disturbance buildDisturbanceFromIncident(final Incident incident, OTCNode intersection) {
        Disturbance disturbance = new Disturbance();

        // When: Timestep begin and end
        // end = NaN since the incident hasn't ended yet
        final float begin = incident.getStartTime();
        disturbance.setStart(begin);
        disturbance.setEnd(Float.NaN);
        disturbance.setLastCalculation(begin);
        disturbance.setIntersection(intersection);

        disturbance.linkIncident(incident);
        disturbance.setType(DisturbanceType.INCIDENT);

        // Where (section ID)
        disturbance.addLinkID(true, incident.getSectionID());

        disturbance.calculateDegree(degreeCalculationMethod);

        return disturbance;
    }

    /**
     * Method is called from AIDAlgorithm to submit a new tentative incident from a intersection.
     *
     * @param incident     The tentative incident
     * @param intersection Intersection which has detected an incident on an outgoing monitoring zone.
     */
    public void startDisturbance(final Incident incident, OTCNode intersection) {
        Disturbance disturbance = buildDisturbanceFromIncident(incident, intersection);

        disturbance.setDegree(calculateDegree(intersection));

        if (!incident.isConfirmed()) {
            // only tentative incidents are allowed to change the TLC
            if (TLC_CHANGE_ACTIVE == 1 && useTentativeIncidents) {
                adjustGreenTimes(disturbance, intersection, incident);
            }
        } else {
            confirmDisturbance(incident, intersection);
        }
        submitDisturbance(disturbance);
    }

    private void adjustGreenTimes(Disturbance disturbance, OTCNode intersection, Incident incident) {
        // shorten green time for turnings from the incoming intersection
        intersection.adjustGreenTimesForIncomingIntersection(disturbance);

        // increase green time for turnings to the outgoing intersection
        Section section = OTCManager.getInstance().getNetwork().getSection(incident.getSectionID());
        List<Integer> nextIntersections = section.determineReceivingNodes(new ArrayList<>(), new ArrayList<>());
        List<Section> path = section.determineCompletePathToNextJunction(new ArrayList<>());
        OTCNode nextIntersection = OTCManager.getInstance().getNetwork().getJunction(nextIntersections.get(0)).getNode();
        //nextIntersection.adjustGreenTimesForOutgoingIntersection(disturbance, path.get(path.size() - 1));
    }

    /**
     * Method is called from AIDAlgorithm to confirm a tentative incident.
     */
    public void confirmDisturbance(final Incident incident, OTCNode intersection) {
        Disturbance disturbance = getLinkDisturbance(incident);

        if (disturbance == null) {
            // reached in case no method for the disturbance calculation was specified
            disturbance = buildDisturbanceFromIncident(incident, intersection);
        } else {
            disturbance.setDegree(calculateDegree(intersection));
        }

        // only confirmed incidents are allowed to change the TLC
        if (TLC_CHANGE_ACTIVE == 1 && !useTentativeIncidents) {
            adjustGreenTimes(disturbance, intersection, incident);
        }
    }

    private float calculateDegree(OTCNode intersection) {
        // oder auch Attribute.LOS, kann aber über 1 steigen!
        float degree = intersection.getEvaluation(Layer1Observer.DataSource.STATISTICS, Attribute.UTILISATION, TrafficType.INDIVIDUAL_TRAFFIC, true) / 100;
        return degree;
    }

    /**
     * Submit the ending time of an incident.
     *
     * @param incident     The incident
     * @param intersection The OTCNode that detected the Incident
     */
    public void endDisturbance(final Incident incident, OTCNode intersection) {
        Disturbance disturbanceToStop = getLinkDisturbance(incident);

        if (disturbanceToStop != null) {
            if (incident.getType() == Incident.IncidentType.DETECTOR_INCIDENT) {
                disturbanceToStop.setEnd(incident.getEndTime());

                if (TLC_CHANGE_ACTIVE == 1) {
                    intersection.resetTLC(disturbanceToStop);

                    OTCNode nextIntersection = findNextIntersection(incident);
                    nextIntersection.resetTLC(disturbanceToStop);
                }
            }
        }
    }

    private OTCNode findNextIntersection(Incident incident) {
        Section section = OTCManager.getInstance().getNetwork().getSection(incident.getSectionID());
        List<Integer> nextIntersections = section.determineReceivingNodes(new ArrayList<>(), new ArrayList<>());
        return OTCManager.getInstance().getNetwork().getJunction(nextIntersections.get(0)).getNode();
    }

    /**
     * Method used to find disturbances which are or were already linked to an incident.
     *
     * @param incident The incident
     * @return The linked disturbance
     */
    private Disturbance getLinkDisturbance(final Incident incident) {
        for (Disturbance disturbance : activeDisturbances) {
            if (disturbance.getLinkIncident() == incident) {
                return disturbance;
            }
        }

        for (Disturbance disturbance : history) {
            if (disturbance.getLinkIncident() == incident) {
                return disturbance;
            }
        }

        return null;
    }
}
