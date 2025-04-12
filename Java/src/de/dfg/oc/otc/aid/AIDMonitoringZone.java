package de.dfg.oc.otc.aid;

import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.aid.evaluation.AIDEvaluator;
import de.dfg.oc.otc.manager.aimsun.Link;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Monitoring Zone is a logical overlay of some links of the network. A zone
 * is managed by one node.
 */
public class AIDMonitoringZone {
    /**
     * Link which is monitored by this zone.
     */
    private final Link monitoredLink;
    /**
     * List of monitored detector pairs on the link.
     */
    private final List<AbstractDetectorGroup> monitoredDetectorPairs;
    /**
     * ID of the monitoring zone.
     */
    private int id;
    /**
     * Assigned AID algorithm.
     */
    private AbstractAIDAlgorithm aidAlgorithm;
    /**
     * Defines whether the monitoring zone is outgoing (false) or incoming
     * (true) according to its assigned node.
     */
    private boolean isIncoming;
    /**
     * Contains the detector pair which is the closest to the node/junction. It
     * is used for algorithms with support for junction monitoring.
     */
    private AbstractDetectorGroup nearestPairToJunction;

    /**
     * Default constructor which takes the monitored link as parameter.
     *
     * @param link which is monitored
     */
    AIDMonitoringZone(Link link) {
        this.monitoredDetectorPairs = new ArrayList<>();
        this.monitoredLink = link;
    }

    public int getId() {
        return this.id;
    }

    void setId(int id) {
        this.id = id;
    }

    /**
     * Adds a detector pair to the responsibility of this monitoring zone.
     *
     * @param pair which is being monitored
     */
    void addDetectorPair(AbstractDetectorGroup pair) {
        this.monitoredDetectorPairs.add(pair);
    }

    public List<AbstractDetectorGroup> getMonitoredDetectorPairs() {
        return this.monitoredDetectorPairs;
    }

    private int getMonitoredPairsCount() {
        return this.monitoredDetectorPairs.size();
    }

    /**
     * Returns the AID algorithm which is used by this monitoring zone.
     */
    public AbstractAIDAlgorithm getAIDAlgorithm() {
        return this.aidAlgorithm;
    }

    /**
     * Sets the AID algorithm which is used for this monitoring zone. Thereby it
     * also adds the algorithm as observer to the monitored detector pairs.
     *
     * @param algorithm which is used for this monitoring zone
     */
    public void setAIDAlgorithm(AbstractAIDAlgorithm algorithm) {
        this.aidAlgorithm = algorithm;

        for (AbstractDetectorGroup detectorPair : this.monitoredDetectorPairs) {
            detectorPair.addObserver(this.aidAlgorithm);
        }

        this.aidAlgorithm.setObservedDetectorCount(getMonitoredPairsCount());
        this.aidAlgorithm.setDirection(this.isIncoming);
    }

    /**
     * This method is called after all detector pairs have been assigned to the
     * monitoring zone. It orders the pairs according to their distance from the
     * node and defines the closest one.
     */
    void initDetectorPairs() {
        if (this.isIncoming) {
            this.monitoredDetectorPairs.sort(AIDUtilities.getInstance().getDetectorPairDistanceComparator(false));
        } else {
            this.monitoredDetectorPairs.sort(AIDUtilities.getInstance().getDetectorPairDistanceComparator(true));
        }

        this.nearestPairToJunction = this.monitoredDetectorPairs.get(0);
    }

    /**
     * Returns the evaluator of the algorithm.
     */
    public AIDEvaluator getEvaluator() {
        return this.aidAlgorithm.getAIDEvaluator();
    }

    /**
     * Returns the associated link of this monitoring zone.
     */
    public Link getMonitoredLink() {
        return this.monitoredLink;
    }

    /**
     * Returns the direction of this monitoring zone (incoming or outgoing).
     *
     * @return {@code true} if the direction of this monitoring zone is
     * incoming, {@code false} otherwise
     */
    public boolean isIncoming() {
        return this.isIncoming;
    }

    /**
     * Sets the direction of this monitoring zone (whether it is incoming to or
     * outgoing from the node).
     */
    void setIncoming(boolean incoming) {
        this.isIncoming = incoming;
    }

    /**
     * Returns the detector pair which is closest to the observed node (for
     * junction monitoring purposes).
     *
     * @return AbstractDetectorGroup which is closest to the observed node
     */
    public AbstractDetectorGroup getNearestPairToJunction() {
        return this.nearestPairToJunction;
    }

    /**
     * Checks if a detector pair is monitored by this zone.
     *
     * @param pair which is being tested
     * @return {@code true} if the given pair is monitored by this zone,
     * {@code false} otherwise
     */
    boolean isDetectorPairContained(AbstractDetectorGroup pair) {
        for (AbstractDetectorGroup detectorPair : this.monitoredDetectorPairs) {
            if (detectorPair.isEquallyComposed(pair)) {
                return true;
            }
        }
        return false;
    }
}
