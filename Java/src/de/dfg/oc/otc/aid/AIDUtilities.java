package de.dfg.oc.otc.aid;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXB;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * AID Helper Class.
 */
public class AIDUtilities {
    /**
     * Only one instance allowed for this class.
     */
    private static AIDUtilities instance;
    /**
     * Internal counter used for generating unique ids.
     */
    private int counter;

    private AIDUtilities() {
        counter = 0;
    }

    /**
     * Returns the singleton instance.
     */
    public static AIDUtilities getInstance() {
        if (instance == null) {
            instance = new AIDUtilities();
        }
        return instance;
    }

    /**
     * Returns an instance of the {@link DetectorComparator} class.
     *
     * @return Custom detector comparator
     */
    public Comparator<Detector> getDetectorComparator() {
        return new DetectorComparator();
    }

    /**
     * Returns an instance of the {@link DetectorPairComparator} class.
     *
     * @param startNode Defines if the distance should be calculated to the start node
     * @return Custom detector pair comparator
     */
    Comparator<AbstractDetectorGroup> getDetectorPairDistanceComparator(boolean startNode) {
        return new DetectorPairComparator(startNode);
    }

    /**
     * Loads incidents from a XML file.
     *
     * @param file the XML file
     * @return List of incidents contained in that file
     */
    public List<Incident> loadIncidentsFromFile(File file) {
        List<Incident> incidents = new ArrayList<>();

        try {
            IncidentList list = JAXB.unmarshal(file, IncidentList.class);
            incidents.addAll(list.getIncidents());
        } catch (Exception e) {
            System.err.println("Could not load incidents from file " + file.getAbsolutePath());
        }

        return incidents;
    }

    /**
     * This method returns an unique ID which is calculated with the node-id and an
     * internal counter.
     *
     * @param x id of the corresponding node
     * @return Unique id
     */
    int computeID(final int x) {
        final int y = counter++;
        return (x + y) * (x + y + 1) / 2 + y;
    }

    /**
     * Compares the position of two detectors.
     */
    private class DetectorComparator implements Comparator<Detector> {
        @Override
        public int compare(Detector d1, Detector d2) {
            if (d1.getPositionBegin() > d2.getPositionBegin()) {
                return 1;
            } else if (d1.getPositionBegin() < d2.getPositionBegin()) {
                return -1;
            }
            return 0;
        }
    }

    /**
     * Compares the distance of two DetectorPairs to their start (outgoing) or
     * end (incoming) node depending on the value of the {@code startNode}
     * parameter.
     */
    private class DetectorPairComparator implements Comparator<AbstractDetectorGroup> {
        /**
         * Defines whether the distance should be calculated to the start or to the end
         * node.
         */
        private final boolean startNode;

        DetectorPairComparator(boolean startNode) {
            this.startNode = startNode;
        }

        @Override
        public int compare(final AbstractDetectorGroup d1, final AbstractDetectorGroup d2) {
            if (startNode) {
                if (d1.getDistanceToStart() < d2.getDistanceToStart()) {
                    return 1;
                }
                return -1;
            } else {
                if (d1.getDistanceToEnd() > d2.getDistanceToEnd()) {
                    return 1;
                }
                return -1;
            }
        }
    }
}
