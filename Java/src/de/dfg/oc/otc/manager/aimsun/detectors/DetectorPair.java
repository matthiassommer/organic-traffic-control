package de.dfg.oc.otc.manager.aimsun.detectors;

import de.dfg.oc.otc.aid.TrafficDataExport;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.aimsun.Link;

import java.util.Observable;
import java.util.Observer;

/**
 * A detector pair consists of two detectors on the same section or on different
 * sections on the same road covering the same lanes.
 *
 * @author Matthias Sommer
 */
public class DetectorPair extends AbstractDetectorGroup implements Observer {
    private final Detector downstreamDetector;
    private final float monitoredDistance;
    private final Detector upstreamDetector;
    private DetectorDataValue currentDownstreamValue;
    private DetectorDataValue currentUpstreamValue;
    private boolean downstreamDetectorNotified;
    private boolean upstreamDetectorNotified;

    public DetectorPair(final Detector upstreamDetector, final Detector downstreamDetector, final Link associatedLink) {
        super();
        this.upstreamDetector = upstreamDetector;
        this.downstreamDetector = downstreamDetector;
        this.monitoredDistance = associatedLink.getDetectorDistance(upstreamDetector, downstreamDetector);
        setType(Type.SIMPLE_DETECTOR_PAIR);
    }

    public final float getMonitoredDistance() {
        return this.monitoredDistance;
    }

    public final Detector getDownstreamDetector() {
        return this.downstreamDetector;
    }

    @Override
    public final String getPairDescription() {
        return "Upstream detector " + this.upstreamDetector.getId() + ", Downstream detector "
                + this.downstreamDetector.getId();
    }

    public final Detector getUpstreamDetector() {
        return this.upstreamDetector;
    }

    @Override
    public final boolean isEquallyComposed(final AbstractDetectorGroup pair) {
        if (pair.getType() != getType()) {
            return false;
        }

        if (pair.getType() == Type.SIMPLE_DETECTOR_PAIR) {
            final int upId = ((DetectorPair) pair).upstreamDetector.getId();
            final int downId = ((DetectorPair) pair).downstreamDetector.getId();

            if (upId == this.upstreamDetector.getId() && downId == this.downstreamDetector.getId()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public final void registerAsObserver() {
        this.upstreamDetector.addObserver(this);
        this.downstreamDetector.addObserver(this);
    }

    @Override
    public final void update(final Observable o, final Object arg) {
        final Detector detector = (Detector) o;
        final DetectorDataValue value = (DetectorDataValue) arg;

        if (detector.equals(this.upstreamDetector)) {
            // no value until now or current value newer then stored one?
            if (this.currentUpstreamValue == null || value.getTime() > this.currentUpstreamValue.getTime()) {
                this.currentUpstreamValue = value;
                this.upstreamDetectorNotified = true;
            }
        } else {
            if (this.currentDownstreamValue == null || value.getTime() > this.currentDownstreamValue.getTime()) {
                this.currentDownstreamValue = value;
                this.downstreamDetectorNotified = true;
            }
        }

        // new values for both detectors received
        if (this.upstreamDetectorNotified && this.downstreamDetectorNotified) {
            this.upstreamDetectorNotified = false;
            this.downstreamDetectorNotified = false;

            // calculate new aggregated value
            final int numberFeatures = value.getValues().length;
            final float[] aggregate = new float[numberFeatures];

            for (int i = 0; i < numberFeatures; i++) {
                aggregate[i] = (currentUpstreamValue.getValues()[i] + currentDownstreamValue.getValues()[i]) / 2;   // MERKE: Hier wird aus zwei Detektorenwerten einer gebildet
            }

            final DetectorDataValue aggregatedValue = new DetectorDataValue(value.getTime(), aggregate);

            setChanged();
            notifyObservers(aggregatedValue);

            String upstreamID = "" + this.upstreamDetector.getId();
            String downstreamID = "" + this.downstreamDetector.getId();
            String id = upstreamID + downstreamID;
            TrafficDataExport.exportData(id, aggregatedValue);
        }
    }
}
