package de.dfg.oc.otc.manager.aimsun;

import de.dfg.oc.otc.layer0.tlc.AbstractTLC;
import de.dfg.oc.otc.layer0.tlc.Recall;
import de.dfg.oc.otc.layer0.tlc.fixedTimeController.FixedTimeController;
import de.dfg.oc.otc.layer0.tlc.fixedTimeController.FixedTimeRecallController;
import de.dfg.oc.otc.layer0.tlc.nemaController.NEMAController;
import de.dfg.oc.otc.layer2.TurningData;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import de.dfg.oc.otc.region.OTCNodeSynchronized;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Describes a junction in the way it is desribed in AIMSUN: the active TLC, its
 * phases, signal groups, neighbouring nodes, etc.
 */
public class AimsunJunction implements Serializable {
    /**
     * List of all turnings of this junction.
     */
    private final List<Turning> allTurnings;
    /**
     * List of ingoing sections of this junction.
     */
    private final List<Section> inSections;
    /**
     * Maps the Centroid-ID to an offset object.
     */
    private final Map<Integer, Offset> localCentroidOffsets = new HashMap<>(3);
    /**
     * Maps the ID of the directly connecting centroids to the Centroid object.
     */
    private final Map<Integer, Centroid> localCentroids = new HashMap<>(3);
    /**
     * Nur Turnings, die von motorisierten Fahrzeugen genutzt werden (die also
     * Sections verbinden, die nicht als Footpath gekennzeichnet sind).
     */
    private final List<Turning> motorisedVehiclesTurnings;
    /**
     * Enth�lt die Nachbarknoten für diese {@code AimsunJunction}. Die ID
     * der Nachbarknoten wird durch die {@code HashMap} auf das zugehörige
     * {@code OTCNode}-Objekt abgebildet.
     */
    private final Map<Integer, OTCNode> neighbouringNodes = new HashMap<>(5);
    /**
     * Maps the ids of neighbouring nodes to {@code Offset}-objects.
     * The delay for the whole path from this junction to a neighboring one.
     */
    private final Map<Integer, List<Offset>> offsets = new HashMap<>(5);
    /**
     * List of outgoing sections of this junction.
     */
    private final List<Section> outSections;
    /**
     * Nur Turnings, die Fußgängerwege darstellen.
     */
    private final List<Turning> pedestrianTurnings;
    /**
     * List of phases of the current TLC.
     */
    private final List<Phase> phases;
    /**
     * Nur Turnings, die ausschließlich vom öffentlichen Nahverkehr genutzt
     * werden.
     */
    private final List<Turning> publicTransportTurnings;
    private final List<SignalGroup> signalGroups;
    /**
     * The currently activated TLC.
     */
    private AbstractTLC activeTLC;
    /**
     * Flag if junction is controlled by OTC.
     */
    private boolean controlled;
    /**
     * coordinates in raster system.
     */
    private Point2D.Double coordinates;
    private int id = -1;
    /**
     * Name of this junction from Aimsun.
     */
    private String name = "";
    /**
     * The associated node to this junction.
     */
    private OTCNode node;
    /**
     * Number of phases of the current TLC.
     */
    private int numberPhases;
    /**
     * Bildet die Spuren aller Zufahrten auf eine Liste von Turnings ab, die von
     * der Spur ausgehen. Spuren werden dabei durch einen String der Form
     * "sectionID;laneID" gekennzeichnet.
     */
    private Map<String, List<Turning>> sharedLanesMap;

    /**
     * Create a new junction.
     *
     * @param id          of the junction
     * @param controlType if 2 or 3 than junction is OTC-controlled, otherwise not.
     * @param name        of the junction or empty
     */
    public AimsunJunction(final int id, final int controlType, final String name) {
        this.id = id;

        if (!name.isEmpty()) {
            this.name = " (" + name + ")";
        }

        this.inSections = new ArrayList<>(5);
        this.outSections = new ArrayList<>(5);
        this.phases = new ArrayList<>(5);
        this.signalGroups = new ArrayList<>(5);
        this.allTurnings = new ArrayList<>();
        this.motorisedVehiclesTurnings = new ArrayList<>(5);
        this.publicTransportTurnings = new ArrayList<>(5);
        this.pedestrianTurnings = new ArrayList<>(5);

        if (controlType == 2 || controlType == 3) {
            this.controlled = true;
        }
    }

    /**
     * F�gt eine neue Phase zur Junction hinzu. Diese Phase repr�sentiert eine
     * in Aimsun definierte Phase.
     *
     * @param id         Id der Phase
     * @param interphase Legt fest, ob die Phase eine Interphase/Zwischenphase ist
     *                   (true) oder nicht (false).
     * @throws OTCManagerException Wenn das neue Phasenobjekt nicht angelegt werden konnte.
     */
    final void addPhase(final int id, final boolean interphase) throws OTCManagerException {
        final Phase newPhase = new Phase(id, interphase);
        this.phases.add(newPhase);
        this.numberPhases++;
    }

    /**
     * F�gt eine neue Phase zur Junction hinzu. Diese Phase repr�sentiert eine
     * in Aimsun definierte Phase.
     *
     * @param id          Id der Phase
     * @param interphase  Legt fest, ob die Phase eine Interphase/Zwischenphase ist
     *                    (true) oder nicht (false).
     * @param duration    Normale Phasendauer.
     * @param maxDuration Maximale Phasendauer.
     * @param minDuration Minimale Phasendauer.
     * @throws OTCManagerException Wenn das neue Phasenobjekt nicht angelegt werden konnte.
     */
    final void addPhase(final int id, final boolean interphase, final float duration,
                        final float maxDuration, final float minDuration) throws OTCManagerException {
        final Phase newPhase = new Phase(id, interphase, duration, maxDuration);
        this.phases.add(newPhase);
        this.numberPhases++;
    }

    final void addSignalGroup(final int id) throws OTCManagerException {
        final SignalGroup newSignalGrp = new SignalGroup(id);
        this.signalGroups.add(newSignalGrp);
    }

    /**
     * F�gt einer Phase eine Signalgruppe hinzu.
     *
     * @param idSignalGrp Id der SignalGroup
     * @param idPhase     Id der Phase, zu der die SignalGroup hinzugef�gt werden soll.
     * @throws OTCManagerException wenn SignalGroup oder Phase nicht existieren.
     */
    final void addSignalGrpPhase(final int idSignalGrp, final int idPhase) throws OTCManagerException {
        /*
         * Die SignalGroups sind von 1 bis NumSignalGroups durchnummeriert,
		 * daher ist ein Zugriff �ber die fortlaufende Nummer direkt auf das
		 * Array hier kein Problem (sonst m�sste hier auch eine HashMap zum
		 * Einsatz kommen). Entsprechendes gilt nicht(!) f�r die Phases, da
		 * Interphases ausgefiltert werden.
		 */
        final SignalGroup signalGrp = signalGroups.get(idSignalGrp - 1);
        if (signalGrp == null) {
            throw new OTCManagerException("SignalGroup " + idSignalGrp + " does not exist.");
        }

        final Phase phase = getPhaseById(idPhase);
        phase.addSignalGroup(signalGrp);
    }

    /**
     * F�gt der Junction ein Turning hinzu. Die angegebene SignalGroup muss
     * bereits existieren.
     *
     * @param id            Id des Turnings.
     * @param signalGroupID Id der SignalGroup, zu der dieses Turning hinzugef�gt werden
     *                      soll.
     * @param sectionIn     Id der Section, die Ausgangspunkt des Turnings ist.
     * @param sectionOut    Id der Section, die Endpunkt des Turnings ist.
     * @throws OTCManagerException wenn die SignalGroup nicht existiert oder das Turning nicht
     *                             angelegt werden kann (Speicherproblem).
     */
    public final void addTurning(final int id, final int signalGroupID, final Section sectionIn,
                                 final Section sectionOut) throws OTCManagerException {
        /*
         * Die SignalGroups sind von 1 bis NumSignalGroups durchnummeriert,
		 * daher ist ein Zugriff über die fortlaufende Nummer direkt auf das
		 * Array hier kein Problem.
		 */
        final SignalGroup signalGroup = signalGroups.get(signalGroupID - 1);
        if (signalGroup == null) {
            throw new OTCManagerException("SignalGroup " + signalGroupID + " does not exist");
        }

        final Turning newTurning = new Turning(id, sectionIn, sectionOut);
        signalGroup.addTurning(newTurning);

        if (newTurning.getRoadType() == RoadType.FOOTPATH) {
            this.pedestrianTurnings.add(newTurning);
            newTurning.setTrafficType(TrafficType.PEDESTRIANS);
        } else if (newTurning.getRoadType() == RoadType.PUBLICTRANSPORT) {
            this.publicTransportTurnings.add(newTurning);
            newTurning.setTrafficType(TrafficType.PUBLIC_TRANSPORT);
        } else {
            this.motorisedVehiclesTurnings.add(newTurning);
            newTurning.setTrafficType(TrafficType.INDIVIDUAL_TRAFFIC);
        }

        this.allTurnings.add(newTurning);

        if (!inSections.contains(sectionIn)) {
            this.inSections.add(sectionIn);
            sectionIn.setNextJunction(this);
        }
        if (!outSections.contains(sectionOut)) {
            this.outSections.add(sectionOut);
            sectionOut.setPreviousJunction(this);
        }
    }

    /**
     * Calculates the (approximate) time in seconds needed to pass the list of
     * sections given as parameter. The calculation is based on the section's
     * length and their speed limits.
     *
     * @param path a list of sections
     */
    final void calculateOffset(final List<Section> path) {
        // Add 2.5s to pass inital node
        float offset = 2.5f;
        for (Section section : path) {
            offset += section.getLength() / (section.getSpeedlimit() * 1000 / 3600);
        }

        // Add 1s for each connection between intermediate sections
        final int size = path.size() - 1;
        offset += size;

        // Round to full seconds
        final int newOffset = Math.round(offset);
        final AimsunJunction prevJunction = path.get(0).getPreviousJunction();
        final AimsunJunction nextJunction = path.get(size).getNextJunction();
        final Offset offsetObj = new Offset(prevJunction, nextJunction, path, newOffset);

        this.neighbouringNodes.putIfAbsent(nextJunction.getNode().getId(), nextJunction.getNode());

        // Add path to neighbour and calculated offset for path to map
        if (!offsets.containsKey(nextJunction.id)) {
            this.offsets.put(nextJunction.id, new ArrayList<>(Collections.singletonList(offsetObj)));
        } else {
            // Several paths with same first and last section are possible: Keep only shortest!
            Section firstSection = path.get(0);
            Section lastSection = path.get(size);

            List<Offset> offsets = this.offsets.get(nextJunction.id);
            for (Offset oldOffset : offsets) {
                List<Section> oldPath = oldOffset.getPath();
                boolean isFirstIdentical = oldPath.get(0) == firstSection;
                boolean isLastIdentical = oldPath.get(oldPath.size() - 1) == lastSection;
                boolean isNewPathFaster = oldOffset.getOffset() > newOffset;

                if (isFirstIdentical && isLastIdentical && isNewPathFaster) {
                    oldOffset.setOffset(newOffset);
                    oldOffset.setPath(path);
                    return;
                }
            }

            offsets.add(offsetObj);
        }
    }

    /**
     * Erzeugt eine Map, die Spuren aller Zufahrten auf eine Liste von
     * Turnings abbildet, die von der Spur ausgehen. Spuren werden dabei durch
     * einen String der Form "sectionID;laneID" gekennzeichnet.
     *
     * @see #sharedLanesMap
     */
    private void createSharedLanesMap() {
        sharedLanesMap = new HashMap<>();

        for (Section section : this.inSections) {
            // Leere Datenstruktur anlegen
            for (int i = 1; i <= section.getNumberOfLanes(); i++) {
                sharedLanesMap.put(section.getId() + ";" + i, new ArrayList<>());
            }

            // mit Inhalt füllen
            List<Turning> turnings = getTurningsForIncomingSectionID(section.getId());
            for (Turning turning : turnings) {
                for (int lane = turning.getFirstLaneOrigin(); lane <= turning.getLastLaneOrigin(); lane++) {
                    List<Turning> turnsForLane = sharedLanesMap.get(section.getId() + ";" + lane);
                    turnsForLane.add(turning);
                }
            }
        }
    }

    /**
     * Determines the neighbouring nodes for this junction. This method needs to
     * be called only once since the neighbourhoods do not change. Use
     * {@code getNeighbouringNodes()} to obtain the neighbours of this
     * junction. Find neighbouring junction (start from outsection).
     *
     * @see #getNeighbouringNodes()
     */
    private void determineNeighbouringNodesAndOffsets() {
        // Skip internal sections and footpaths
        outSections.stream().filter(outSection -> !outSection.isInternalSection() && !outSection.isFootpath()).forEach(outSection -> outSection.findPathsToNextJunction(new ArrayList<>()));
    }

    /**
     * Returns a HashMap of data sets representing the intersection's turnings.
     * Within the map, a turning is identifed by a string formed from its
     * incoming section id and its outgoing section id.
     *
     * @return a HashMap of data sets representing the intersection's turnings
     */
    public final Map<String, TurningData> exportTurningData() {
        node.updateTurnToFlowMap();

        // Determine: Turning is part of several phases
        final Map<String, TurningData> turningDataMap = new HashMap<>();
        final List<Turning> turningsAlreadyHandled = new ArrayList<>();

        for (Phase phase : this.phases) {
            List<SignalGroup> signalGroups = phase.getSignalGroups();

            // Interphase with "all red"?
            for (SignalGroup signalgroup : signalGroups) {
                List<Turning> turnings = signalgroup.getTurnings();

                for (Turning turning : turnings) {
                    if (!turningsAlreadyHandled.contains(turning)) {
                        // Filter turnings for pedestrians as there is no TurningData available for Layer 2
                        if (turning.getTrafficType() == TrafficType.PEDESTRIANS) {
                            break;
                        }

                        String turningId = turning.getInSection().getId() + ";" + turning.getOutSection().getId();

                        // Turning has not yet been handled
                        if (turningDataMap.containsKey(turningId)) {
                            // Add phase for known turning
                            TurningData turningData = turningDataMap.get(turningId);
                            turningData.addPhase(phase.getId());
                        } else {
                            List<Turning> sharingTurns = getTurnsSharingLanes(turning);

                            // Create new turning with shared lanes
                            turningsAlreadyHandled.addAll(sharingTurns);

                            // Lanes
                            int minLane = turning.getFirstLaneOrigin();
                            int maxLane = turning.getLastLaneOrigin();

                            float totalFlow = node.getFlowForTurning(turning);

                            for (Turning otherTurn : sharingTurns) {
                                totalFlow += node.getFlowForTurning(otherTurn);

                                minLane = Math.min(otherTurn.getFirstLaneOrigin(), minLane);
                                maxLane = Math.max(otherTurn.getLastLaneOrigin(), maxLane);
                            }

                            TurningData turningData = new TurningData(turning.getInSection().getId(), turning.getOutSection()
                                    .getId(), maxLane - minLane + 1, totalFlow);

                            if (!sharingTurns.isEmpty()) {
                                turningData.setShared(true);
                            }

                            turningData.addPhase(phase.getId());
                            turningDataMap.put(turningId, turningData);
                        }
                    }
                }
            }
        }

        return turningDataMap;
    }

    /**
     * Erstellt einen FixedTimeController. Die Phasendauern entsprechen den in
     * Aimsun definierten Dauern.
     */
    public final void generateDefaultFTC() {
        activeTLC = new FixedTimeController(getMaxGreens(), this);
    }

    /**
     * Erstellt einen FixedTimeRecallController. Die Phasendauern entsprechen
     * den in Aimsun definierten Dauern. Weiterhin werden jeder Phase alle
     * verf�gbaren Presence-Detektoren als Recall-Detektoren zugewiesen und alle
     * Phasen, die �ber einen Presence Detector verf�gen, auf "no recall"
     * gesetzt, Phasen ohne Detector auf "max recall".
     */
    public final void generateDefaultFTRC() {
        final Recall[] recalls = new Recall[numberPhases];

        for (int i = 0; i < numberPhases; i++) {
            if (phases.get(i).getNumRecallDetectors() > 0) {
                recalls[i] = Recall.no;
            } else {
                recalls[i] = Recall.max;
            }
        }

        activeTLC = new FixedTimeRecallController(getMaxGreens(), this, recalls);
    }

    /**
     * Erstellt einen NemaController. Die Phasendauern entsprechen den in Aimsun
     * definierten Dauern. Weiterhin werden jeder Phase alle verf�gbaren
     * Presence-Detektoren als Recall-Detektoren zugewiesen und alle Phasen, die
     * �ber einen Presence Detector verf�gen, auf "no recall" gesetzt, Phasen
     * ohne Detector auf "max recall".
     */
    public final void generateDefaultNEMA() {
        final float[] maxGreens = new float[numberPhases];
        final float[] maxInits = new float[numberPhases];
        final float[] maxGaps = new float[numberPhases];
        final float[] minGreens = new float[numberPhases];
        final float[] extSteps = new float[numberPhases];
        final float[] reductionDelays = new float[numberPhases];
        final Recall[] recalls = new Recall[numberPhases];

        for (int i = 0; i < numberPhases; i++) {
            maxGreens[i] = phases.get(i).getDefaultMaximalDuration();
            minGreens[i] = phases.get(i).getAbsoluteMinDuration();
            extSteps[i] = phases.get(i).getDefaultExtensionStep();
            maxInits[i] = phases.get(i).getDefaultMaximalDuration() / 2;
            // TODO Parameter setzen: Werte
            reductionDelays[i] = phases.get(i).getDefaultMaximalDuration() / 6;
            maxGaps[i] = phases.get(i).getDefaultMaximalDuration();

            if (phases.get(i).getNumRecallDetectors() > 0) {
                recalls[i] = Recall.no;
            } else {
                recalls[i] = Recall.max;
            }
        }

        activeTLC = new NEMAController(maxGreens, this, recalls, maxInits, maxGaps, minGreens, extSteps,
                reductionDelays);
    }

    /**
     * Gibt den dieser Kreuzung zugeordneten TLC zur�ck.
     *
     * @return Traffic Light Controller-Objekt.
     * @throws OTCManagerException wenn kein TLC definiert ist.
     */
    public final AbstractTLC getActiveTLC() throws OTCManagerException {
        if (this.activeTLC == null) {
            throw new OTCManagerException("No TLC for junction " + id + " defined.");
        }
        return this.activeTLC;
    }

    public final Point2D.Double getCoordinates() {
        return this.coordinates;
    }

    /**
     * Set the loaded coordinates for this junction.
     *
     * @param coordinates of the junction in the network
     */
    public final void setCoordinates(final Point2D.Double coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Gibt die aktuelle Phase des dieser Junction zugeordneten TLCs zur�ck.
     *
     * @return Aktuelle Phase
     * @throws OTCManagerException wenn kein TLC definiert ist.
     */
    public final int getCurrentPhase() throws OTCManagerException {
        if (this.activeTLC == null) {
            throw new OTCManagerException("No TLC for junction " + id + " defined.");
        }
        return this.activeTLC.getCurrentPhaseID();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * de.dfg.oc.otc.manager.ObservableNetworkObjectDetectors#getDescription()
     */
    public final String getDescription() {
        final String isControlled = controlled ? "" : " (uncontrolled)";
        String output = "<h1>Junction " + id + name + isControlled
                + "</h1> <table><tr><td>Number of Turnings: </td><td><b>" + allTurnings.size()
                + "</b></td></tr><tr><td>Number of Phases: </td><td><b>" + numberPhases
                + "</b></td></tr><tr><td>Number of SignalGroups: </td><td><b>" + signalGroups.size()
                + "</b></td></tr></table>";
        output = output.concat(inSections.size() + " InSections and " + outSections.size() + " OutSections");

        output = output.concat("<h2>Regional information</h2>");
        String coords = "none";
        if (this.coordinates != null) {
            coords = this.coordinates.toString();
        }
        output = output.concat("Coordinates: " + coords + "<br>");

        output = output.concat("<h2>Turnings</h2>");
        output = output
                .concat("<table><tr><th>Id</th><th>From</th><th>To</th><th>Traffic Type</th><th>Relevant Detectors</th></tr>");
        for (Turning theTurning : allTurnings) {
            output = output.concat(theTurning.getDescription());
        }
        output = output.concat("</table>");

        output = output.concat("<h2>SignalGroups</h2>");
        for (SignalGroup signalGroup : signalGroups) {
            output = output.concat(signalGroup.getSimpleDescription());
        }

        output = output.concat("<h2>Phases</h2>");
        for (Phase phase : phases) {
            output = output.concat(phase.getSimpleDescription());
        }

        output = output.concat("<h2>Neighbouring nodes</h2>");
        for (OTCNode neighbour : this.neighbouringNodes.values()) {
            output = output.concat("<b>ID " + neighbour.getId() + "</b><br>");

            List<Offset> paths = offsets.get(neighbour.getId());

            for (Offset offsetObj : paths) {
                output = output.concat(offsetObj + "<br>");
            }
        }
        return output;
    }

    /**
     * Gibt ein Array mit allen Detektoren zur�ck, die mindestens einem Turning
     * dieser Junction zugeordnet sind.
     *
     * @return Array der Detektoren.
     */
    public final List<Detector> getDetectors() {
        final List<Detector> detectorList = new ArrayList<>();

        for (Turning turning : allTurnings) {
            detectorList.addAll(turning.getDetectors().stream().filter(detector -> !detectorList.contains(detector)).map(detector -> detector).collect(Collectors.toList()));
        }

        return detectorList;
    }

    /**
     * Returns the summed green time (phase and interphases) for the given
     * signal group.
     *
     * @param s a signal group
     * @return the summed green time for the given signal group
     */
    public final int getGreenTimeForSignalGroup(final SignalGroup s) {
        int greenTime = 0;

        for (Phase phase : phases) {
            List<SignalGroup> signalGroups = phase.getSignalGroups();

            for (SignalGroup signalGroup : signalGroups) {
                if (signalGroup.equals(s)) {
                    greenTime += getActiveTLC().getParameters().getGreenTimes()[phase.getId() - 1];
                }
            }
        }

        return greenTime;
    }

    public final int getId() {
        return this.id;
    }

    /**
     * Zugriff auf die von einer Junction hinf�hrenden Sections.
     *
     * @return Hinf�hrende Sections.
     */
    public final List<Section> getInSections() {
        return this.inSections;
    }

    /**
     * Get all IDs of all direct connected centroids.
     *
     * @return list of centroid IDs
     */
    public final Set<Integer> getLocalCentroidIDs() {
        return this.localCentroids.keySet();
    }

    private float[] getMaxGreens() {
        final float[] maxGreens = new float[numberPhases];

        for (int i = 0; i < numberPhases; i++) {
            maxGreens[i] = phases.get(i).getDefaultMaximalDuration();
        }
        return maxGreens;
    }

    public final String getName() {
        return this.name;
    }

    /**
     * Returns the number of lanes assigned to a signal group.
     *
     * @param signalGroup a signal group
     * @return the number of lanes assigned to the signal group
     */
    public final int getNbOfLanesForSignalGroup(final SignalGroup signalGroup) {
        final List<Turning> turns = signalGroup.getTurnings();

        int minLane = Integer.MAX_VALUE;
        int maxLane = Integer.MIN_VALUE;

        for (Turning turning : turns) {
            minLane = Math.min(turning.getFirstLaneOrigin(), minLane);
            maxLane = Math.max(turning.getLastLaneOrigin(), maxLane);
        }

        return maxLane - minLane + 1;
    }

    /**
     * Returns the neighbouring nodes for this junction. This method is a
     * wrapper for {@code determineNeighbouringNodesAndOffsets()}.
     *
     * @return a {@code Map} mapping the node ids of the neighbours to the
     * corresponding {@code OTCNode}s
     * @see #determineNeighbouringNodesAndOffsets()
     */
    public final Map<Integer, OTCNode> getNeighbouringNodes() {
        if (this.neighbouringNodes.isEmpty()) {
            determineNeighbouringNodesAndOffsets();
        }
        return this.neighbouringNodes;
    }

    /**
     * Liefert den dieser Junction zugeordneten Node.
     *
     * @return den Node.
     * @throws OTCManagerException wenn die Junction �ber keinen Node verf�gt.
     */
    public final OTCNode getNode() throws OTCManagerException {
        if (this.node == null) {
            throw new OTCManagerException("This junction (id " + id + ") has no node.");
        }
        return this.node;
    }

    /**
     * Setz den dieser Junction zugeordneten Node.
     *
     * @param node Node f�r diese Junction.
     */
    final void setNode(final OTCNode node) {
        this.node = node;
    }

    /**
     * Gibt die Anzahl der Phasen zur�ck, die nicht als "Interphase" markiert
     * sind.
     *
     * @return die Anzahl der Phasen, die nicht als "Interphase" markiert sind
     */
    public final int getNumNonInterphases() {
        int numNonInterphases = 0;
        for (Phase phase : phases) {
            if (!phase.isInterphase()) {
                numNonInterphases++;
            }
        }
        return numNonInterphases;
    }

    /**
     * Gibt die Anzahl der Phasen zur�ck.
     *
     * @return die Anzahl der Phasen
     */
    public final int getNumPhases() {
        return this.numberPhases;
    }

    /**
     * Returns a list of {@code Offset}-objects for a neighbouring node.
     * The neighbour is specified by its id.
     *
     * @param id the id of the neighbouring node
     * @return a list of {@code Offset}-objects or {@code null} if the
     * id is not a neighbouring node
     */
    public final List<Offset> getOffsetForNode(final int id) {
        return this.offsets.get(id);
    }

    /**
     * Returns the offset to the centroid from this junction.
     *
     * @param centroidID if of the centroid
     * @return offset
     */
    public final Offset getOffsetForCentroid(final int centroidID) {
        return this.localCentroidOffsets.get(centroidID);
    }

    /**
     * Zugriff auf die von einer Junction wegf�hrenden Sections.
     *
     * @return Wegf�hrende Sections.
     */
    public final List<Section> getOutSections() {
        return this.outSections;
    }

    /**
     * Gibt die AimsunJunction.Phase zur�ck, die die angegebene Id hat.
     *
     * @param id Id der gesuchten Phase (externe Id, wie in Aimsun).
     * @return Gesuchte Phase als Instanz von AimsunJunction.Phase
     * @throws OTCManagerException Wenn keine Phase mit der angegebenen Id gefunden wurde.
     */
    public final Phase getPhaseById(final int id) throws OTCManagerException {
        Phase returnPhase = null;
        for (Phase phase : this.phases) {
            if (phase.getId() == id) {
                returnPhase = phase;
                break;
            }
        }
        return returnPhase;
    }

    /**
     * Gibt ein Array mit den Ids aller f�r die Junction verf�gbarer Phasen
     * zur�ck.
     *
     * @return Array der Ids.
     */
    public final List<Integer> getPhaseIds() {
        return phases.stream().map(Phase::getId).collect(Collectors.toList());
    }

    public final List<Phase> getPhases() {
        return this.phases;
    }

    /**
     * Get all non-interphases for a turning.
     */
    public final List<Phase> getPhasesForTurnings(final int turningID) {
        final List<Phase> phases = new ArrayList<>();

        this.phases.stream().filter(phase -> !phase.isInterphase()).forEach(phase -> phases.addAll(getPhasesForTurning(turningID, phase)));
        return phases;
    }

    private Collection<? extends Phase> getPhasesForTurning(int turningID, Phase phase) {
        List<Phase> phases = new ArrayList<>();

        List<SignalGroup> signalGroups = phase.getSignalGroups();
        for (SignalGroup signalGroup : signalGroups) {
            List<Turning> turnings = signalGroup.getTurnings();
            phases.addAll(turnings.stream().filter(turning -> turning.getId() == turningID).map(turning -> phase).collect(Collectors.toList()));
        }
        return phases;
    }

    /**
     * Returns the unique signal group containing the given turning.
     *
     * @param turning1 a turning
     * @return the unique signal group containing the given turning
     */
    public final SignalGroup getSignalGroupForTurning(final Turning turning1) {
        final List<SignalGroup> signalGroups = this.signalGroups;

        for (SignalGroup signalGroup : signalGroups) {
            List<Turning> turnings = signalGroup.getTurnings();

            for (Turning turning : turnings) {
                if (turning.equals(turning1)) {
                    return signalGroup;
                }
            }
        }

        throw new OTCManagerException("No signal group for turning " + turning1);
    }

    /**
     * Gibt eine Liste der Signalgruppen für diesen Knoten zur�ck.
     *
     * @return eine Liste der Signalgruppen für diesen Knoten
     */
    public final List<SignalGroup> getSignalGroups() {
        return signalGroups;
    }

    /**
     * Returns the turning from the given inbound section to the given outbound
     * section or {@code null} if no turning exists.
     *
     * @param inSectionId  id of inbound section
     * @param outSectionId id of outbound section
     * @return the desired turning (or {@code null})
     */
    public final Turning getTurning(final int inSectionId, final int outSectionId) {
        final List<Turning> candidateTurns = getTurningsForIncomingSectionID(inSectionId);
        for (Turning turning : candidateTurns) {
            if (turning.getOutSection().getId() == outSectionId) {
                return turning;
            }
        }

        return null;
    }

    /**
     * Liefert ein Array aller Turnings dieser Junction, die Verkehr des
     * angegebenen Typs bedienen, oder {@code null}, wenn keine Turnings
     * f�r den angegebenen Typ existieren.
     *
     * @param trafficType Verkehrsart, f�r die die Turnings zur�ckgegeben werden sollen.
     * @return Array von {@code Turning}-Objekten oder {@code null},
     * wenn kein passendes Turning vorhanden ist.
     */
    public final List<Turning> getTurnings(final TrafficType trafficType) {
        switch (trafficType) {
            case ALL:
                return allTurnings;
            case PEDESTRIANS:
                return pedestrianTurnings;
            case INDIVIDUAL_TRAFFIC:
                return motorisedVehiclesTurnings;
            case PUBLIC_TRANSPORT:
                return publicTransportTurnings;
            case CYCLISTS:
                break;
            case UNDEFINED:
                break;
        }

        return Collections.emptyList();
    }

    /**
     * Methode sammelt alle Turnings f�r eingehende Sections (identifiziert
     * durch den Parameter beim Methodenaufruf).
     *
     * @param sectionId Die ID, die die entsprechende Section identifiziert
     * @return Menge aller Turnings, die ihren Ursprung in der �bergebenen
     * Section haben.
     */
    public final List<Turning> getTurningsForIncomingSectionID(final int sectionId) {
        final List<Turning> turnings = new ArrayList<>();

        // Sammle alle Turnings ein, die als Eingang die als Parameter übergebene Section haben
        turnings.addAll(getTurnings(TrafficType.INDIVIDUAL_TRAFFIC).stream().filter(turning -> turning.getInSection().getId() == sectionId).map(turning -> turning).collect(Collectors.toList()));

        // Rückgabemenge entspricht allen Turnings, die ihren Ursprung in der übergebenen Section haben.
        return turnings;
    }

    /**
     * Collects all turnings that lead to an outgoing section of the junction on
     * the path to a specified neighbour.
     *
     * @param neighbour the ID representing the particular neighbour (junction ID)
     * @return a {@code List} containing all turnings connected with the
     * section leading to the neighbour
     */
    public final List<Turning> getTurningsForNeighbour(final int neighbour) {
        final List<Turning> turnings = new ArrayList<>();
        final Map<Integer, Turning> checkTurns = new HashMap<>();

        for (Offset offset : offsets.get(neighbour)) {
            int startId = offset.getPath().get(0).getId();

            for (Turning turning : getTurnings(TrafficType.INDIVIDUAL_TRAFFIC)) {
                if (turning.getOutSection().getId() == startId) {
                    if (!checkTurns.containsKey(turning.getId())) {
                        turnings.add(turning);
                        checkTurns.put(turning.getId(), turning);
                    }
                }
            }
        }
        return turnings;
    }

    /**
     * Returns a list of other turnings having a common lane with the given
     * turning {@code t}.
     *
     * @param turning a turning
     * @return a list of other turnings having a common lane with the given
     * turning {@code t}
     */
    private List<Turning> getTurnsSharingLanes(final Turning turning) {
        // Falls notwendig: Datenstruktur erzeugen
        if (sharedLanesMap == null) {
            createSharedLanesMap();
        }

        final List<Turning> sharingTurns = new ArrayList<>();

        for (int i = turning.getFirstLaneOrigin(); i <= turning.getLastLaneOrigin(); i++) {
            final List<Turning> turnsForLane = sharedLanesMap.get(turning.getInSection().getId() + ";" + i);

            // Erg�nze anderes Turning auf dieser Spur
            turnsForLane.stream().filter(turn -> !turn.equals(turning) && !sharingTurns.contains(turn)).forEach(sharingTurns::add);
        }

        return sharingTurns;
    }

    /**
     * Gibt zur�ck, ob dieser Kreuzung ein TLC zugeordnet ist.
     *
     * @return True, wenn ein TLC zugeordnet ist, sonst false.
     */
    public final boolean hasActiveTLC() {
        return this.activeTLC != null;
    }

    /**
     * Initialisiert das Attribut absoluteMinDuration (Mindestfreigabezeit) f�r
     * jede Phase, indem umliegende Zwischenphasen darauf gepr�ft werden, ob sie
     * alle Signalgruppen der Phase abdecken. Wenn ja, wird die
     * Mindestfreigabezeit um die Standardfreigabezeit (defaultDuration) der
     * gefundenen passenden Zwischenphasen reduziert. Die Mindestfreigabezeit
     * kann nicht unter 0 fallen.
     */
    final void initAbsoluteMinDurations() {
        int previousPhaseIndex, nextPhaseIndex;
        for (int i = 0; i < numberPhases; i++) {
            final Phase phase = phases.get(i);

            if (phase.isInterphase()) {
                continue;
            }

            previousPhaseIndex = i - 1;

            Phase previousPhase;
            if (previousPhaseIndex < 0) {
                previousPhaseIndex = numberPhases - 1;
            }
            previousPhase = phases.get(previousPhaseIndex);

            boolean validExtension = true;
            while (previousPhase.isInterphase()) {
                for (SignalGroup signalGroup : phase.getSignalGroups()) {
                    if (!previousPhase.getSignalGroups().contains(signalGroup)) {
                        validExtension = false;
                        break;
                    }
                }

                if (validExtension) {
                    final float absoluteMinDuration = phase.getAbsoluteMinDuration()
                            - previousPhase.getDefaultDuration();
                    if (absoluteMinDuration < 0) {
                        phase.setAbsoluteMinimalDuration(0);
                        break;
                    } else {
                        phase.setAbsoluteMinimalDuration(absoluteMinDuration);
                    }
                } else {
                    break;
                }

                previousPhaseIndex--;
                if (previousPhaseIndex < 0) {
                    previousPhaseIndex = numberPhases - 1;
                }
                previousPhase = phases.get(previousPhaseIndex);
            }

            nextPhaseIndex = i + 1;
            if (nextPhaseIndex == numberPhases) {
                nextPhaseIndex = 0;
            }

            Phase nextPhase = phases.get(nextPhaseIndex);
            validExtension = true;
            while (nextPhase.isInterphase()) {
                for (SignalGroup signalGroup : phase.getSignalGroups()) {
                    if (!nextPhase.getSignalGroups().contains(signalGroup)) {
                        validExtension = false;
                        break;
                    }
                }

                if (validExtension) {
                    final float absoluteMinDuration = phase.getAbsoluteMinDuration() - nextPhase.getDefaultDuration();
                    if (absoluteMinDuration < 0) {
                        phase.setAbsoluteMinimalDuration(0);
                        break;
                    } else {
                        phase.setAbsoluteMinimalDuration(absoluteMinDuration);
                    }
                } else {
                    break;
                }

                nextPhaseIndex++;
                if (nextPhaseIndex == numberPhases) {
                    nextPhaseIndex = 0;
                }
                nextPhase = phases.get(nextPhaseIndex);
            }
        }
    }

    /**
     * Gibt zur�ck, ob die Junction extern gesteuert wird oder nicht.
     *
     * @return true, wenn externe Steuerung vorliegt, sonst false.
     */
    public final boolean isControlled() {
        return this.controlled;
    }

    final String printStatisticalData() {
        final String linesep = System.getProperty("line.separator");
        String output = "Statistical data for junction " + id + linesep;

        for (Turning turning : allTurnings) {
            output = output.concat(turning.printStatisticalData() + linesep);
        }
        return output;
    }

    final void receiveLocalCentroid(final List<Section> path) {
        // Add 2.5s to pass inital node
        float offset = 2.5f;

        // Calculate offset
        for (Section section : path) {
            offset += section.getLength() / (section.getSpeedlimit() * 1000 / 3600);
        }

        // Add 1s for each connection between intermediate sections
        offset += path.size() - 1;

        // Round to full seconds
        final int offsetInSeconds = new Float(Math.round(offset)).intValue();

        // Add neighbouring centroid (if not already known)
        final List<Centroid> centroids = path.get(path.size() - 1).getConnectedCentroids();

        for (Centroid centroid : centroids) {
            final int centroidID = centroid.getId();
            this.localCentroids.putIfAbsent(centroidID, centroid);

            final Offset offsetObj = new Offset(this, centroid, path, offsetInSeconds);

            if (!localCentroidOffsets.containsKey(centroidID)) {
                // New Centroid found
                this.localCentroidOffsets.put(centroidID, offsetObj);
            } else {
                // Already known Centroid - keep only shortest offset
                if (offsetObj.getOffset() < localCentroidOffsets.get(centroidID).getOffset()) {
                    // New offset is shorter - replace
                    this.localCentroidOffsets.put(centroidID, offsetObj);
                }
            }
        }
    }

    /**
     * Setzt den aktiven TLC dieser Junction zur�ck (z.B. wenn eine Simulation
     * neu gestartet wird).
     *
     * @see AbstractTLC#reset()
     */
    public final void reset() {
        // R�cksetzen des TLCs
        if (this.activeTLC != null) {
            this.activeTLC.reset();
        }

        // R�cksetzen der Werte f�r DPSS
        if (getNode() instanceof OTCNodeSynchronized) {
            final OTCNodeSynchronized node = (OTCNodeSynchronized) getNode();
            node.initialisePSSValues();
        }
    }

    public final void setTrafficLightController(final AbstractTLC tlc) {
        this.activeTLC = tlc;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dfg.oc.otc.manager.ObservableNetworkObjectDetectors#toString()
     */
    public final String toString() {
        final String linesep = System.getProperty("line.separator");
        String output = "Junction " + id + name + ":" + linesep + allTurnings.size() + " Turnings" + linesep
                + numberPhases + " Phases" + linesep + signalGroups.size() + " SignalGroups" + linesep + linesep;

        for (Turning turning : this.allTurnings) {
            output = output.concat(turning.toString());
        }

        output = output.concat(linesep + "Phases:" + linesep);
        for (Phase phase : this.phases) {
            output = output.concat(phase + linesep);
        }

        return output;
    }
}
