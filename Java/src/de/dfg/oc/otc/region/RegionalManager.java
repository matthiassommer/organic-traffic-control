package de.dfg.oc.otc.region;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Regional manager - used for the calculation of regional network
 * configurations (e.g. PSS - progressive signal systems).
 *
 * @author tomforde
 */
public final class RegionalManager {
    private static final Logger log = Logger.getLogger(RegionalManager.class);
    private static RegionalManager instance;
    /**
     * Default values for graph creation.
     */
    private final float DEFAULTCOST = 1;
    /**
     * The registered OTCNodes identified by their ID.
     */
    private final Map<Integer, OTCNodeRegion> nodes;
    private final float normalisationCost = 0.5f;
    /**
     * Maximum cost of all edges (needed for inverting).
     */
    private float highestCost;
    /**
     * The representations created and transfered by the nodes.
     */
    private Map<Integer, NodeDataStructure> nodeRepresentations;
    /**
     * List of all determined OTCNode paths.
     */
    private List<PSSPath> otcPaths;
    /**
     * List of all determined paths for the PSSsys using 8-node representations.
     */
    private List<PSSPath> paths;
    /**
     * List of all PSS informations per OTC-node.
     */
    private Map<Integer, PSSInfo> pssInfos;
    /**
     * List of all systems of PSSs.
     */
    private List<PSSSystem> systems;
    /**
     * Currently active/chosen PSS system.
     */
    private PSSSystem activeSystem;
    /**
     * The graph of the network.
     */
    private Graph networkGraph;

    private RegionalManager() {
        this.nodes = new HashMap<>(8);
        this.nodeRepresentations = new HashMap<>(8);
        this.pssInfos = new HashMap<>(10);
        this.systems = new ArrayList<>(5);
    }

    public static RegionalManager getInstance() {
        if (instance == null) {
            instance = new RegionalManager();
        }
        return instance;
    }

    /**
     * Main method for central calculation of PSS.
     */
    void calculatePSS() {
        log.info(OTCManager.getInstance().getTime() + ": Calculating PSS");

        resetValues();
        receiveNodeRepresentations();
        createGraph();

        // Teste, ob Berechnungsgrundlage erstellt
        if (highestCost > DEFAULTCOST && highestCost > normalisationCost) {
            this.networkGraph.invertCosts(highestCost);

            determineStreams();
            filterPaths();

            // Kosten bestimmen, IDs setzen
            int counter = 1;
            this.otcPaths = new ArrayList<>();
            for (PSSPath path : paths) {
                try {
                    PSSPath otcPath = generateOTCPath(path);

                    // Invertiere die Kosten des Pfades
                    otcPath.invertCost(highestCost);

                    // Bestimmen und Setzen der Kosten des Pfades
                    // path.setCost(determineBenefitingNbOfCars(path.getPath()));
                    otcPaths.add(otcPath);

                    counter++;
                } catch (OTCManagerException e) {
                    log.warn("Created invalid otc path object!");
                }
            }

            determineStreamSystems();
            chooseBestStreamSystem();
            generateNodeInformation();

            log.debug("RM created " + pssInfos.size() + " infos for nodes!");
            for (OTCNodeRegion node : nodes.values()) {
                // Aufruf der entsprechenden Methode zur übergabe der Daten
                node.receivePSSInformationFromRegionalManager(node.getId(), pssInfos.get(node.getId()));
            }

            // TODO Check auf Veränderung: Wenn alles gleich bleibt keine Aktion!
            // Aktiviere DPSS Mechanismus am jeweils ersten Knoten
            pssInfos.values().forEach(this::applyPSSValues);
        } else {
            log.warn("Error creating the graph: invalid cost received!");
        }
    }

    private void applyPSSValues(PSSInfo pssInfo) {
        // Fuer alle Knoten mit aktivem PSS, die Startknoten sind
        if (pssInfo.isActivePSS() && pssInfo.isStart()) {
            log.debug("Starting DPSS mechanism at node " + pssInfo.getOtcNodeID());
            nodes.get(pssInfo.getOtcNodeID()).runSynchronisationMechanism(6);
        }
    }

    /**
     * Method chooses and sets the best PSS system out of the set of possible
     * candidates.
     */
    private void chooseBestStreamSystem() {
        if (systems.isEmpty()) {
            log.error("Unable to choose best stream system: no systems available!");
            return;
        }

        PSSSystem bestSystem = null;
        float bestValue = 0;
        // Iteriere ueber alle erstellten Systeme
        for (PSSSystem system : systems) {
            // Bestimme die Anzahl der profitierenden Fhzg
            float value = system.calculateBenefit();

            // Waehle das System mit dem hoechsten Wert
            if (value > bestValue) {
                bestValue = value;
                bestSystem = system;
            }
        }

        this.activeSystem = bestSystem;
    }

    /**
     * Method creates the graph by using the information received from the
     * nodes.
     */
    private void createGraph() {
        if (!nodeRepresentations.isEmpty()) {
            this.networkGraph = new Graph();
            this.highestCost = 0;
            final Map<Integer, JunctionDataStructure> edgesForNodes = new HashMap<>();

            // Iteriere ueber alle empfangenen NodeDataStructures
            for (NodeDataStructure nodeDataStructure : nodeRepresentations.values()) {
                // Hinzufügen aller Kanten
                Collection<TurningDataStructure> turnDataStructures = nodeDataStructure.getTDS();

                // Iteriere ueber alle TurningStructures des Knotens
                for (TurningDataStructure turnDataStructure : turnDataStructures) {
                    float cost = turnDataStructure.getWeight();

                    // Normalisieren
                    if (Float.isNaN(cost) || cost < 0 || cost > Float.MAX_VALUE) {
                        cost = normalisationCost;
                    }
                    // Fuege Kante zum Graphen hinzu
                    networkGraph.addEdge(turnDataStructure.getOriginNodeID(), turnDataStructure.getDestinationNodeID(),
                            cost, false);

                    // Update der hoechsten Kosten (dient der Invertierung)
                    if (cost > highestCost) {
                        highestCost = cost;
                    }
                }

                // Speichern der Entries/ Exits
                networkGraph.addInSubNodeIDs(nodeDataStructure.getInSubNodeIDs());
                networkGraph.addOutSubNodeIDs(nodeDataStructure.getOutSubNodeIDs());

                // Speichern aller subnode - node Beziehungen
                // Iteriere ueber alle Vorgaengerknoten
                updateJunctionDataPredecessors(nodeDataStructure, edgesForNodes);
                updateJunctionDataSuccessors(nodeDataStructure, edgesForNodes);
            }
            // Nun noch alle Zwischenkanten einfügen (Zwischen OTCNodes)
            edgesForNodes.values().forEach(this::addNewEdge);
        } else {
            this.networkGraph = null;
            this.highestCost = -1;
            final String message = "Couldn't create graph: no node representations!";
            log.error(message);
            OTCManager.getInstance().newWarning(message);
        }
    }

    private void updateJunctionDataSuccessors(NodeDataStructure nodeDataStructure, Map<Integer, JunctionDataStructure> edgesForNodes) {
        // Iteriere ueber alle Nachfolgerknoten
        for (OTCNode node : nodeDataStructure.getSuccessorNodes()) {
            int id = 1000 * nodeDataStructure.getNodeID() + node.getId();
            if (edgesForNodes.containsKey(id)) {
                // Update des Eintrages
                JunctionDataStructure structure = edgesForNodes.get(id);
                structure.setOriginSubNodeID(nodeDataStructure.getOutNodeIDForNeighbour(node.getId()));
            } else {
                // Neuer Eintrag
                JunctionDataStructure structure = new JunctionDataStructure();
                structure.setOriginSubNodeID(nodeDataStructure.getOutNodeIDForNeighbour(node.getId()));
                edgesForNodes.put(id, structure);
            }
        }
    }

    private void updateJunctionDataPredecessors(NodeDataStructure nodeDataStructure, Map<Integer, JunctionDataStructure> edgesForNodes) {
        for (OTCNode node : nodeDataStructure.getPredecessorNodes()) {
            int id = 1000 * node.getId() + nodeDataStructure.getNodeID();
            if (edgesForNodes.containsKey(id)) {
                // Update des Eintrages
                JunctionDataStructure structure = edgesForNodes.get(id);
                structure.setDestinationSubNodeID(nodeDataStructure.getInNodeIDForNeighbour(node.getId()));
            } else {
                // Neuer Eintrag
                JunctionDataStructure structure = new JunctionDataStructure();
                structure.setDestinationSubNodeID(nodeDataStructure.getInNodeIDForNeighbour(node.getId()));
                edgesForNodes.put(id, structure);
            }
        }
    }

    private void addNewEdge(JunctionDataStructure junctionDataStructure) {
        boolean validOriginNode = junctionDataStructure.getOriginSubNodeID() >= 0;
        boolean validDestinationNode = junctionDataStructure.getDestinationSubNodeID() >= 0;
        if (validOriginNode && validDestinationNode) {
            networkGraph.addEdge(junctionDataStructure.getOriginSubNodeID(), junctionDataStructure.getDestinationSubNodeID(),
                    DEFAULTCOST, true);
            if (highestCost < DEFAULTCOST) {
                highestCost = DEFAULTCOST;
            }
        }
    }

    /**
     * Method used to determine the cost of a given path by taking into account
     * the number of cars who benefit from a PSS on this path.
     *
     * @param edges The path as sequence of edges
     * @return The nb of cars per hour who benefit from a possible PSS
     */
    private float determineBenefitingCars(final Collection<Edge> edges) {
        if (edges.isEmpty()) {
            log.debug("Received invalid edge list. Cannot determine number of benfitting cars!");
            return -1;
        }
        final List<Edge> list = generateEdgesList(edges);

        boolean ignoredFirstNode = false;
        float nbOfCars = 0;

        // Folge dem Pfad, finde den Wert der günstigsten ursprünglichen Kosten
        for (Edge edge : list) {
            if (!edge.isIntermediate()) {
                // Startknoten ignorieren
                if (ignoredFirstNode) {
                    // Ungueltige Kanten und Zwischenkante ignorieren
                    if (!edge.isIntermediate()) {
                        // Aufpassen, das nicht invertierte Kosten gerechnet werden
                        float cost;
                        if (edge.isInverted()) {
                            cost = edge.getPrimaryCost();
                        } else {
                            cost = edge.getCost();
                        }
                        // Aufaddieren der Anzahl der profitierenden Fhzg /
                        // Stunde
                        if (!Float.isNaN(cost) || !Float.isInfinite(cost)) {
                            nbOfCars += cost;
                        } else {
                            log.debug("Invalid cost of edge: infinite or NaN!");
                        }
                    }
                } else {
                    // Hier wird am Anfang einmal die erste "echte" Kante ignoriert,
                    // damit sind die Fhzg am ersten synchronisierten Knoten nicht
                    // beruecksichtigt in der Berechnung!
                    ignoredFirstNode = true;
                }
            }
        }

        return nbOfCars;
    }

    /**
     * Method used to determine the cost of an edge when generating the OTC-node
     * path. Cost are equal to FLOW from one node to another via the Turning on
     * the PSS path.
     *
     * @return edgeCost
     */
    private float determineEdgeCost(final Vertex start, final Vertex end, final PSSPath path) {
        /**
         * Ziel: Die letzten Kosten eines Turnings sind die Kosten zwischen zwei
         * verbundenen OTCKnoten
         */
        try {
            final Edge edge = start.getEdge(end);
            if (!edge.isIntermediate()) {
                if (edge.isInverted()) {
                    return edge.getPrimaryCost();
                } else {
                    return edge.getCost();
                }
            } else {
                Vertex vertex = path.getPredecessor(start);
                return determineEdgeCost(vertex, start, path);
            }
        } catch (OTCManagerException e) {
            // Kein Vorgaenger mehr moeglich
            return 0;
        }
    }

    /**
     * Method used to determine the succeeding vertices for the currently
     * investigated PSS path.
     *
     * @param graph
     * @param theEdge
     * @return predecessors
     */
    private List<Edge> determinePredecessors(final Graph graph, final Edge theEdge) {
        // Der gefundene Pfad an Vorgaenger Knoten
        final List<Edge> preds = new LinkedList<>();
        // Die Liste der bereits besuchten OTC Knoten
        final Collection<Integer> nodeIDs = new ArrayList<>();
        int lastNodeID = -1;

        Edge edge1 = theEdge;
        boolean run = true;

        while (run) {
            // Hole den aktuellen Startknoten
            final Vertex vertex = graph.getVertex(edge1.getStartVertexID());

            if (vertex == null) {
                run = false;
            } else {
                final List<Edge> candidates = graph.getAllEdgesLeadingToVertex(vertex);

                if (candidates.isEmpty()) {
                    run = false;
                } else {
                    // Auswahl treffen, welche Kante genommen werden muss
                    float cost = -1;
                    Edge loopEdge = null;

                    // Iteriere ueber die Menge der Nachfolgerkanten
                    for (Edge edge : candidates) {
                        // Ueberpruefe, ob aktuelle Kante brauchbar
                        if (loopEdge == null) {
                            final int id = edge.destination.getID() / 100;
                            if (id == lastNodeID) {
                                // Identischer OTCNode
                                loopEdge = edge;
                                cost = loopEdge.getPrimaryCost();
                            } else {
                                // Ueberpruefung, ob auf bereits bekanntem
                                // OTCNode
                                if (!nodeIDs.contains(id)) {
                                    // Alles okay
                                    loopEdge = edge;
                                    cost = loopEdge.getPrimaryCost();
                                    // Update lastNodeID
                                    lastNodeID = id;
                                }
                            }
                        } else {
                            if (edge.getPrimaryCost() > cost) {
                                loopEdge = edge;
                                cost = loopEdge.getPrimaryCost();
                            }
                        }
                    }
                    // Auswahl getroffen - hinzufuegen
                    // && loopEdge.getPrimaryCost() > 0
                    if (loopEdge != null) {
                        final int node = loopEdge.destination.getID() / 100;
                        if (verifyNextOTCNode(loopEdge, node)) {
                            edge1 = loopEdge;
                            preds.add(edge1);
                        } else {
                            log.debug("End of search for predecessors due to conflicting edge.");
                            run = false;
                        }
                    } else {
                        // Keine gueltige Kante mehr
                        run = false;
                    }
                }

                // Pruefen ob Abbruch.
                if (edge1.getStartVertexID() < 0) {
                    run = false;
                }
            }
        }

        return preds;
    }

    /**
     * Main method to determine the possible candidates for PSSs.
     */
    private void determineStreams() {
        this.paths = new ArrayList<>();

        final Graph graph = networkGraph.clone();
        final PriorityQueue<Edge> edges = graph.getCloneOfEdgeQueue();
        float maxCost = -1;

        boolean keepRunning = true;
        // Iteriere über Liste der Edges, solange noch welche drin
        while (keepRunning) {
            final Edge edge = edges.poll();

            final List<Edge> predecessors = determinePredecessors(graph, edge);
            Collections.reverse(predecessors);

            final List<Edge> successors = determineSuccessors(edge, predecessors);

            // Setze den Strom zusammen (PSSPath): Vorgaenger - Edge - Nachfolger
            final List<Edge> path = predecessors;
            path.add(edge);
            path.addAll(successors);

            final PSSPath myPath = createPSSPath(path);

            // Bestimme die Kosten des Pfades
            final float cost = determineBenefitingCars(path);
            // Bestimmen der groessten Kosten: Wird zur Invertierung benoetigt!
            if (cost > maxCost) {
                maxCost = cost;
            }
            myPath.setCost(cost);

            paths.add(myPath);

            // Entferne die genutzten Kanten aus dem Graphen (auch die zusaetzlich eingehenden)
            for (Edge toBeRemoved : path) {
                // Ueberpruefen, ob noch zusaetzliche Eingangskanten entfernt werden koennen
                Vertex vertex = graph.getVertex(toBeRemoved.getStartVertexID());
                for (Edge deleteIt : graph.getAllEdgesLeadingToVertex(vertex)) {
                    graph.removeEdge(deleteIt);
                    edges.remove(deleteIt);
                }

                graph.removeEdge(toBeRemoved);
                edges.remove(toBeRemoved);
            }

            if (edges.isEmpty()) {
                keepRunning = false;
            }
        }

        this.highestCost = maxCost;
    }

    private PSSPath createPSSPath(List<Edge> path) {
        final Vertex start = networkGraph.getVertex(path.get(0).getStartVertexID());
        final LinkedList<Vertex> vertices = generateVertexPath(path, start);
        return new PSSPath(start, vertices);
    }

    /**
     * Method used to determine the candidates of PSS systems and fills the list
     * {@code systems}.
     * <p>
     * Methode soll jetzt PSS Systeme erstellen. Dabei besteht ein System
     * aus einer Menge an konfliktfreien gruenen Wellen.
     * <p>
     * Ausgehend von der PrioQueue wird der jeweils wichtigste Strom dem
     * System hinzugefuegt. Wenn es mit dem aktuellen Strom einen Konflikt
     * gibt, wandert er in die Gegenmenge. Wird aber aufgespalten, die
     * Kosten neu berechnet und der PrioQueue hinzugefuegt.
     * <p>
     * Sobald alle Stroeme der Queue abgearbeitet sind, werden alternative
     * Systeme gebaut. Dazu wird die Basismenge wieder vollständig gefuellt.
     * Der Ansatz ist dann identisch mit dem zuvor, basiert aber auf der
     * erstellten Gegenmenge. Dies wird so lange durchgefuehrt, bis die
     * urspruengliche Gegenmenge leer ist.
     */
    private void determineStreamSystems() {
        this.systems = new ArrayList<>(5);

        final PriorityQueue<PSSPath> counterQueue = new PriorityQueue<>();

        findMainStream(counterQueue);
        findAlternativeStreams(counterQueue);
    }

    /**
     * Hauptsystem erstellen.
     *
     * @param counterQueue
     */
    private void findMainStream(PriorityQueue<PSSPath> counterQueue) {
        PSSSystem pss = new PSSSystem(1);

        PriorityQueue<PSSPath> queue = getFilledQueue();
        log.debug("PriorityQueue size: " + queue.size());
        while (!queue.isEmpty()) {
            final PSSPath path = queue.poll();

            if (path.getNumberOfNodes() > 0) {
                if (pss.verifyAdditionalStream(path)) {
                    // Hinzufuegen zum System
                    pss.add(path);
                } else {
                    // Wenn noch nicht SPLIT - in CounterQueue
                    if (!path.isSplitted()) {
                        counterQueue.add(path);
                    }

                    if (path.getNumberOfNodes() > 2) {
                        // Split evtl. moeglich
                        final List<PSSPath> splitted = splitPath(path, pss.getConflictingVertexIDs(path));

                        // SPLITs in queue packen
                        queue.addAll(splitted.stream().filter(px -> px.getNumberOfNodes() > 1).collect(Collectors.toList()));
                    }
                }
            }

        }
        this.systems.add(pss);
    }

    /**
     * Durchlauf 2 bis x - Gegenmenge leeren.
     *
     * @param counterQueue
     */
    private void findAlternativeStreams(PriorityQueue<PSSPath> counterQueue) {
        int id = 1;
        boolean run = !counterQueue.isEmpty();
        while (run) {
            id++;

            PSSPath path = counterQueue.poll();
            PSSSystem pss = new PSSSystem(id, path);

            PriorityQueue<PSSPath> queue = getFilledQueue();
            queue.remove(path);

            while (!queue.isEmpty()) {
                path = queue.poll();

                // Ueberpruefen des aktuellen Pfades
                if (path.getNumberOfNodes() > 0) {
                    if (pss.verifyAdditionalStream(path)) {
                        // Hinzufuegen, da okay
                        pss.add(path);

                        // Falls Objekt aus CounterQueue: Entfernen!
                        if (!path.isSplitted() && counterQueue.contains(path)) {
                            counterQueue.remove(path);
                        }
                    } else {
                        // Passt nicht: Splitten!
                        final List<PSSPath> splitted = splitPath(path, pss.getConflictingVertexIDs(path));

                        // SPLITs in queue packen
                        queue.addAll(splitted.stream().collect(Collectors.toList()));
                    }
                }
            }

            systems.add(pss);

            if (counterQueue.isEmpty()) {
                return;
            }
        }
    }

    /**
     * Method used to determine the preceding vertices for the currently
     * investigated PSS path.
     *
     * @param theEdge
     * @param link
     * @return
     */
    private List<Edge> determineSuccessors(final Edge theEdge, final Collection<Edge> link) {
        final Collection<Integer> nodeIDs = findNodeIDs(link);
        int lastID = theEdge.destination.getID() / 100;
        nodeIDs.add(lastID);

        List<Edge> successors = new ArrayList<>();
        Edge myEdge = theEdge;

        boolean run = true;
        while (run) {
            // Hole den aktuellen Startknoten
            final Vertex startNode = myEdge.destination;
            if (startNode == null) {
                run = false;
            } else {
                final List<Edge> candidates = startNode.getEdges();
                if (candidates.isEmpty()) {
                    run = false;
                } else {
                    // Auswahl treffen, welche Kante genommen werden muss
                    float costCurrentEdge = -1;
                    Edge currentEdge = null;

                    for (Edge edge : candidates) {
                        int id = edge.destination.getID() / 100;

                        if (currentEdge == null) {
                            // Wenn noch keine Kante gefunden (null) & eine
                            // gueltige
                            // untersucht wird, nimm diese
                            // Verhindere bereits bekannte OTCNodes
                            if (id != lastID) {
                                if (!nodeIDs.contains(id)) {
                                    // Ich komme sonst wieder auf einen schon bekannten OTCNode
                                    currentEdge = edge;
                                    costCurrentEdge = currentEdge.getPrimaryCost();
                                    lastID = id;
                                }
                            } else {
                                // Wenn auf gleichem OTCNode, dann Kante okay
                                currentEdge = edge;
                                costCurrentEdge = currentEdge.getPrimaryCost();
                            }
                        } else {
                            // Wenn bereits eine Kante gefunden wurde & eine
                            // gueltige untersucht wird, nimm sie wenn sie besser ist.
                            // Verhindere Auswahl von bereits bekannten OTC-Knoten
                            if (edge.getPrimaryCost() > costCurrentEdge) {
                                if (id != lastID) {
                                    if (!nodeIDs.contains(id)) {
                                        currentEdge = edge;
                                        costCurrentEdge = currentEdge.getPrimaryCost();
                                        lastID = id;
                                    }
                                } else {
                                    currentEdge = edge;
                                    costCurrentEdge = currentEdge.getPrimaryCost();
                                }
                            }
                        }
                    }

                    // Auswahl getroffen - add
                    if (currentEdge != null) {
                        final int node = currentEdge.destination.getID() / 100;
                        if (verifyNextOTCNode(currentEdge, node)) {
                            myEdge = currentEdge;
                            successors.add(myEdge);
                        } else {
                            run = false;
                        }
                    } else {
                        run = false;
                    }
                }

                // Abbruch?
                if (myEdge.getStartVertexID() < 0) {
                    run = false;
                }
            }
        }

        return successors;
    }

    /**
     * Fülle die Liste der bekannten Knoten IDs.
     *
     * @param link
     * @return node ids
     */
    private Collection<Integer> findNodeIDs(Collection<Edge> link) {
        final Collection<Integer> nodeIDs = new ArrayList<>();

        if (!link.isEmpty()) {
            int id = -1;
            for (Edge edge : link) {
                int verticeIDs = edge.getStartVertexID() / 100;

                if (verticeIDs != id) {
                    id = verticeIDs;
                    nodeIDs.add(id);
                }
            }
        }
        return nodeIDs;
    }

    /**
     * Avoid using paths with less than 2 participating nodes.
     */
    private void filterPaths() {
        Iterator<PSSPath> it = paths.iterator();
        while (it.hasNext()) {
            if (it.next().getPathLength() < 2) {
                it.remove();
            }
        }
    }

    /**
     * Method creates an {@code ArrayList} of edges out of a
     * {@code LinkedList} of edges.
     *
     * @param edges The {@code LinkedList} of edges
     * @return The {@code ArrayList} of edges
     */
    private List<Edge> generateEdgesList(final Collection<Edge> edges) {
        final List<Edge> list = new ArrayList<>();
        list.addAll(edges);
        return list;
    }

    private Map<Integer, Float> generateCostList(final Collection<Edge> edges) {
        final Map<Integer, Float> map = new HashMap<>();

        if (edges.isEmpty()) {
            log.warn("Received invalid edge list: cannot generate cost list!");
            return map;
        }

        for (Edge edge : edges) {
            if (!edge.isIntermediate()) {
                // Ungueltige Kanten und Zwischenkante ignorieren
                if (!edge.isIntermediate()) {
                    float cost;
                    // Aufpassen, das nicht invertierte Kosten gerechnet werden
                    if (edge.isInverted()) {
                        cost = edge.getPrimaryCost();
                    } else {
                        cost = edge.getCost();
                    }

                    int id = (int) Math.floor(edge.getStartVertexID() / 100);
                    // Aufaddieren der Anzahl der profitierenden Fhzg / Stunde
                    if (!Float.isNaN(cost) || !Float.isInfinite(cost)) {
                        map.put(id, cost);
                        log.info("NEW ENTRY: id " + id + " cost: " + cost
                                + " start vertex ID: " + edge.getStartVertexID());
                    } else {
                        map.put(id, 0f);
                        log.debug("Invalid cost of edge: infinite or NaN!");
                    }
                }
            }
        }

        return map;
    }

    /**
     * Method used to create the INFO objects for all registered nodes. INFO
     * containes status information about PSS participation.
     * <p>
     * Methode erstellt fuer alle Knoten, die am ausgewaehlten PSS System
     * beteiligt sind, die entsprechenden PSSInfo Objekte und legt sie in
     * "pssInfos" ab. Fuer alle nicht beteiligten OTC Knoten werden
     * daraufhin ebenfalls die entsprechenden Informationen erstellt.
     */
    private void generateNodeInformation() {
        this.pssInfos = new HashMap<>(10);

        // Iteriere ueber alle PSSPfade des ausgewaehlten Systems
        if (activeSystem != null && !activeSystem.isEmpty()) {
            for (PSSPath path : activeSystem) {
                // Ueberpruefung
                if (!path.getInterNodes().isEmpty()) {
                    List<Vertex> vertices = path.getInterNodes();

                    for (int i = 0; i < vertices.size(); i++) {
                        final Vertex vertex = vertices.get(i);
                        final int name = vertex.getID();
                        if (name <= 0) {
                            log.debug("Found invalid vertex while creating PSS infos!");
                            continue;
                        }

                        OTCNodeRegion node = nodes.get(name);

                        OTCNodeRegion pred = null;
                        if (i > 0) {
                            pred = nodes.get(vertices.get(i - 1).getID());
                        }

                        OTCNodeRegion succ = null;
                        if (i < vertices.size() - 1) {
                            succ = nodes.get(vertices.get(i + 1).getID());
                        }

                        // Flags setzen: Startknoten
                        boolean start = false;
                        if (i == 0) {
                            start = true;
                        }

                        // Flags setzen: Endknoten
                        boolean end = false;
                        if (i == vertices.size() - 1) {
                            end = true;
                        }

                        // InfoObjekt erzeugen und der Liste hinzufuegen
                        PSSInfo info = new PSSInfo(node.getId(), pred, succ, start, end, true);
                        pssInfos.put(node.getId(), info);
                    }

                } else {
                    log.debug("Active PSS system contains invalid PSS path!");
                }
            }
        } else {
            log.debug("Could not generate Node-Infos: no active PSSSystem!");
        }

        nodes.values().forEach(this::createPSSInfo);
    }

    /**
     * Infos fuer alle nicht an einer PSS beteiligten Nodes erstellen.
     *
     * @param node
     */
    private void createPSSInfo(OTCNodeRegion node) {
        if (!pssInfos.containsKey(node.getId())) {
            PSSInfo info = new PSSInfo(node.getId(), false);
            pssInfos.put(node.getId(), info);
        }
    }

    /**
     * Method generates a path of OTC nodes for a given path of sub-nodes.
     * Converts 8-node representation back into OTC node representation.
     *
     * @param path
     * @return PSSPath
     */
    private PSSPath generateOTCPath(final PSSPath path) {
        final LinkedList<Vertex> nodePath = new LinkedList<>();

        if (path.getPathLength() <= 1) {
            throw new OTCManagerException("Cannot generate OTCPath: received invalid path (internodes based)!");
        } else if (path.isOTCPath()) {
            log.debug("Cannot generate OTCPath: path is already representation of OTCNodes!");
            return path;
        }

        int lastOTCNodeID = -1;
        Vertex oldVertex = null;

        if (path.getOrigin() != null && path.getOrigin().getID() > 0) {
            lastOTCNodeID = path.getOrigin().getID() / 100;
            final Vertex nextNode = new Vertex(lastOTCNodeID);
            nodePath.add(nextNode);
            oldVertex = nextNode;
        }

        Vertex start;
        Vertex end = null;

        for (Vertex vertex : path.getInterNodes()) {
            // Aktualisieren der Vertices
            start = end;
            end = vertex;

            // Ueberpruefung des Knoten
            if (vertex.getID() < 0) {
                throw new OTCManagerException("Cannot generate OTCPath - path contains invalid vertex!");
            }

            int id = vertex.getID() / 100;
            if (id != lastOTCNodeID) {
                lastOTCNodeID = id;
                vertex = new Vertex(lastOTCNodeID);

                // Bei vorheriger Kante hinzufuegen!
                if (oldVertex != null && oldVertex.getID() != id) {
                    Edge newEdge = new Edge(vertex, determineEdgeCost(start, end, path));
                    oldVertex.addEdge(newEdge);
                    log.debug("Determined cost: " + oldVertex.getEdges().get(0).getCost());
                    oldVertex = vertex;
                }
                nodePath.add(vertex);
            }
        }

        try {
            return createPSSPath(nodePath, path);
        } catch (OTCManagerException e) {
            throw new OTCManagerException("Cannot generate OTCPath - path too short");
        }
    }

    private PSSPath createPSSPath(LinkedList<Vertex> nodePath, PSSPath path) {
        if (nodePath.size() < 2) {
            throw new OTCManagerException("Path too short");
        }

        PSSPath otcPath = new PSSPath(nodePath.getFirst(), nodePath);
        otcPath.setOTCPath(true);
        otcPath.setCosts(generateCostList(path.getPath()));
        otcPath.setCost(determineBenefitingCars(path.getPath()));
        return otcPath;
    }

    /**
     * Method generates a path of vertices representing the PSSPath by starting
     * at the origin node and following the edges given by the sequence given as
     * parameter.
     *
     * @param edges  Sequence of edges
     * @param origin The origin node
     * @return The sequence of nodes defining the path
     */
    private LinkedList<Vertex> generateVertexPath(final Collection<Edge> edges, final Vertex origin) {
        final LinkedList<Vertex> vertices = new LinkedList<>();
        vertices.add(origin);

        vertices.addAll(edges.stream().map(Edge::getDestination).collect(Collectors.toList()));

        if (edges.isEmpty()) {
            log.warn("Cannot convert edge path: invalid param!");
        }

        return vertices;
    }

    /**
     * Method creates a PriorityQueue objects and fills it with all determined
     * path candidates (all possible otcPaths).
     *
     * @return queue
     */
    private PriorityQueue<PSSPath> getFilledQueue() {
        final PriorityQueue<PSSPath> queue = new PriorityQueue<>();

        if (otcPaths.isEmpty()) {
            log.error("Cannot fill the queue for stream system calculation: no paths available!");
            return queue;
        }

        queue.addAll(otcPaths.stream().map(path -> path).collect(Collectors.toList()));
        return queue;
    }

    /**
     * Method collects the node representations of registered OTCNodes.
     */
    private void receiveNodeRepresentations() {
        this.nodeRepresentations = new HashMap<>();

        for (OTCNodeRegion node : nodes.values()) {
            this.nodeRepresentations.put(node.getId(), node.getCurrentNodeRepresentation());
            node.findNeighbours();
        }

        if (nodes.isEmpty()) {
            log.error("Could not receive node representations: no registered nodes!");
        }
    }

    /**
     * Method for registration of nodes.
     *
     * @param node The OTCNode calling this method
     */
    void registerNode(final OTCNodeRegion node) {
        final int nodeID = node.getId();
        nodes.putIfAbsent(nodeID, node);
        log.info("RegionalManager registered node: " + nodeID);
    }

    private void resetValues() {
        this.highestCost = -1;
        this.nodeRepresentations = new HashMap<>(8);
        this.otcPaths = new ArrayList<>();
        this.paths = new ArrayList<>();
        this.pssInfos = new HashMap<>(10);
        this.systems = new ArrayList<>(5);
        this.activeSystem = null;
        this.networkGraph = null;
    }

    /**
     * Method used to split the given path according to the conflicting vertices
     * and to create new (sub-)paths.
     *
     * @param pssPath
     * @param vertices
     * @return
     */
    private List<PSSPath> splitPath(final PSSPath pssPath, final Collection<Integer> vertices) {
        final List<PSSPath> paths = new ArrayList<>();

        // Aufteilen in mglw. mehrere Pfade
        final Iterator<Vertex> it = pssPath.getInterNodes().listIterator();
        boolean keepOn = it.hasNext();
        boolean innerLoop = true;

        while (keepOn) {
            final LinkedList<Vertex> tmpPath = new LinkedList<>();
            // Sammle Pfad bis zum nächsten "boesen" Knoten
            while (innerLoop) {
                final Vertex vertex = it.next();
                if (!vertices.contains(vertex.getID())) {
                    tmpPath.add(vertex);
                } else {
                    // Abbruch der inneren Schleife
                    innerLoop = false;
                }
                if (!it.hasNext()) {
                    innerLoop = false;
                }
            }

            // wenn Pfad >= 2, okay, hinzufuegen; sonst: weg
            if (tmpPath.size() >= 2) {
                final PSSPath path = new PSSPath(tmpPath.getFirst(), tmpPath);
                path.setOTCPath(true);
                // Kosten bestimmen und setzen
                path.addCost(pssPath.getCosts());
                path.updateCost();
                path.invertCost(highestCost);

                path.setSplitted(true);

                paths.add(path);
            }

            if (!it.hasNext()) {
                keepOn = false;
            } else {
                innerLoop = true;
            }
        }

        return paths;
    }

    /**
     * Method verifies the next possible OTCNode on a path while building the
     * path for a PSS. It checks, whether there are other turnings on the
     * OTCNode being in conflict with the given one. Conflict means, they lead
     * to the same exit- vertex an have higher cost.
     *
     * @param edge      The chosen edge
     * @param otcNodeID The identifier of the OTCNode to check
     * @return The decision (true, if choice is fine)
     */
    private boolean verifyNextOTCNode(final Edge edge, final int otcNodeID) {
        if (edge.getStartVertexID() <= 0 || edge.getDestination() == null || otcNodeID <= 0) {
            log.warn("Cannot verify next OTCNode: invalid arguments!");
            return false;
        }

        // Bestimmen der Kosten
        float cost;
        if (edge.isInverted()) {
            cost = edge.getPrimaryCost();
        } else {
            cost = edge.getCost();
        }

        // Bestimmen der Kandidaten
        final List<Edge> edges = networkGraph.getAllEdgesForOTCNode(otcNodeID);
        if (edges.isEmpty()) {
            log.debug("Cannot verify next OTCNode: no edges found!");
            return false;
        }

        // Iteriere ueber alle Turnings und schaue, ob eine Kante mit hoeheren Kosten
        for (Edge candidate : edges) {
            float candCost;
            if (candidate.isInverted()) {
                candCost = candidate.getPrimaryCost();
            } else {
                candCost = candidate.getCost();
            }

            // Check, ob Kosten hoeher
            if (candCost > cost) {
                // Konflikt: Kante weist auf gleichen Ausgangs(Sub-)knoten
                if (candidate.destination != null && candidate.destination.getID() == edge.destination.getID()) {
                    log.debug("Found conflicting edge!");
                    return false;
                }
            }
        }

        return true;
    }
}
