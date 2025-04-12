package de.dfg.oc.otc.routing;

import com.Ostermiller.util.BadDelimiterException;
import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVParser;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Centroid;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * CoordinateHelper is in charge of loading {@link AimsunJunction} and
 * {@link Centroid} coordinates data from CSV files.
 * <p>
 * Until now it is not possible to send coordinates from Aimsun to the
 * simulation. Therefore, this class provides coordinates.
 *
 * @author Johannes Lyda
 */
abstract class CoordinateHelper {
    private static final Logger log = Logger.getLogger(CoordinateHelper.class);
    private static final double MULTIPLICATOR = 250;

    /**
     * Loads the {@link Centroid} and {@link AimsunJunction} data and puts it
     * into the objects of the centroidMap and junctionMap of the network.
     */
    static void loadCoordinates() {
        final String networkName = OTCManager.getInstance().getNetwork().getName();
        final String path = DefaultParams.getPath() + "de/dfg/oc/otc/config/" + networkName;
        setCentroidCoordinates(OTCManager.getInstance().getNetwork().getCentroidMap(), path + "_centroidCoordinates.csv");
        setJunctionCoordinates(OTCManager.getInstance().getNetwork().getJunctions(), path + "_junctionCoordinates.csv");
    }

    /**
     * Uses the {@link Centroid} map and loads from a CSV file.
     *
     * @param centroidMap
     * @param path
     */
    private static void setCentroidCoordinates(final Map<Integer, Centroid> centroidMap, final String path) {
        log.info("loading centroid coordinates from " + path);
        final File data = new File(path);

        try {
            final CSVParse parser = new CSVParser(new FileInputStream(data), ';');
            final String[][] values = parser.getAllValues();

            for (String[] rows : values) {
                Point2D.Double coord = new Point2D.Double(Double.valueOf(rows[1]) * MULTIPLICATOR, Double.valueOf(rows[2])
                        * MULTIPLICATOR);

                Integer id = Integer.valueOf(rows[0]);
                Centroid centroid = centroidMap.get(id);
                if (centroid != null) {
                    centroid.setCoordinates(coord);
                    log.debug("Centroid attached " + centroid.getId() + ", " + centroid.getCoordinates());
                }
            }
        } catch (BadDelimiterException | IOException e) {
            log.warn(e.getMessage());
        }
    }

    /**
     * Uses the {@link AimsunJunction} map and loads from a CSV file.
     *
     * @param junctionMap
     * @param path
     */
    private static void setJunctionCoordinates(final Map<Integer, AimsunJunction> junctionMap, final String path) {
        log.info("loading junction coordinates from " + path);
        final File data = new File(path);

        try {
            final CSVParse parser = new CSVParser(new FileInputStream(data), ';');
            final String[][] values = parser.getAllValues();

            for (String[] rows : values) {
                Point2D.Double coord = new Point2D.Double(Double.valueOf(rows[1]) * MULTIPLICATOR, Double.valueOf(rows[2])
                        * MULTIPLICATOR);

                Integer id = new Integer(rows[0]);
                AimsunJunction junction = junctionMap.get(id);
                if (junction != null) {
                    junction.setCoordinates(coord);
                    log.debug("Junction attached " + junction.getId() + ", " + junction.getCoordinates());
                }
            }
        } catch (BadDelimiterException | IOException e) {
            log.warn(e.getMessage());
        }
    }
}
