package de.dfg.oc.otc.manager.aimsun;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.ForecastAdapter;
import de.dfg.oc.otc.layer1.observer.monitoring.AbstractObservableStatistics;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import forecasting.DefaultForecastParameters;
import org.apache.commons.math3.util.FastMath;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A section consists of several lanes and can serve as a connection between
 * e.g. two junctions or a junction and a centroid.
 */
public class Section extends AbstractObservableStatistics {
    /**
     * List of neighbouring {@link Centroid}s.
     */
    private final List<Centroid> connectedCentroids;
    private final int[] destinationId;
    /**
     * List of direct connected outgoing sections.
     */
    private final List<Section> destinationSections;
    /**
     * List of detectors on this section.
     */
    private final List<Detector> detectors;
    /**
     * Unique ID of section.
     */
    private final int id;
    /**
     * Length in meter.
     */
    private final float length;
    private final int numberOfTurnings;
    private final List<Section> originSections;
    /**
     * Geschwindigkeitsbegrenzung in km/h für diese section.
     */
    private final float speedlimit;
    /**
     * Capacity of this section in veh/h.
     */
    private final float capacity;
    /**
     * Bildet die <b>Id</b> der Zielsection auf das zugeh rige {@link Turning}s
     * ab.
     */
    private final Map<Integer, Turning> turningMap;
    /**
     * Road type of section.
     */
    private final RoadType roadType;
    /**
     * Component which calculates the capacity of this section.
     */
    private final SectionCapacityComponent capacityComponent;
    /**
     * Component is adapter for enhancing sections with flow forecast abilities.
     */
    private ForecastAdapter flowForecaster;
    /**
     * True if this section comes from a {@link Centroid}.
     */
    private boolean isEntry;
    /**
     * True if this section ends in a {@link Centroid}.
     */
    private boolean isExit;
    /**
     * The {@link AimsunJunction} this sections ends.
     */
    private AimsunJunction nextJunction;
    /**
     * Number of lanes this section consists of.
     */
    private int numberOfLanes;
    /**
     * The {@link AimsunJunction} where this section starts.
     */
    private AimsunJunction previousJunction;
    /**
     * Last measured traffic flow in Aimsun.
     */
    private float flow = 0;

    /**
     * Creates a section object.
     *
     * @param angId         Section Identifier (ANG)
     * @param roadType      Id des zugehörigen {@link RoadType}
     * @param nbTurnings    Anzahl der {@link Turning}s, die von dieser Section ausgehen.
     *                      Ist dieser Wert 1, schlie t sich eine andere Section an, ist
     *                      er gr  er, schlie t sich ein Knotenpunkt (Junction) an.
     * @param destinationId Array mit den IDs der in Fahrtrichtung liegenden Sections (nur
     *                      Sections, die  ber {@link Turning}s erreichbar sind, werden
     *                      ber cksichtigt).
     * @param length        L nge der Section.
     */
    public Section(final int angId, final int roadType, final int nbTurnings, final int[] destinationId,
                   final float length, final float speedlimit, final float capacity) {
        this.id = angId;
        this.roadType = RoadType.getTypeForId(roadType);
        this.numberOfTurnings = nbTurnings;
        this.destinationId = destinationId.clone();
        this.length = length;
        this.speedlimit = speedlimit;
        this.capacity = capacity;
        this.detectors = new ArrayList<>(5);
        this.destinationSections = new ArrayList<>(3);
        this.originSections = new ArrayList<>(2);
        this.connectedCentroids = new ArrayList<>(2);
        this.turningMap = new HashMap<>(3);
        this.capacityComponent = new SectionCapacityComponent(this);

        if (DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE && !DefaultParams.ROUTING_PROTOCOL.equals("NONE")) {
            this.flowForecaster = new ForecastAdapter(60);
        }
    }

    public ForecastAdapter getFlowForecaster() {
        return this.flowForecaster;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String printStatisticalData() {
        return "";
    }

    public float getFlow() {
        return this.flow;
    }

    public void setFlow(float flow) {
        this.flow = flow;
    }

    /**
     * Return connected sinks (Centroids).
     */
    final void addConnectedSink(final Centroid centroid) {
        this.connectedCentroids.add(centroid);
    }

    final void addDestinationSection(final Section theSection) {
        this.destinationSections.add(theSection);
    }

    /**
     * Fügt der Section einen Detector hinzu.
     *
     * @param id        Id des Detectors
     * @param posBegin  Relative Position in der Section, an der der Detector-Anfang
     *                  liegt (0...1)
     * @param posEnd    Relative Position in der Section, an der das Detector-Ende
     *                  liegt (0...1)
     * @param firstLane Die erste Fahrspur der Section, die von diesem Detektor
     *                  erfasst wird.
     * @param lastLane  Die letzte Fahrspur der Section, die von diesem Detektor
     *                  erfasst wird.
     * @return new detector
     */
    final Detector addDetector(final int id, final float posBegin, final float posEnd, final int firstLane,
                               final int lastLane) {
        final Detector newDetector = new Detector(id, this.id, posBegin, posEnd, firstLane, lastLane);
        this.detectors.add(newDetector);
        return newDetector;
    }

    final void addOriginSection(final Section section) {
        originSections.add(section);
    }

    final void addTurning(final Turning turning) {
        turningMap.put(turning.getOutSection().id, turning);
    }

    /**
     * Pr ft, ob eine Section Netzzu- oder ausfahrt ist. Voraussetzung f r ein
     * sinnvolles Ergebnis ist, da  zuvor das Netz mittels initNetwork()
     * initialisiert wurde.
     */
    final void checkEntryExit() {
        if (destinationSections.isEmpty()) {
            this.isExit = true;
        }
        if (originSections.isEmpty()) {
            this.isEntry = true;
        }
    }

    public final List<Section> determineCompletePathToNextJunction(final Collection<Section> path) {
        final List<Section> newPath = new ArrayList<>();
        newPath.addAll(path);
        newPath.add(this);

        if (isJunctionApproach() || isExit) {
            if (newPath.isEmpty()) {
                OTCManager.getInstance().newInfo("determineCompletePathToNextJunction: newPath has no entries!");
            }

            final AimsunJunction origin = newPath.get(0).previousJunction;

            // Avoid paths leaving the network and paths to lead back to the
            // origin node
            if (!this.isExit && !origin.equals(this.nextJunction)) {
                // alles okay
                return newPath;
            } else {
                // Kreis gefunden.
                return newPath;
            }
        } else {
            Section candidate = null;
            float dist = -1;

            for (Section nextSection : destinationSections) {
                if (dist < 0) {
                    // Noch kein Kandidat gefunden!
                    candidate = nextSection;
                    dist = nextSection.length;
                } else if (nextSection.length < dist) {
                    // Besseren Kandidaten gefunden!
                    candidate = nextSection;
                    dist = nextSection.length;
                } else {
                    // Aktueller Kandidat ist nicht besser
                }
            }

            if (candidate != null) {
                return candidate.determineCompletePathToNextJunction(newPath);
            }
            return newPath;
        }
    }

    public final List<Section> determineCompletePathToPreviousJunction(final Collection<Section> path) {
        final List<Section> newPath = new ArrayList<>();
        newPath.addAll(path);
        newPath.add(this);

        if (isJunctionExit() || this.isEntry) {
            if (newPath.isEmpty()) {
                OTCManager.getInstance().newInfo("determineCompletePathFromNextJunction: newPath has no entries!");
            }

            final AimsunJunction destination = newPath.get(0).nextJunction;

            // Avoid paths leaving the network and paths to lead back to the origin node
            if (!this.isEntry && !destination.equals(previousJunction)) {
                // alles okay
                return newPath;
            } else {
                // Kreis gefunden.
                return newPath;
            }
        } else {
            Section candidate = null;
            float dist = -1;

            for (Section nextSection : originSections) {
                if (dist < 0) {
                    // Noch kein Kandidaten gefunden
                    candidate = nextSection;
                    dist = nextSection.length;
                } else if (nextSection.length < dist) {
                    // Besseren Kandidaten gefunden
                    candidate = nextSection;
                    dist = nextSection.length;
                } else {
                    // Aktueller Kandidat ist nicht besser
                }
            }

            if (candidate != null) {
                return candidate.determineCompletePathToPreviousJunction(newPath);
            }
            return newPath;
        }
    }

    /**
     * Collects all {@code OTCNode}-IDs of those nodes, which can be found
     * by following the outgoing {@code Sections} to their destination.
     *
     * @param path  The path of sections followed already
     * @param nodes The OTCNode-IDs found already (-1 if there is a centroid
     *              instead of a junction)
     * @return The OTCNode-IDs of all nodes found
     */
    public final List<Integer> determineReceivingNodes(final List<Section> path, final List<Integer> nodes) {
        final List<Integer> foundNodes = new ArrayList<>(nodes);
        path.add(this);

        if (isJunctionApproach() || this.isExit) {
            // Am Nachfolger angekommen
            if (isJunctionApproach()) {
                // Nachfolger ist Knoten
                foundNodes.add(nextJunction.getNode().getId());
            } else {
                // Nachfolger ist Centroid
                foundNodes.add(-1);
            }
        } else {
            // Noch nicht am Ziel angekommen: rekursiver Aufruf
            for (Section section : this.destinationSections) {
                List<Integer> receivingNodes = section.determineReceivingNodes(path, nodes);
                if (!receivingNodes.isEmpty()) {
                    // Rückgabe ist gültig, übertrage alle Einträge
                    foundNodes.addAll(receivingNodes);
                }
            }
        }

        return foundNodes;
    }

    /**
     * Collects all {@code OTCNode}-IDs of those nodes, which can be found
     * by following the incoming {@code Sections} to their origin.
     *
     * @param path  The path of sections followed already
     * @param nodes The OTCNode-IDs found already (-1 if there is a centroid
     *              instead of a junction)
     * @return The OTCNode-IDs of all nodes found
     */
    public final List<Integer> determineSendingNodes(final List<Section> path, final List<Integer> nodes) {
        final List<Integer> foundNodes = new ArrayList<>(nodes);
        path.add(this);

        if (isJunctionExit() || this.isEntry) {
            // Am Sender angekommen
            if (isJunctionExit()) {
                foundNodes.add(previousJunction.getNode().getId());
            } else {
                foundNodes.add(-1);
            }
        } else {
            // Noch nicht am Ziel angekommen: rekursiver Aufruf
            for (Section section : this.originSections) {
                List<Integer> sendingNodes = section.determineSendingNodes(path, nodes);
                if (!sendingNodes.isEmpty()) {
                    // R ckgabe ist g ltig,  bertrage alle Eintr ge
                    foundNodes.addAll(sendingNodes);
                }
            }
        }

        return foundNodes;
    }

    /**
     * Finds all paths that start in this section and end at a
     * {@link AimsunJunction}. This is a recursive method which should only be
     * called for out-sections of a {@link AimsunJunction} serving as origin.
     * When a neighbouring {@link AimsunJunction} is found, the corresponding
     * path is announced to the origin {@link AimsunJunction}.
     *
     * @param path Please start with an empty {@code List<Section>} !
     * @return needed only in recursion
     */
    public final List<Section> findPathsToNextJunction(List<Section> path) {
        path.add(this);

        if (isJunctionApproach() || isExit) {
            final AimsunJunction origin = path.get(0).previousJunction;

            // Avoid paths leaving the network and paths to lead back to the origin node
            if (!this.isExit && !origin.equals(this.nextJunction)) {
                // Clone path, otherwise it would be modified (by reference)
                final List<Section> clonedPath = path.stream().map(section -> section).collect(Collectors.toList());
                // Inform origin node
                origin.calculateOffset(clonedPath);
            } else if (this.isExit) {
                final List<Section> clonedPath = path.stream().map(section -> section).collect(Collectors.toList());

                // Inform origin node about sink
                origin.receiveLocalCentroid(clonedPath);
            }
        } else {
            for (Section nextSection : destinationSections) {
                if (!path.contains(nextSection)) {
                    path = nextSection.findPathsToNextJunction(path);

                    for (int i = path.indexOf(this) + 1; i < path.size(); i++) {
                        path.remove(i);
                    }
                }
            }
        }
        return path;
    }

    /**
     * Return connected sinks ({@link Centroid}s).
     *
     * @return list of {@link Centroid}s
     */
    public final List<Centroid> getConnectedCentroids() {
        return this.connectedCentroids;
    }

    public final String getDescription(final boolean includeDetectorValues) {
        final DecimalFormat formatter = new DecimalFormat("#.##");
        String output = "<h1>Section " + id + "</h1><table><tr><td>Length: </td><td><b>" + formatter.format(length)
                + "m </b></td></tr><tr><td>Number of turnings: </td><td><b>" + numberOfTurnings
                + "</b></td></tr><tr><td>Number of lanes: </td><td><b>" + numberOfLanes
                + "</b></td></tr><tr><td>Destination sections: </td><td><b>";

        for (int i = 0; i < numberOfTurnings; i++) {
            output = output.concat(destinationId[i] + " ");
        }
        output = output.concat("</b></td></tr><tr><td>Number of detectors: </td><td><b>" + getNumberOfDetectors()
                + "</b></td></tr></table>");

        if (this.isExit) {
            output = output.concat("<br>Section ends at a centroid");
        }

        if (this.isEntry) {
            output = output.concat("<br>Section starts at a centroid");
        }

        output = output.concat("<br>Section roadType: " + roadType.name());

        if (this.nextJunction != null) {
            output = output.concat("<br>Section is approach for junction " + nextJunction.getId());
        }

        if (this.previousJunction != null) {
            output = output.concat("<br>Section is exit for junction " + previousJunction.getId());
        }

        if (!this.connectedCentroids.isEmpty()) {
            output = output.concat("<br>Section is connected to centroids ");
            for (Centroid c : connectedCentroids) {
                output = output.concat(c.getId() + " ");
            }
        }

        for (Detector detector : this.detectors) {
            output = output.concat(detector.getSimpleDescription(includeDetectorValues));
        }

        return output;
    }

    final int[] getDestinationIds() {
        return this.destinationId;
    }

    /**
     * Gibt die erste Ziel-Section zurück. Ist primär für Sections gedacht, die
     * nicht zu einem Knoten führen und somit nur eine Ziel-Section haben.
     *
     * @return Erste Ziel-Section dieser Section.
     */
    public final Section getDestinationSection() {
        if (!this.destinationSections.isEmpty()) {
            return this.destinationSections.get(0);
        }
        throw new OTCManagerException("Section has no destinationSections");
    }

    public final List<Section> getDestinationSections() {
        return this.destinationSections;
    }

    public final List<Detector> getDetectors() {
        return this.detectors;
    }

    public final float getLength() {
        return this.length;
    }

    final int getNumberOfDetectors() {
        return this.detectors.size();
    }

    public final int getNumberOfLanes() {
        return this.numberOfLanes;
    }

    public final void setNumberOfLanes(final int lanes) {
        this.numberOfLanes = lanes;
    }

    /**
     * Falls diese Section Approach für eine {@link AimsunJunction} ist, wird
     * diese zurückgegeben, sonst null.
     *
     * @return Nächste {@link AimsunJunction} wenn Approach, sonst null;
     */
    public final AimsunJunction getNextJunction() {
        return this.nextJunction;
    }

    final void setNextJunction(final AimsunJunction junction) {
        this.nextJunction = junction;
    }

    public final int getNumDestinationSections() {
        return this.destinationSections.size();
    }

    final int getNumOriginSections() {
        return this.originSections.size();
    }

    /**
     * @return an {@code List} containing the source sections of this
     * section.
     */
    public final Iterable<Section> getOriginSections() {
        return this.originSections;
    }

    public final AimsunJunction getPreviousJunction() {
        return this.previousJunction;
    }

    final void setPreviousJunction(final AimsunJunction junction) {
        this.previousJunction = junction;
    }

    /**
     * Returns the current dynamically estimated capacity of this section.
     */
    private float getDynamicEstimatedCapacity() {
        return this.capacityComponent.getCapacity();
    }

    /**
     * Calculate the delay for this section and a given traffic flow according to the Bureau of Public Roads (BPR) formula.
     * https://www.fhwa.dot.gov/planning/tmip/publications/other_reports/delay_volume_relations/ch04.cfm
     *
     * @param flow of traffic in veh/h
     * @return delay
     */
    public final float calculateDelay(final float flow) {
        float capacity = getDynamicEstimatedCapacity();
        float freeFlowTravelTime = this.length / (this.speedlimit * 1000 / 3600);
        final float a = 0.7f;
        final int b = 3;

        return (float) (freeFlowTravelTime * (1 + a * FastMath.pow(flow / capacity, b)));
    }

    /**
     * Returns the default capacity of this section (which is set in Aimsun).
     */
    final float getBaseCapacity() {
        return this.capacity;
    }

    public final float getSpeedlimit() {
        return this.speedlimit;
    }

    public final Map<Integer, Turning> getTurningMap() {
        return turningMap;
    }

    public final RoadType getRoadType() {
        return this.roadType;
    }

    /**
     * Gibt {@code true} zurück, wenn die Section Zufahrt des Netzes ist,
     * also über diese Section Fahrzeuge von außen in das Netz eingespeist
     * werden.
     *
     * @return {@code true} wenn Zufahrt, sonst {@code false}
     */
    public final boolean isEntry() {
        return this.isEntry;
    }

    /**
     * Gibt {@code true} zurück, wenn die Section Ausfahrt des Netzes ist,
     * also über diese Section Fahrzeuge das Netz nach außen verlassen.
     *
     * @return {@code true} wenn Ausfahrt, sonst {@code false}
     */
    public final boolean isExit() {
        return this.isExit;
    }

    /**
     * Gibt {@code true} zurück, wenn diese Section ein Fußweg ist, sonst
     * False.
     *
     * @return {@code true} wenn Fußweg, sonst {@code false}.
     */
    final boolean isFootpath() {
        return this.roadType == RoadType.FOOTPATH;
    }

    /**
     * Gibt {@code true} zurück, wenn diese Section eine Interne Section
     * einer Junction ist, sonst {@code false}.
     *
     * @return {@code true} wenn interne Section, sonst {@code false}.
     */
    final boolean isInternalSection() {
        return this.roadType == RoadType.INTERNAL;
    }

    /**
     * Gibt {@code true} zurück, wenn die Section direkte Zufahrt einer
     * Junction ist.
     *
     * @return {@code true} wenn Approach, sonst {@code false}
     */
    public final boolean isJunctionApproach() {
        return this.nextJunction != null;
    }

    /**
     * Gibt True zurück, wenn die Section direkte Ausfahrt einer Junction ist.
     *
     * @return {@code true} wenn Ausfahrt, sonst {@code false}
     */
    public final boolean isJunctionExit() {
        return this.previousJunction != null;
    }

    public final String toString() {
        final String linesep = System.getProperty("line.separator");
        String output = "Section " + id + " (" + length + "m, " + numberOfLanes + " lanes), " + numberOfTurnings
                + " turnings:" + linesep;

        for (int i = 0; i < numberOfTurnings; i++) {
            output = output.concat("Turning " + (i + 1) + " to " + destinationId[i] + linesep);
        }

        output = output.concat(getNumberOfDetectors() + " detectors" + linesep);
        output = output.concat(roadType.name() + " section" + linesep);
        return output;
    }
}