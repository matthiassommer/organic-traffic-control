package de.dfg.oc.otc.routing;

import com.Ostermiller.util.BadDelimiterException;
import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVParser;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Centroid;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class to load regional information from a CSV file. Each {@link Centroid} and
 * each {@link de.dfg.oc.otc.manager.aimsun.AimsunJunction} is declared to be in a special region. The
 * {@link RegionalHelper} loads a mapping into special maps which can be
 * accessed by the {@link de.dfg.oc.otc.manager.OTCNode}s.
 *
 * @author lyda
 */
public final class RegionalHelper {
    private static final Logger log = Logger.getLogger(RegionalHelper.class);
    private static final RegionalHelper INSTANCE = new RegionalHelper();

    public static RegionalHelper getInstance() {
        return INSTANCE;
    }

    /**
     * Maps a region to a list of centroid IDs.
     */
    private final Map<Integer, List<Integer>> centroidRegionMapping;
    /**
     * Maps a region to a junction ID.
     */
    private final Map<Integer, Integer> junctionRegionMapping;

    private RegionalHelper() {
        this.centroidRegionMapping = new HashMap<>();
        this.junctionRegionMapping = new HashMap<>();
        loadRegionalMapping();
    }

    public Map<Integer, List<Integer>> getCentroidRegionMapping() {
        return centroidRegionMapping;
    }

    Map<Integer, Integer> getJunctionRegionMapping() {
        return junctionRegionMapping;
    }

    /**
     * Loading the Region->Centroid mapping from a CSV file.
     *
     * @param path to csv file
     */
    private void loadCentroidMapping(final String path) {
        final File data = new File(path + "_centroidRegionMapping.csv");

        try {
            final CSVParse parser = new CSVParser(new FileInputStream(data), ';');
            final String[][] values = parser.getAllValues();

            for (String[] rows : values) {
                final String rowString = rows[0];
                final Integer regionId = new Integer(rowString.substring(0, rowString.indexOf("-")));
                String centroidIds = rowString.substring(rowString.indexOf("-") + 1, rowString.length());

                // entries have the form
                // regionid;centroid1id,centroid2id,centroid3id
                centroidIds = centroidIds.replace("\"", "");
                String[] centroidIdArray = centroidIds.split(",");

                List<Integer> centroidIdList = new ArrayList<>();
                for (String centroidId : centroidIdArray) {
                    centroidIdList.add(new Integer(centroidId));
                }

                centroidRegionMapping.put(regionId, centroidIdList);
                log.info("Centroids " + centroidIds + " sorted into Region " + regionId);
            }
        } catch (NumberFormatException | BadDelimiterException | IOException e) {
            log.error(e.getMessage());

            // load all centroids of the network in the same region if file
            // could not be read
            Map<Integer, Centroid> centroids = OTCManager.getInstance().getNetwork().getCentroidMap();

            List<Integer> idList = centroids.values().stream().map(Centroid::getId).collect(Collectors.toList());
            centroidRegionMapping.put(1, idList);
        }
    }

    /**
     * Loading the junction->regionId mapping.
     *
     * @param path to CSV file
     */
    private void loadJunctionMapping(final String path) {
        final File data = new File(path + "_junctionRegionMapping.csv");

        try {
            final CSVParse parser = new CSVParser(new FileInputStream(data));
            final String[][] values = parser.getAllValues();

            for (String[] rows : values) {
                String entry = rows[0];
                String[] ids = entry.split(";");

                Integer junctionId = new Integer(ids[0]);
                Integer regionId = new Integer(ids[1]);
                this.junctionRegionMapping.put(junctionId, regionId);

                log.info("Junction " + junctionId + " sorted into Region " + regionId);
            }
        } catch (NumberFormatException | BadDelimiterException | IOException e) {
            log.error(e.getMessage());

            // load all junctions of the network in the same region if file
            // could not be read
            Map<Integer, AimsunJunction> junctions = OTCManager.getInstance().getNetwork().getJunctions();

            for (AimsunJunction junction : junctions.values()) {
                this.junctionRegionMapping.put(junction.getId(), 1);
            }
        }
    }

    /**
     * Load mapping for centroids and junctions.
     */
    private void loadRegionalMapping() {
        final String networkName = OTCManager.getInstance().getNetwork().getName();
        final String path = DefaultParams.getPath() + "de/dfg/oc/otc/config/" + networkName;
        loadCentroidMapping(path);
        loadJunctionMapping(path);
    }
}
