package de.dfg.oc.otc.manager.aimsun;

import java.awt.geom.Point2D;
import java.util.Arrays;

/**
 * A centroid is a node at the border of the network. Traffic is generated and
 * consumed here.
 *
 * @author Matthias Sommer
 */
public class Centroid {
    /**
     * Coordinates in grid-system.
     */
    private Point2D.Double coordinates;
    /**
     * ID of the centroid.
     */
    private final int id;
    /**
     * List of IDs of incoming sections.
     */
    private final int[] incomingSectionIds;
    /**
     * List of IDs of outgoing sections.
     */
    private final int[] outgoingSectionIds;

    /**
     * Creates a centroid (represented by its id and lists of incoming and
     * outgoing sections).
     *
     * @param centId           the centroid id
     * @param incomingSections ids of incoming sections
     * @param outgoingSections ids of outgoing sections
     */
    Centroid(final int centId, final int[] incomingSections, final int[] outgoingSections) {
        this.id = centId;
        this.incomingSectionIds = incomingSections;
        this.outgoingSectionIds = outgoingSections;
    }

    public final Point2D.Double getCoordinates() {
        return this.coordinates;
    }

    /**
     * Returns description for inclusion in the GUI.
     *
     * @return a description of this centroid
     */
    public final String getDescription() {
        String coordinates = "none";
        if (this.coordinates != null) {
            coordinates = this.coordinates.toString();
        }

        return "<h1>Centroid " + id + "</h1><table><tr><td>Incoming sections: </td><td><b>"
                + Arrays.toString(incomingSectionIds) + "</b></td></tr><tr><td>Outgoing sections: </td><td><b>"
                + Arrays.toString(outgoingSectionIds) + "</b></td></tr><tr><td>Coordinates: </td><td><b>" + coordinates
                + "</b></td></tr>";
    }

    public final int getId() {
        return id;
    }

    public final void setCoordinates(final Point2D.Double coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public final String toString() {
        return "Centroid " + id + "; insections " + Arrays.toString(incomingSectionIds) + "; outsections "
                + Arrays.toString(outgoingSectionIds);
    }
}
