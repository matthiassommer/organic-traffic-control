package de.dfg.oc.otc.routing.linkState;

import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.RouteEntry;

/**
 * Extension of the {@link de.dfg.oc.otc.routing.RouteEntry} by adding a sequence number and
 * specialising it for {@link LinkStateRC}.
 *
 * @author lyda
 */
public class DatabaseEntry extends RouteEntry {
    /**
     * The sequence number of this entry as an indicator how old this message is.
     */
    private final int sequenceNumber;
    /**
     * Only used for Border protocols.
     */
    private final int realOutSectionID;

    /**
     * Create a new database entry.
     *
     * @param sourceRC       sender node
     * @param destRC         RC of the next junction
     * @param destCentroid   target centroid
     * @param inSection      of this node
     * @param outSection     of this node
     * @param costs          from the origin to the destination
     * @param sequenceNumber of the advertisement
     */
    public DatabaseEntry(final LinkStateRC sourceRC, final LinkStateRC destRC, final Centroid destCentroid,
                         final Section inSection, final Section outSection, final float costs,
                         final int realOutSectionID, final int sequenceNumber) {
        this.sourceRC = sourceRC;
        this.destinationRC = destRC;
        this.destinationCentroid = destCentroid;
        this.inSection = inSection;
        this.outSection = outSection;
        this.costs = costs;
        this.sequenceNumber = sequenceNumber;
        this.realOutSectionID = realOutSectionID;
    }

    public final int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public int getRealOutSectionID() {
        return realOutSectionID;
    }

    @Override
    public final String toString() {
        int dest;
        if (this.destinationRC == null) {
            dest = this.destinationCentroid.getId();
        } else {
            dest = this.destinationRC.getNodeID();
        }
        return "Origin RC: " + this.sourceRC.getNodeID() + "\tDestination: " + dest + "\tInsection: " +
                this.inSection.getId() + "\tOutsection: " + this.outSection.getId() + "\tCosts: " + this.costs;
    }
}
