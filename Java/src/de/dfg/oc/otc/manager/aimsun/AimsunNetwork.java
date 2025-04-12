package de.dfg.oc.otc.manager.aimsun;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer1.observer.monitoring.AbstractObservableStatistics;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import de.dfg.oc.otc.manager.aimsun.detectors.SubDetector;
import de.dfg.oc.otc.region.OTCNodeRegion;
import de.dfg.oc.otc.region.OTCNodeSynchronized;

import java.util.*;

/**
 * A traffic network consisting of {@link Section}s, {@link Turning}s,
 * {@link AimsunJunction}s, {@link Centroid}s, etc.
 */
public class AimsunNetwork extends AbstractObservableStatistics {
    /**
     * Enthält alle Centroids des Netzes.
     */
    private final Map<Integer, Centroid> centroidMap;
    /**
     * Enthält nur extern (also von der Java-Seite aus) gesteuerte Junctions des
     * Netzes. Wird durch initJunctions initialisiert.
     */
    private final List<AimsunJunction> controlledJunctions = new ArrayList<>();
    private final DetectorCapabilities detectorCapabilities = new DetectorCapabilities();
    /**
     * Contains all detectors of the network.
     */
    private final Map<Integer, Detector> detectorMap;
    /**
     * Contains all junctions of the network.
     */
    private final Map<Integer, AimsunJunction> junctionMap;
    private final String name;
    /**
     * Enthält für alle extern gesteuerten Junctions einen Node, der die
     * komplette OTC-Steuerungslogik gemäß Architektur kapselt.
     */
    private final Map<Integer, OTCNode> nodeMap;
    /**
     * Enthält alle Sections im Netz, Schlüssel ist die Section-Id.
     */
    private final Map<Integer, Section> sectionMap;

    private final List<SubDetector> subDetectors;
    /**
     * Wird mit jedem neu angelegten Turning inkrementiert, um diesen eindeutige
     * Ids geben zu können.
     */
    private int nextTurningId = 1;

    /**
     * Erzeugt ein neues Netzwerk-Objekt.
     *
     * @param name Name des Netzwerks
     */
    public AimsunNetwork(final String name) {
        this.name = name;
        // 10 SD sind schon fest vergeben
        this.subDetectors = new ArrayList<>();
        // TODO: Subdetectors 0 bis 9 implementieren.
        for (int i = 0; i < 10; i++) {
            subDetectors.add(new SubDetector());
        }

        this.sectionMap = new HashMap<>();
        this.junctionMap = new HashMap<>();
        this.nodeMap = new HashMap<>();
        this.detectorMap = new HashMap<>();
        this.centroidMap = new HashMap<>();
    }

    /**
     * Fügt einen Centroid hinzu.
     *
     * @param id               Id des Centroids
     * @param incomingSections Ids der eingehenden Sections
     * @param outgoingSections Ids der ausgehenden Sections
     */
    public final void addCentroid(final int id, final int[] incomingSections, final int[] outgoingSections) {
        final Centroid centroid = new Centroid(id, incomingSections, outgoingSections);
        this.centroidMap.put(id, centroid);

        for (int sectionID : incomingSections) {
            final Section section = getSection(sectionID);
            section.addConnectedSink(centroid);
        }
    }

    /**
     * Fügt einen Detektor hinzu.
     *
     * @param id        Id des Detektors.
     * @param sectionId Id der Section, in der der Detektor liegt.
     * @param posBegin  Position, an der der Detektor in der Section beginnt.
     * @param firstLane Die erste Fahrspur der Section, die von diesem Detektor
     *                  erfasst wird.
     * @param lastLane  Die letzte Fahrspur der Section, die von diesem Detektor
     *                  erfasst wird.
     * @throws OTCManagerException
     */
    public final void addDetector(final int id, final int sectionId, final float posBegin, final float posEnd,
                                  final int firstLane, final int lastLane) throws OTCManagerException {
        final Section section = sectionMap.get(sectionId);
        if (section == null) {
            throw new OTCManagerException("Section " + sectionId + " not found.");
        }

        final Detector detector = section.addDetector(id, posBegin, posEnd, firstLane, lastLane);
        this.detectorMap.put(id, detector);
    }

    /**
     * Fügt dem Netzwerk eine Junction hinzu.
     *
     * @param id          Id der Junction
     * @param controlType 0=Uncontrolled, 1=Fixed, <b>2=External</b>, 3=Actuated. Nur
     *                    <b>2</b> erlaubt die Kontrolle von der Java-Seite aus.
     * @throws OTCManagerException wenn die Junction nicht erzeugt werden konnte
     */
    public final void addJunction(final int id, final int controlType, final String name) throws OTCManagerException {
        final AimsunJunction newJunction = new AimsunJunction(id, controlType, name);
        this.junctionMap.put(id, newJunction);
    }

    /**
     * Fügt eine neue Phase zur Junction hinzu. Diese Phase repräsentiert eine
     * in Aimsun definierte Phase (inkl. der dort angegebenen Phasendauern).
     *
     * @param id          Id der Phase
     * @param interphase  Legt fest, ob die Phase eine Interphase/Zwischenphase ist
     *                    (true) oder nicht (false).
     * @param duration    Normale Phasendauer.
     * @param maxDuration Maximale Phasendauer.
     * @param minDuration Minimale Phasendauer.
     * @param junctionId  Id der Junction, zu der die neu angelegte Phase gehört.
     */
    public final void addPhase(final int id, final boolean interphase, final float duration, final float maxDuration,
                               final float minDuration, final int junctionId) {
        final AimsunJunction junction = junctionMap.get(junctionId);
        if (junction == null) {
            throw new OTCManagerException("Junction " + junctionId + " not found.");
        }
        junction.addPhase(id, interphase, duration, maxDuration, minDuration);
    }

    /**
     * Fügt eine neue Phase zur Junction hinzu. Diese Phase repräsentiert eine
     * in Aimsun definierte Phase. Phasendauern werden auf Standardwerte (-1)
     * gesetzt.
     *
     * @param id         Id der Phase
     * @param interphase Legt fest, ob die Phase eine Interphase/Zwischenphase ist
     *                   (true) oder nicht (false).
     * @param junctionId Id der Junction, zu der die neu angelegte Phase gehört.
     */
    public final void addPhase(final int id, final boolean interphase, final int junctionId) {
        final AimsunJunction junction = junctionMap.get(junctionId);
        if (junction == null) {
            throw new OTCManagerException("Junction " + junctionId + " not found.");
        }
        junction.addPhase(id, interphase);
    }

    /**
     * Fügt eine Section hinzu.
     *
     * @param angId      ANG-Id der Section
     * @param roadType       Id des zugehörigen RoadTypes der Section
     * @param nbTurnings Anzahl der "Turnings", die von dieser Section ausgehen. Ist
     *                   dieser Wert ==1, schließt sich eine andere Section an, ist er
     *                   größer, schließt sich ein Knotenpunkt (Junction) an.
     * @param length     Länge der Section
     * @param destId     Array der Ids der durch Turnings erreichbaren Sections
     */
    public final void addSection(final int angId, final int roadType, final int nbTurnings, final float length,
                                 final int[] destId, float speedlimit, float capacity) {
        final Section newSection = new Section(angId, roadType, nbTurnings, destId, length, speedlimit, capacity);
        this.sectionMap.put(angId, newSection);
    }

    public final void addSignalGroup(final int id, final int junctionId) throws OTCManagerException {
        final AimsunJunction junction = junctionMap.get(junctionId);
        if (junction == null) {
            throw new OTCManagerException("Junction " + junctionId + " not found.");
        }
        junction.addSignalGroup(id);
    }

    /**
     * Fügt einer Phase eine SignalGroup hinzu.
     *
     * @param id         Id der SignalGroup, die der Phase hinzugefügt werden soll.
     * @param phaseId    Id der Phase, der die SignalGroup hinzugefügt werden soll.
     * @param junctionId Id der Junction, zu der die Phase gehört.
     * @throws OTCManagerException Wenn die Junction nicht existiert.
     */
    public final void addSignalGrpPhase(final int id, final int phaseId, final int junctionId)
            throws OTCManagerException {
        final AimsunJunction junction = junctionMap.get(junctionId);
        if (junction == null) {
            throw new OTCManagerException("Junction " + junctionId + " not found.");
        }
        junction.addSignalGrpPhase(id, phaseId);
    }

    /**
     * Fügt dem Netzwerk ein Turning (Abbiegebeziehung) hinzu. Junction und
     * Sections müssen bereits existieren.
     *
     * @param junctionId   Id der Junction, zu der das Turning gehört.
     * @param signalGrpId  Id der SignalGroup, der das Turning zugeordnet wird.
     * @param sectionInId  Id der Section, die Ausgangspunkt des Turnings ist.
     * @param sectionOutId Id der Section, die Endpunkt des Turnings ist.
     * @throws OTCManagerException wenn die Junction oder eine der Sections nicht existieren.
     * @see AimsunJunction#addTurning(int, int, Section, Section) AimsunJunction.addTurning
     */
    public final void addTurning(final int junctionId, final int signalGrpId, final int sectionInId,
                                 final int sectionOutId) throws OTCManagerException {
        final AimsunJunction junction = junctionMap.get(junctionId);
        if (junction == null) {
            throw new OTCManagerException("Junction " + junctionId + " not found.");
        }

        final Section inSection = sectionMap.get(sectionInId);
        if (inSection == null) {
            throw new OTCManagerException("Section (In) " + sectionInId + " not found.");
        }

        final Section outSection = sectionMap.get(sectionOutId);
        if (outSection == null) {
            throw new OTCManagerException("Section (Out) " + sectionOutId + " not found.");
        }

        junction.addTurning(nextTurningId, signalGrpId, inSection, outSection);
        this.nextTurningId++;
    }

    private void findMappingDetectorForTurning(final Iterable<Turning> turnings) {
        for (Turning turning : turnings) {
            Section inSection = turning.getInSection();
            Section outSection = turning.getOutSection();

            inSection.addTurning(turning);

            for (Detector detector : inSection.getDetectors()) {
                for (int sectionId : detector.getDestinations()) {
                    if (sectionId == outSection.getId()) {
                        turning.addDetector(detector);
                    }
                }
            }
        }
    }

    /**
     * Liefert den Centroid zu Id.
     *
     * @param id Id der der Centroid zugeordnet ist.
     * @return Den zugehörigen Centroid.
     * @throws OTCManagerException wenn der angegebenen ID kein Centroid zugeordnet ist.
     */
    public final Centroid getCentroid(final int id) throws OTCManagerException {
        final Centroid result = this.centroidMap.get(id);
        if (result == null) {
            throw new OTCManagerException("No centroid found with id " + id);
        }
        return result;
    }

    public final Map<Integer, Centroid> getCentroidMap() {
        return this.centroidMap;
    }

    public final List<AimsunJunction> getControlledJunctions() {
        if (this.controlledJunctions.isEmpty()) {
            throw new OTCManagerException(
                    "Es gibt keine extern gesteuerte Junction, oder initJunctions() wurde noch nicht aufgerufen.");
        }
        return this.controlledJunctions;
    }

    public final DetectorCapabilities getDetectorCapabilities() {
        return this.detectorCapabilities;
    }

    public final Map<Integer, Detector> getDetectors() {
        if (this.detectorMap.isEmpty()) {
            throw new OTCManagerException("Es gibt keine Detektoren, oder initJunctions() wurde noch nicht aufgerufen.");
        }
        return this.detectorMap;
    }

    /**
     * @return Die ID des Networks ist immer -1.
     */
    @Override
    public final int getId() {
        return -1;
    }

    /**
     * Returns the junction object for a given ID.
     *
     * @param id ID of the junction that should be returned
     * @return the junction object for the given ID
     * @throws OTCManagerException if no junction object exists
     */
    public final AimsunJunction getJunction(final int id) throws OTCManagerException {
        final AimsunJunction junction = this.junctionMap.get(id);
        if (junction == null) {
            throw new OTCManagerException("Junction " + id + " not found.");
        }
        return junction;
    }

    public final Map<Integer, AimsunJunction> getJunctions() {
        if (this.junctionMap.isEmpty()) {
            throw new OTCManagerException("Es gibt keine Junction, oder initJunctions() wurde noch nicht aufgerufen.");
        }
        return this.junctionMap;
    }

    /**
     * @return Network name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Liefert den Node, der zur Junction mit der übergebenen Id gehört.
     *
     * @param id Id der Junction, der der Node zugeordnet ist.
     * @return Den zugehörigen Node.
     * @throws OTCManagerException wenn der angegebenen Junction kein Node zugeordnet ist.
     */
    public final OTCNode getNode(final int id) throws OTCManagerException {
        final OTCNode node = this.nodeMap.get(id);
        if (node == null) {
            throw new OTCManagerException("No node found for junction " + id);
        }
        return node;
    }

    /**
     * Liefert einen Iterator über alle Nodes in diesem Netzwerk.
     *
     * @return Iterator über alle Nodes.
     * @throws OTCManagerException wenn kein Node vorhanden ist.
     */
    public final Collection<OTCNode> getNodes() throws OTCManagerException {
        if (this.nodeMap.isEmpty()) {
            throw new OTCManagerException("Es gibt keinen Node, oder initJunctions() wurde noch nicht aufgerufen.");
        }
        return this.nodeMap.values();
    }

    /**
     * @return Anzahl der extern gesteuerten Junctions.
     */
    public final int getNumControlledJunctions() {
        return this.controlledJunctions.size();
    }

    /**
     * Returns the section object for a given ID.
     *
     * @param id ID of the section that should be returned
     * @return the section object for the given ID
     * @throws OTCManagerException if no section object exists
     */
    public final Section getSection(final int id) throws OTCManagerException {
        final Section section = sectionMap.get(id);
        if (section == null) {
            throw new OTCManagerException("Section " + id + " not found.");
        }
        return section;
    }

    public final Collection<Section> getSections() {
        if (sectionMap.isEmpty()) {
            throw new OTCManagerException("Es gibt keine Sections, oder initJunctions() wurde noch nicht aufgerufen.");
        }
        return this.sectionMap.values();
    }

    /**
     * Get a turning between two sections.
     */
    public final Turning getTurning(final int inSectionId, final int outSectionId) throws OTCManagerException {
        final Section inSection = sectionMap.get(inSectionId);
        if (inSection == null) {
            throw new OTCManagerException("Section " + inSectionId + " not found");
        }

        final Turning turning = inSection.getTurningMap().get(outSectionId);
        if (turning == null) {
            throw new OTCManagerException("No turning from section " + inSectionId + " to " + outSectionId);
        }

        return turning;
    }

    /**
     * Erledigt die Zuordnung von Detektoren zu Turnings, baut die Liste
     * ControlledJunctions auf und fügt den Phasen die vorhandenen Detektoren
     * hinzu.
     */
    public final void initJunctions() {
        final Collection<AimsunJunction> invalidJunctions = new ArrayList<>();

        for (AimsunJunction junction : this.junctionMap.values()) {
            if (junction.getNumPhases() < 1) {
                invalidJunctions.add(junction);
                continue;
            }

            List<Turning> turnings = junction.getTurnings(TrafficType.ALL);
            if (turnings.isEmpty()) {
                invalidJunctions.add(junction);
                continue;
            }

            junction.initAbsoluteMinDurations();
            findMappingDetectorForTurning(turnings);

            // Zuweisen der Subdetektoren zu den Phasen
            List<Phase> phases = junction.getPhases();
            if (phases.isEmpty()) {
                continue;
            }
            mapSubDetectorsWithPhases(phases);

            if (junction.isControlled()) {
                this.controlledJunctions.add(junction);
            }

            // Erzeugen des Knoten-Controllers
            OTCNode node;
            if (DefaultParams.PSS_REGION_ACTIVE) {
                // Nutze Region-Funktion
                node = new OTCNodeRegion(junction, Attribute.LOS);
            } else {
                node = new OTCNodeSynchronized(junction, Attribute.LOS);
            }

            this.nodeMap.put(junction.getId(), node);
            junction.setNode(node);
        }

        // Junctions, die keine Phasen oder Turnings haben, werden entfernt.
        for (AimsunJunction junction : invalidJunctions) {
            this.junctionMap.remove(junction.getId());
        }

        this.junctionMap.values().forEach(AimsunJunction::getNeighbouringNodes);
    }

    /**
     * Verknüpft die Sections des aktuellen Netzwerks miteinander, so dass jede
     * Section eine Quell- und eine Zielsection hat oder als Eingangs- bzw.
     * Ausgangssection gekennzeichnet ist.
     */
    public final void initSections() {
        for (Section section : this.sectionMap.values()) {
            int[] destinationIds = section.getDestinationIds();
            for (int id : destinationIds) {
                Section destinationSection = sectionMap.get(id);
                section.addDestinationSection(destinationSection);
                destinationSection.addOriginSection(section);
            }
        }

        this.sectionMap.values().forEach(Section::checkEntryExit);
    }

    public final void initSubDetectors() {
        // Bereinigen der Subdetektor-Listen ist nötig, falls diese Methode
        // (z.B. in JUnit-Tests) mehrfach aufgerufen wird.
        final Collection<SubDetector> standardDetectors = new ArrayList<>(subDetectors.subList(0, 10));
        this.subDetectors.retainAll(standardDetectors);

        int subDetectorCounter = 10;
        // Die ersten 9 sind reserviert für Simulationszeit u.ä.

        for (Detector detector : detectorMap.values()) {
            for (int i = 0; i < DetectorCapabilities.NUM; i++) {
                SubDetector[] subDetectors = detector.getSubDetectors();
                if (subDetectors[i].isEnabled()) {
                    subDetectors[i].setId(subDetectorCounter);
                    subDetectors[i].setDetector(detector);
                    subDetectors[i].setDetectorIdentifier(detector.getId());
                    this.subDetectors.add(subDetectorCounter, subDetectors[i]);

                    subDetectorCounter++;
                }
            }
        }
    }

    private void mapSubDetectorsWithPhases(final Iterable<Phase> phases) {
        final Collection<SubDetector> subDetectors = new ArrayList<>();

        for (Phase phase : phases) {
            // löscht alle schon eingetragenen Subdetektoren
            phase.resetDetectors();

            List<SignalGroup> signalGroups = phase.getSignalGroups();
            if (signalGroups.isEmpty()) {
                continue;
            }

            for (SignalGroup signalGroup : signalGroups) {
                List<Turning> turnings = signalGroup.getTurnings();

                if (turnings.isEmpty()) {
                    continue;
                }

                for (Turning turning : turnings) {
                    for (Detector detector : turning.getDetectors()) {
                        SubDetector presenceDetector = detector.getSubDetector(DetectorCapabilities.PRESENCE);
                        if (presenceDetector.isEnabled() && !subDetectors.contains(presenceDetector)) {
                            phase.addRecallDetector(presenceDetector);
                            subDetectors.add(presenceDetector);
                        }

                        SubDetector counterDetector = detector.getSubDetector(DetectorCapabilities.COUNT);
                        if (counterDetector.isEnabled() && !subDetectors.contains(counterDetector)) {
                            phase.addCounterDetector(counterDetector);
                            subDetectors.add(counterDetector);
                        }

                        SubDetector headwayDetector = detector.getSubDetector(DetectorCapabilities.HEADWAY);
                        if (headwayDetector.isEnabled() && !subDetectors.contains(headwayDetector)) {
                            phase.addHeadwayDetector(headwayDetector);
                            subDetectors.add(headwayDetector);
                        }
                    }
                }
            }
        }
    }

    public final String printStatisticalData() {
        final String linesep = System.getProperty("line.separator");
        String output = "";
        for (AimsunJunction junction : junctionMap.values()) {
            output = output.concat(junction.printStatisticalData() + linesep);
        }
        return output;
    }

    public final void restart() {
        detectorMap.values().forEach(Detector::reset);
        junctionMap.values().forEach(AimsunJunction::reset);
    }

    public final float sectionDistance(final Section sectionIn, final Section sectionOut) throws OTCManagerException {
        if (sectionIn.isExit() && sectionIn != sectionOut) {
            throw new OTCManagerException("In-section is exit, no valid path to out-section");
        } else if (sectionIn == sectionOut) {
            return sectionIn.getLength();
        }

        for (int sectionId : sectionIn.getDestinationIds()) {
            Section newSectionIn = sectionMap.get(sectionId);
            if (newSectionIn == null) {
                throw new OTCManagerException("Section " + sectionIn.getId() + " gives invalid destination id ("
                        + sectionId + ")");
            }

            if (newSectionIn == sectionOut) {
                return sectionOut.getLength() + sectionIn.getLength();
            }

            try {
                float distance = sectionDistance(newSectionIn, sectionOut);
                distance += sectionIn.getLength();
                return distance;
            } catch (OTCManagerException ignored) {
            }
        }
        throw new OTCManagerException("No valid path to out-section");
    }

    public final void setDetectorCapabilities(final int detectorId, final boolean count, final boolean presence,
                                              final boolean speed, final boolean occupancy, final boolean headway, final boolean density,
                                              final boolean equippedVehicle) throws OTCManagerException {
        final Detector detector = detectorMap.get(detectorId);
        if (detector == null) {
            throw new OTCManagerException("Detector " + detectorId + " not found");
        }
        detector.setCapabilities(count, presence, speed, occupancy, headway, density, equippedVehicle);
    }

    public final void setDetectorDestinations(final int detectorId, final int[] destinationIds)
            throws OTCManagerException {
        final Detector detector = detectorMap.get(detectorId);
        if (detector == null) {
            throw new OTCManagerException("Detector " + detectorId + " not found.");
        }
        detector.setDestinations(destinationIds);
    }

    public final String toString() {
        final String linesep = System.getProperty("line.separator");
        String output = "Network " + name + ":" + linesep + sectionMap.size() + " Sections, " + this.junctionMap.size()
                + " Junctions, " + this.detectorMap.size() + " Detectors." + linesep;

        for (AimsunJunction junction : junctionMap.values()) {
            output = output.concat(junction + linesep);
        }

        return output;
    }
}
