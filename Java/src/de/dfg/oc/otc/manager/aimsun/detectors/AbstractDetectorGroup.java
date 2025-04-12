package de.dfg.oc.otc.manager.aimsun.detectors;

import java.util.Observable;

/**
 * An AbstractDetectorGroup is either a simple detector pair a group of
 * detectors covering a fork. It has certain type and id.
 *
 * @author Matthias Sommer
 */
public abstract class AbstractDetectorGroup extends Observable implements Cloneable {
    /**
     * ID of the detector group.
     */
    private String id;
    /**
     * {@link Type} of the detector group.
     */
    private Type type;
    /**
     * Distance of this detector group to the start of the section.
     */
    private float distanceToStart = -1;
    /**
     * Distance of this detector group to the end of the section.
     */
    private float distanceToEnd = -1;

    /**
     * Returns the id of this detector group.
     */
    public final String getId() {
        return this.id;
    }

    /**
     * Sets the id of this detector group.
     */
    public final void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the distance of this detector group to the start of the section.
     */
    public final float getDistanceToStart() {
        return this.distanceToStart;
    }

    /**
     * Sets the distance of this detector group to the start of the section.
     */
    public final void setDistanceToStart(final float distanceToStart) {
        this.distanceToStart = distanceToStart;
    }

    /**
     * Returns the distance of this detector group to the end of the section.
     */
    public final float getDistanceToEnd() {
        return this.distanceToEnd;
    }

    /**
     * Sets the distance of this detector group to the end of the section.
     */
    public final void setDistanceToEnd(final float distanceToEnd) {
        this.distanceToEnd = distanceToEnd;
    }

    /**
     * Returns the type of this detector group.
     */
    public final Type getType() {
        return this.type;
    }

    /**
     * Sets the type of this detector group.
     */
    public final void setType(final Type type) {
        this.type = type;
    }

    /**
     * Returns a clone instance of the detector pair.
     * <p>
     * This method is needed to pass copies between nodes.
     *
     * @return The cloned detector pair
     * @throws CloneNotSupportedException
     */
    public final AbstractDetectorGroup getClone() throws CloneNotSupportedException {
        return (AbstractDetectorGroup) clone();
    }

    /**
     * Returns an easy textual description of the detector pair with information
     * about the contained detectors.
     *
     * @return description
     */
    public abstract String getPairDescription();

    /**
     * This method returns if this detector group is equally composed as the
     * given group. Thereby only the type of each detector is compared, but not
     * the ID.
     * <p>
     * Used to exclude duplicate detector groups from a node.
     */
    public abstract boolean isEquallyComposed(AbstractDetectorGroup pair);

    /**
     * The method is used to register this class as observer for all contained
     * detectors.
     */
    public abstract void registerAsObserver();

    /**
     * Types of pairs. MonitoredInput for example means that only inputs are
     * being observed.
     */
    public enum Type {
        DIVIDED_MONITORED,
        DIVIDED_MONITORED_INPUTS,
        DIVIDED_MONITORED_OUTPUTS,
        DIVIDED_UNMONITORED_BOTH,
        SIMPLE_DETECTOR_PAIR
    }
}
