package de.dfg.oc.otc.manager.aimsun;

import de.dfg.oc.otc.aid.AIDUtilities;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Logic structure for the AID Component.
 * A Link represents a road with network edges and/or junctions as start- and endpoints and groups all Sections and detectors contained between them.
 * Ein Link kann aus einer oder mehreren Sections bestehen. Links beginnen und enden immer entweder in einer Junction
 * oder einem Centroid. Links sind unidirektional und k�nnen redundante
 * Informationen enthalten, wenn es Abzweigungen in den Sections gibt.
 */
public class Link implements Cloneable {
    // Finale Zuordnung von tats�chlichen DetecorPairs zum Link -> Erm�glicht
    // Erzeugung von MZs anhand Links
    private final List<AbstractDetectorGroup> monitoredDetectorPairs;
    /**
     * Detector stations on the associated sections.
     */
    private final List<Detector> monitoredDetectors;
    /**
     * Set of sections which are combined by this link.
     */
    private final List<Section> monitoredSections;
    private List<Detector[]> detectorBranchGroups;
    /**
     * Junction where this links ends.
     */
    private AimsunJunction endJunction;
    /**
     * Unique link identifier.
     */
    private int id;
    /*
     * Low-level Betrachtung von Paaren. Den eigentlichen Aufbau nimmt die Node
     * anhand von Constraints vor. Dazu braucht sie diese Informationen.
     */
    private List<Detector[]> simpleDetectorPairs;
    /**
     * Junction where this link starts.
     */
    private AimsunJunction startJunction;
    private Type type;

    /**
     * Erzeugt einen Link aus allen ben�tigten Informationen. Allen anderen
     * Eigenschaften des Links werden von ihm selbst berechnet.
     *
     * @param id                (einzigartige) ID des Links.
     * @param monitoredSections zum Link geh�renden Sections.
     */
    public Link(final int id, final List<Section> monitoredSections) {
        this.id = id;
        this.monitoredSections = new ArrayList<>(monitoredSections);
        this.monitoredDetectors = new ArrayList<>();
        this.monitoredDetectorPairs = new ArrayList<>();

        initLinkValues();
    }

    public final void addDetectorPair(final AbstractDetectorGroup pair) {
        this.monitoredDetectorPairs.add(pair);
    }

    /*
     * Es sind zu einer Forking Section des Links diejenigen Detektoren
     * gespeichert, die vor und hinter dem Fork liegen. So kann anschliessend
     * beurteilt werden, ob ein potentielles Paar von einem Fork getrennt wird.
     */
    private List<Detector>[] addToForkingMap(final Section section, final boolean inFork) {
        /*
         * Eine Zuordnung zwischen forking Sections und allen Detektoren die um
		 * sie herum liegen. Wird ben�tigt um Herauszufinden, ob ein
		 * Detektorpaar Outputs/Inputs hat.
		 */
        final int index = this.monitoredSections.indexOf(section);
        final List<Detector> tempAhead = new ArrayList<>();
        final List<Detector> tempAfter = new ArrayList<>();

        if (section.getNumberOfDetectors() > 1) {
            Collections.sort(section.getDetectors(), AIDUtilities.getInstance().getDetectorComparator());
        }

        // Alle Sections, die vor der aktuellen liegen
        for (int i = 0; i < index; i++) {
            final Section associatedSection = this.monitoredSections.get(i);
            if (associatedSection.getNumberOfDetectors() > 1) {
                Collections.sort(associatedSection.getDetectors(), AIDUtilities.getInstance().getDetectorComparator());
                tempAhead.addAll(associatedSection.getDetectors());
            }
        }

        if (inFork) {
            tempAfter.addAll(section.getDetectors());
        } else {
            tempAhead.addAll(section.getDetectors());
        }

        for (int i = index + 1; i < this.monitoredSections.size(); i++) {
            final Section associatedSection = this.monitoredSections.get(i);
            if (associatedSection.getNumberOfDetectors() > 1) {
                Collections.sort(associatedSection.getDetectors(), AIDUtilities.getInstance().getDetectorComparator());
                tempAfter.addAll(associatedSection.getDetectors());
            }
        }

        final List<Detector>[] temp = new ArrayList[2];
        temp[0] = tempAhead;
        temp[1] = tempAfter;

        return temp;
    }

    private List<Detector[]> buildBranchDetectorGroups(final Map<Section, List<Detector>[]> forkDetectorMap) {
        final List<Detector[]> detectorPairs = new ArrayList<>();
        for (Map.Entry<Section, List<Detector>[]> sectionEntry : forkDetectorMap.entrySet()) {
            List<Detector>[] detectors = sectionEntry.getValue();

            if (!detectors[0].isEmpty() && !detectors[1].isEmpty()) {
                Detector[] detectorPair = new Detector[2];
                detectorPair[0] = detectors[0].get(detectors[0].size() - 1);
                detectorPair[1] = detectors[1].get(0);

                if (!isPairContained(detectorPair, detectorPairs)) {
                    detectorPairs.add(detectorPair);
                }
            }
        }

        return detectorPairs;
    }

    private List<Detector[]> buildSimpleDetectorPairs() {
        final List<Detector[]> detectorPairs = new ArrayList<>();
        for (int i = 1; i < this.monitoredDetectors.size(); i++) {
            final Detector[] detectorPair = new Detector[2];
            detectorPair[0] = this.monitoredDetectors.get(i - 1);
            detectorPair[1] = this.monitoredDetectors.get(i);

            detectorPairs.add(detectorPair);
        }
        return detectorPairs;
    }

    /*
     * Leitet alle Werte aus den �bergebenen Werten ab: type,endpoints und
     * monitoredDetectors.
     */
    private void initLinkValues() {
        final Section start = this.monitoredSections.get(0);
        final Section end = this.monitoredSections.get(this.monitoredSections.size() - 1);
        determineLinkTypes(start, end);
        setAssociatedJunctions(start, end);

        setDetectorSectionMapping();

        final Map<Section, List<Detector>[]> forkDetectorMap = getDetectorForkMapping();
        this.detectorBranchGroups = buildBranchDetectorGroups(forkDetectorMap);

        final List<Detector[]> simpleDetectors = buildSimpleDetectorPairs();
        this.simpleDetectorPairs = removeForksFromSimpleDetectorPairs(simpleDetectors, this.detectorBranchGroups);
    }

    private void determineLinkTypes(final Section start, final Section end) {
        final boolean startLinkedToJunction = start.isJunctionApproach() || start.isJunctionExit();
        final boolean startLinkedToEdge = start.isEntry() || start.isExit();
        final boolean endLinkedToJunction = end.isJunctionApproach() || end.isJunctionExit();
        final boolean endLinkedToEdge = end.isEntry() || end.isExit();

        if (start.equals(end)) {
            if (start.isJunctionApproach() && start.isJunctionExit()) {
                this.type = Type.JUNCTION_TO_JUNCTION;
            } else if (start.isEntry() && start.isExit()) {
                this.type = Type.EDGE_TO_EDGE;
            } else if (start.isEntry()) {
                this.type = Type.EDGE_TO_JUNCTION;
            } else {
                this.type = Type.JUNCTION_TO_EDGE;
            }
        } else {
            if (startLinkedToJunction && endLinkedToJunction) {
                this.type = Type.JUNCTION_TO_JUNCTION;
            } else if (startLinkedToEdge && endLinkedToEdge) {
                this.type = Type.EDGE_TO_EDGE;
            } else if (startLinkedToEdge && endLinkedToJunction) {
                this.type = Type.EDGE_TO_JUNCTION;
            } else {
                this.type = Type.JUNCTION_TO_EDGE;
            }
        }
    }

    public final Iterable<Section> getMonitoredSections() {
        return this.monitoredSections;
    }

    /**
     * Diese Methode wird ben�tigt um Kopien von Links zwischen Nodes zu
     * �bergeben.
     *
     * @return Die Linkkopie
     */
    public final Link getClone() throws CloneNotSupportedException {
        return (Link) clone();
    }

    /**
     * Gibt die �u�erste Entfernung zwischen 2 Detektoren die auf diesem Link
     * liegen zur�ck oder -1 falls mindestens einer der Detektoren nicht auf dem
     * Link liegt.
     *
     * @param upstream   , der Upstream Detektor
     * @param downstream , der Downstream Detektor
     * @return Die �u�erste Entfernung zwischen den Detektoren oder -1, falls
     * sie nicht auf dem Link liegen.
     */
    public final float getDetectorDistance(final Detector upstream, final Detector downstream) {
        final int sectionId1 = upstream.getSectionId();
        final int sectionId2 = downstream.getSectionId();

        if (sectionId1 == sectionId2) {
            return downstream.getPositionEnd() - upstream.getPositionBegin();
        }

        float distance = 0;
        boolean found = false;
        for (Section section : this.monitoredSections) {
            // add part of first section
            if (section.getId() == sectionId1) {
                found = true;
                distance = section.getLength() - upstream.getPositionBegin();
            }
            // add part of last section
            else if (section.getId() == sectionId2) {
                distance += downstream.getPositionEnd();
                if (!found) {
                    return -1;
                }
                return distance;
            }
            // add sections in between
            else if (found) {
                distance += section.getLength();
            }
        }

        return -1;
    }

    /**
     * Hier m�sste f�r die Link-L�nge noch die L�nge der Verbindungsst�cke mit
     * eingerechnet werden. Dies ist jedoch aufgrund des momentanen
     * Implementierung des AimSunNetworks nicht m�glich, da diese Information
     * von AimSun nicht abgefragt wird.
     */
    private Map<Section, List<Detector>[]> getDetectorForkMapping() {
        final Map<Section, List<Detector>[]> forkDetectorMap = new HashMap<>();

        for (Section section : this.monitoredSections) {
            if (section.getNumDestinationSections() > 1 && !section.isJunctionApproach()) {
                List<Detector>[] detectors = addToForkingMap(section, false);
                forkDetectorMap.put(section, detectors);
            } else if (section.getNumOriginSections() > 1 && !section.isJunctionExit()) {
                List<Detector>[] detectors = addToForkingMap(section, true);
                forkDetectorMap.put(section, detectors);
            }
        }

        return forkDetectorMap;
    }

    /**
     * Gibt die Entfernungen vom Anfang und Ende des Links bis zu einem Detektor
     * d an.
     *
     * @param detect , der Detektor von dem aus die Entfernungen berechnet werden.
     * @return Die Entfernung vom Anfang des Link und die Entfernung vom Ende
     * des Links bis zum Detektor oder -1, falls er nicht auf dem Link
     * liegt.
     */
    public final float[] getDistancesToStartEnd(final Detector detect) {
        final float[] distances = new float[2];

        if (!isDetectorContained(detect)) {
            distances[0] = -1;
            distances[1] = -1;
            return distances;
        }

        float startDistance = 0;
        float endDistance = 0;
        boolean reached = false;

        for (Section section : this.monitoredSections) {
            if (!reached) {
                if (section.getId() != detect.getSectionId()) {
                    startDistance += section.getLength();
                } else {
                    startDistance += detect.getPositionBegin();
                    reached = true;
                    endDistance = section.getLength() - detect.getPositionEnd();
                }
            } else {
                endDistance += section.getLength();
            }
        }

        distances[0] = startDistance;
        distances[1] = endDistance;

        return distances;
    }

    public final Iterable<Detector[]> getDividedDetectorPairs() {
        return this.detectorBranchGroups;
    }

    /**
     * @return Junction bei der der Link endet oder null
     */
    public final AimsunJunction getEndJunction() {
        return this.endJunction;
    }

    public final int getId() {
        return this.id;
    }

    /**
     * Diese Methode sollte nur verwendet werden um die Id einer Linkkopie zu
     * �ndern. Die Erstzuweisung einer Id erfolgt bereits �ber den Konstruktor.
     *
     * @param id , neue Id f�r diese Linkkopie
     */
    public final void setId(final int id) {
        this.id = id;
    }

    /**
     * Gibt zu einem unfertigen(2 durch Fork getrennte Detektoren)
     * DividedPair(!) zur�ck wieviele In- und Outstreams es hat.
     *
     * @param d1 Upstream-Detektor des Paares
     * @param d2 Downstream-Detektor des Paares
     * @return Array mit der ersten Komponente gleich der Anzahl der
     * Instreams und der zweiten gleich der Outstreams
     */
    public final int[] getInOutStreamsForPair(final Detector d1, final Detector d2) {
        int instreams = 0;
        int outstreams = 0;

        final int firstSectionId = d1.getSectionId();
        final int secondSectionId = d2.getSectionId();

        boolean inbetween = false;

        for (Section section : this.monitoredSections) {
            if (section.getId() == firstSectionId && !inbetween) {
                inbetween = true;
            }

            if (inbetween) {
                if (!section.isJunctionExit() && section.getId() != firstSectionId) {
                    instreams += section.getNumOriginSections();
                    if (!section.isEntry()) {
                        instreams -= 1;
                    }
                }

                if (!section.isJunctionApproach() && section.getId() != secondSectionId) {
                    outstreams += section.getNumDestinationSections();
                    if (!section.isExit()) {
                        outstreams -= 1;
                    }
                }
            }

            if (section.getId() == secondSectionId) {
                break;
            }
        }

        final int[] streams = new int[2];
        streams[0] = instreams;
        streams[1] = outstreams;

        return streams;
    }

    public final float getLength() {
        float length = 0;
        for (Section section : this.monitoredSections) {
            length += section.getLength();
        }
        return length;
    }

    public final Iterable<Detector[]> getSimpleDetectorPairs() {
        return this.simpleDetectorPairs;
    }

    /**
     * @return Junction bei der der Link anf�ngt oder null
     */
    public final AimsunJunction getStartJunction() {
        return this.startJunction;
    }

    public final Type getType() {
        return this.type;
    }

    public final boolean hasMonitoredDetectors() {
        return !this.monitoredDetectors.isEmpty();
    }

    public final boolean isDetectorContained(final Detector detector) {
        for (Detector detector1 : this.monitoredDetectors) {
            if (detector1.getId() == detector.getId()) {
                return true;
            }
        }

        return false;
    }

    public final boolean isDetectorPairContained(final AbstractDetectorGroup pair) {
        for (AbstractDetectorGroup detectorPair : this.monitoredDetectorPairs) {
            if (detectorPair.isEquallyComposed(pair)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ein Link ist eindeutig durch seine Section-Komposition charakterisiert.
     * Eine Pr�fung auf Gleichheit kann also auf diesem Kritierium allein
     * beruhen.
     *
     * @param otherLink , der Link mit dem dieser auf Gleichheit verglichen werden
     *                  soll.
     * @return true, wenn die Links gleich zusammengesetzt sind, false sonst
     */
    public final boolean isEquallyComposed(final Link otherLink) {
        for (Section section : otherLink.monitoredSections) {
            for (Section associatedSection : this.monitoredSections) {
                if (section.getId() != associatedSection.getId()) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * Sicherstellen, dass Paar nicht bereits vorhanden ist. In einigen
     * Situtation (viele In- und outputs) kann es bedingt durch den Algorithmus
     * sonst zu Redundanzen kommen.
     */
    private boolean isPairContained(final Detector[] candidate, final Iterable<Detector[]> pairs) {
        for (Detector[] pair : pairs) {
            if (pair[0].getId() == candidate[0].getId() && pair[1].getId() == candidate[1].getId()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Diese Methode findet alle einfachen und alle durch einen Fork getrennten
     * Detektorenpaare. Die Paare werden noch darauf gepr�ft ob sie schon
     * vorkommen. Es handelt sich hierbei aber nur um 2 Detektoren, die zusammen
     * ein Paar bilden k�nnen. Das eigentlich DetektorPair- Objekt wird erst in
     * der OTCNodeAID-Klasse aus diesen Informationen gebaut. Dort werden sie
     * dann auch erst auf Constraints gepr�ft.
     */
    private List<Detector[]> removeForksFromSimpleDetectorPairs(final List<Detector[]> simpleDetectors, final Iterable<Detector[]> branchDetectorGroups) {
        // Durch Forks getrennten Paare von den einfachen Paaren abziehen
        for (Detector[] branchDetector : branchDetectorGroups) {
            Iterator<Detector[]> it = simpleDetectors.iterator();
            while (it.hasNext()) {
                Detector[] detectorPair = it.next();
                if (detectorPair[0].getId() == branchDetector[0].getId()
                        && detectorPair[1].getId() == branchDetector[1].getId()) {
                    it.remove();
                }
            }
        }

        return simpleDetectors;
    }

    private void setAssociatedJunctions(final Section start, final Section end) {
        switch (this.type) {
            case JUNCTION_TO_JUNCTION:
                this.startJunction = start.getPreviousJunction();
                this.endJunction = end.getNextJunction();
                break;
            case JUNCTION_TO_EDGE:
                this.startJunction = start.getPreviousJunction();
                break;
            case EDGE_TO_JUNCTION:
                this.endJunction = end.getNextJunction();
                break;
            case EDGE_TO_EDGE:
                // No junctions asssociated
                break;
        }
    }

    private void setDetectorSectionMapping() {
        for (Section section : this.monitoredSections) {
            // Detectoren nach Reihenfolge sortieren und dann hinzuf�gen
            List<Detector> detectors = section.getDetectors();
            if (detectors.size() > 1) {
                Collections.sort(detectors, AIDUtilities.getInstance().getDetectorComparator());
            }

            monitoredDetectors.addAll(detectors.stream().map(detector -> detector).collect(Collectors.toList()));
        }
    }

    public final String toString() {
        return "Link " + this.id + " Type: " + this.type + ", Sections: " + this.monitoredSections;
    }

    public enum Type {
        JUNCTION_TO_JUNCTION, JUNCTION_TO_EDGE, EDGE_TO_JUNCTION, EDGE_TO_EDGE
    }
}
