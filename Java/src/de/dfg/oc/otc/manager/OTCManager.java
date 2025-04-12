package de.dfg.oc.otc.manager;

import de.dfg.oc.otc.aid.AimsunPolicyStatus;
import de.dfg.oc.otc.aid.algorithms.AbstractAIDAlgorithm;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.Recall;
import de.dfg.oc.otc.layer0.tlc.TLCException;
import de.dfg.oc.otc.layer0.tlc.fixedTimeController.FixedTimeController;
import de.dfg.oc.otc.layer0.tlc.nemaController.NEMAController;
import de.dfg.oc.otc.layer1.observer.monitoring.RawStatisticalDataContainer;
import de.dfg.oc.otc.layer2.OTCLayer2Announce;
import de.dfg.oc.otc.layer2.OptimisationTask;
import de.dfg.oc.otc.layer2.ea.EAConnection;
import de.dfg.oc.otc.layer2.ea.EAServerInterface;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.Turning;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;
import de.dfg.oc.otc.manager.gui.MainFrame;
import de.dfg.oc.otc.publictransport.PublicTransportController;
import de.dfg.oc.otc.publictransport.PublicTransportLine;
import de.dfg.oc.otc.publictransport.PublicTransportManager;
import de.dfg.oc.otc.region.DPSSManager;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingManager;
import forecasting.DefaultForecastParameters;
import org.apache.log4j.Logger;

import java.io.FilterOutputStream;
import java.io.PrintStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;


/**
 * Diese Klasse beschreibt den OTC Manager, der auf Java-Seite die Schnittstelle
 * zum Simulator darstellt. Die gesamte Kommunikation zwischen Simulator und der
 * Steuerung auf Java-Seite erfolgt �ber eine Instanz (Singleton) des OTC
 * Managers.
 */
public final class OTCManager extends Observable {
    private static final Logger log = Logger.getLogger(OTCManager.class);
    /**
     * True if this OTCManager is used by Layer 2. Prevents it from connecting to the RMI Registry.
     */
    private static boolean isLayer2Attached;
    /**
     * Static singleton instance.
     */
    private static OTCManager instance;
    /**
     * Größe eines Zeitschritts in der Simulation.
     */
    private static float simulationStepSize = 0.75f;
    /**
     * Start time of the simulation. For simulation duration.
     */
    private static long startSimulationTime;
    private static boolean slowIfLayer2Busy;
    private static Remote layer2Announce;
    private Registry rmiRegistry;
    /**
     * The latest exception.
     */
    private Exception lastException;
    /**
     * Id of the current experiment in Aimsun.
     */
    private int experiment;
    /**
     * The latest info message.
     */
    private String lastInfo = "";
    /**
     * Die aktuellen Phasen aller Junctions: [i][0]: Id der
     * {@link AimsunJunction}, [i][1]: ID der {@link de.dfg.oc.otc.manager.aimsun.Phase}.
     */
    private int[][] phases;
    /**
     * The current replicationID in Aimsun.
     */
    private int replicationID;
    /**
     * The current time of the simulation.
     */
    private float time;
    /**
     * Last received warning.
     */
    private String lastWarning = "";
    /**
     * Enthält Referenzen auf die verfügbaren EAs bzw. den diesen zugeordneten
     * Threads.
     */
    private List<EAConnection> eaList;
    /**
     * Used for some logging file names.
     */
    private String filenamePrefix;
    private boolean isAimsunFinishing;
    /**
     * Hat den Wert {@code true}, falls Ebene 2 gerade einen
     * Optimierungslauf durchführt.
     */
    private boolean isLayer2Busy;
    /**
     * Hat den Wert {@code true}, falls
     * <ul>
     * <li>dieser OTC-Manager nicht von der Ebene 2 gestartet wurde, aber
     * <li>eine Ebene 2 per RMI angemeldet ist.
     * </ul>
     */
    private boolean isLayer2Present;
    /**
     * Name der Datei, in der die LCS-Population des letzten Durchlaufs
     * gespeichert wurde. Wird verwendet, um bei Mehrtagesl�ufen die Population
     * des Vortages nach dem Restart laden zu k�nnen.
     */
    private String lastLCSPopFile = "";
    /**
     * Zeitpunkt, zu dem die von Aimsun gelieferten statistischen Daten zuletzt
     * zur�ckgesetzt wurden.
     */
    private float lastStatResetTime;
    private long lastRealTime = -1;
    /**
     * Instanz von AimsunNetwork, für die der Manager zuständig ist.
     */
    private AimsunNetwork network;
    /**
     * Interval, in dem von AIMSUN statistische Daten angeboten werden.
     */
    private float statisticsInterval;
    /**
     * Anzahl der noch zu bearbeitenden Optimierungsaufgaben.
     */
    private int taskCounter;
    /**
     * Enthält die noch zu bearbeitenden Optimierungsaufgaben.
     */
    private List<OptimisationTask> taskList;
    private int[][] result = new int[0][0];

    private OTCManager() {
        init(false);

        if (!isLayer2Attached) {
            this.eaList = new CopyOnWriteArrayList();
            this.taskList = new CopyOnWriteArrayList();
            connectRegistry();

            if (this.rmiRegistry != null) {
                layer2Announce = new OTCLayer2Announce();
                try {
                    final Remote remoteObject = UnicastRemoteObject.exportObject(layer2Announce, 0);
                    this.rmiRegistry.rebind("Layer2Announce", remoteObject);
                } catch (RemoteException e) {
                    newWarning("Anmelden der RMI-Methoden fehlgeschlagen");
                    this.lastException = e;
                    setChanged();
                    notifyObservers("New Exception");
                    this.rmiRegistry = null;
                }
            }
        } else {
            newInfo("Wird von Layer 2 verwendet, Registry nicht kontaktet");
        }
    }

    /**
     * Gibt die Instanz des Singletons OTCManager zur�ck; erzeugt ggf. eine Instanz.
     *
     * @return Singleton OTCManager
     */
    // API: getInstance
    public static OTCManager getInstance() {
        if (instance == null) {
            System.setOut(new PrintStream(new FilterOutputStream(System.out), true));
            System.setErr(new PrintStream(new FilterOutputStream(System.err), true));

            instance = new OTCManager();
            startSimulationTime = Clock.systemUTC().millis();
            EventManager.setEvent();
        }
        return instance;
    }

    public static float getSimulationStepSize() {
        return simulationStepSize;
    }

    /**
     * Setzt die Größe eines Zeitschritts in der Simulation.
     *
     * @param stepSize size of a time step (in seconds)
     */
    // API: setSimulationStepSize(F)V
    public static void setSimulationStepSize(final float stepSize) {
        simulationStepSize = stepSize;
    }

    static boolean isLayer2Attached() {
        return isLayer2Attached;
    }

    /**
     * Wird aufgerufen, wenn sich eine Aimsun-Simulation an eine bestehende
     * Instanz des OTCManagers anhängt.
     */
    public static void reInitialize() {
        OTCManager.getInstance();
        EventManager.setEvent();
    }

    public static void setLayer2Attached() {
        isLayer2Attached = true;
    }

    /**
     * This method will generate the PublicTransportControllers for the given public transport line.
     *
     * @param sectionIDs the sections the public transport line is composed of
     * @param lineID     the ID of the public transport lines
     */
    //API: setPublicTransportData([II)V
    public void setPublicTransportData(int[] sectionIDs, int lineID) {
        List<Section> sections = new ArrayList<>();
        for (int sectionID : sectionIDs) {
            Section section = this.network.getSection(sectionID);
            sections.add(section);
        }

        PublicTransportLine line = new PublicTransportLine(lineID, sections);
        PublicTransportManager.getInstance().addPublicTransportLine(lineID, line);
    }

    /**
     * This function will call a traffic light signal change in the corresponding PublicTransportController for the given detector.
     *
     * @param detectorID    the ID of the called detector
     * @param lineID        the ID of the public transport lines
     * @param detectorSpeed the current speed of the detected vehicle
     */
    //API: setPublicTransportDetectors(IIF)I
    public int setPublicTransportDetectors(int detectorID, int lineID, float detectorSpeed) {
        try {
            PublicTransportLine line = PublicTransportManager.getInstance().getPublicTransportLines().get(lineID);
            PublicTransportController controller = line.getPublicTransportController(detectorID);
            controller.detectedPublicTransport(detectorSpeed);
            return 0;
        } catch (OTCManagerException e) {
            log.warn(e.getMessage());
            return -1;
        }
    }

    /**
     * F�gt einen Centroid hinzu.
     *
     * @param id               Id des Centroids
     * @param incomingSections Ids der eingehenden Sections
     * @param outgoingSections Ids der ausgehenden Sections
     */
    // API: addCentroid(I[I[I)V
    public void addCentroid(final int id, final int[] incomingSections, final int[] outgoingSections) {
        this.network.addCentroid(id, incomingSections, outgoingSections);
    }

    /**
     * Adds a detector.
     */
    // API: addDetector(IIFFIILS)I
    public int addDetector(final int id, final int sectionId, final float posBegin, final float posEnd,
                           final int firstLane, final int lastLane, final String name) {
        try {
            this.network.addDetector(id, sectionId, posBegin, posEnd, firstLane, lastLane);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    /**
     * Fügt einen neuen EA (bzw. das entsprechende {@code EAConnection}
     * -Objekt zur Liste der verfügbaren EAs hinzu.
     *
     * @param serverInterface der neue EA (bzw. das entsprechende {@code EAConnection}
     *                        -Objekt)
     * @return die Anzahl der nach Hinzufügung verfügbaren EAs
     */
    public int addEA(final EAServerInterface serverInterface) {
        this.eaList.add(new EAConnection(serverInterface));
        return this.eaList.size();
    }

    /**
     * @param id          Id der Junction
     * @param controlType 0=Uncontrolled, 1=Fixed, <b>2=External</b>, 3=Actuated. Nur
     *                    <b>2</b> erlaubt die Kontrolle von Java aus.
     * @param name        of this junction
     * @return error code
     */
    // API: (IIS)I
    public int addJunction(final int id, final int controlType, final String name) {
        try {
            this.network.addJunction(id, controlType, name);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    /**
     * @param id          Id der Phase
     * @param interphase  Legt fest, ob die Phase eine Interphase/Zwischenphase ist
     *                    (true) oder nicht (false).
     * @param duration    Normale Phasendauer.
     * @param maxDuration Maximale Phasendauer.
     * @param minDuration Minimale Phasendauer.
     * @param junctionId  Id der Junction, zu der die neu angelegte Phase gehört.
     * @return error code
     */
    public int addPhase(final int id, final int interphase, final float duration, final float maxDuration,
                        final float minDuration, final int junctionId) {
        try {
            final boolean boolInterphase = interphase == 1;
            this.network.addPhase(id, boolInterphase, duration, maxDuration, minDuration, junctionId);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    /**
     * @param id         Id der Phase
     * @param interphase Legt fest, ob die Phase eine Interphase/Zwischenphase ist
     *                   (true) oder nicht (false).
     * @param junctionId Id der Junction, zu der die neu angelegte Phase gehört.
     * @return error code
     */
    // API: addPhase(IIFFFI)I
    public int addPhase(final int id, final int interphase, final int junctionId) {
        try {
            final boolean boolInterphase = interphase == 1;
            this.network.addPhase(id, boolInterphase, junctionId);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    /**
     * F�gt dem Netzwerk eine Section hinzu.
     *
     * @param angId      Id der Section
     * @param roadType   Id des zugehörigen RoadTypes der Section
     * @param nbTurnings Anzahl der Turnings, die von dieser Section ausgehen.
     * @param length     in meters.
     * @param destId     Id der in Fahrtrichtung folgenden Section.
     * @param speedlimit in km/h.
     * @param capacity   Basiskapazit�t des RoadTypes aus Aimsun.
     * @return -1 im Fehlerfall, sonst 0.
     */
    // API: addSection(ISIF[IFF)I
    public int addSection(final int angId, final int roadType, final int nbTurnings, final float length, final int[] destId, float speedlimit, float capacity) {
        try {
            this.network.addSection(angId, roadType, nbTurnings, length, destId, speedlimit, capacity);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    // API: (II)I
    public int addSignalGrp(final int id, final int junctionId) {
        try {
            this.network.addSignalGroup(id, junctionId);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    /**
     * F�gt einer {@link de.dfg.oc.otc.manager.aimsun.Phase} eine {@link de.dfg.oc.otc.manager.aimsun.SignalGroup} hinzu.
     *
     * @param id         Id der SignalGroup, die der Phase hinzugef�gt werden soll.
     * @param phaseId    Id der Phase, der die SignalGroup hinzugef�gt werden soll.
     * @param junctionId Id der {@link AimsunJunction}, zu der die Phase geh�rt.
     * @return -1 im Fehlerfall (Exception wurde geworfen), sonst 0.
     * @see AimsunNetwork#addSignalGrpPhase(int, int, int)
     */
    // addSignalGrpPhase(III)I
    public int addSignalGrpPhase(final int id, final int phaseId, final int junctionId) {
        try {
            this.network.addSignalGrpPhase(id, phaseId, junctionId);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    /**
     * F�gt eine neue Optimierungsaufgabe zur Liste der zu bearbeitenden
     * Aufgaben hinzu.
     *
     * @param task eine neue Optimierungsaufgabe
     * @see #taskList
     */
    public void addTask(final OptimisationTask task) {
        this.taskCounter++;
        task.setTaskID(this.taskCounter);
        this.taskList.add(task);

        newInfo(task.toString());

        setLayer2Busy(true);
        final EAConnection availableEA = getAvailableEA();
        startEA(availableEA);
    }

    /**
     * @param junctionId   Id der Junction, zu der das Turning gehört.
     * @param signalGrpId  Id der SignalGroup, der das Turning zugeordnet wird.
     * @param sectionInId  Id der Section, die Ausgangspunkt des Turnings ist.
     * @param sectionOutId Id der Section, die Endpunkt des Turnings ist.
     * @return error code
     */
    // API: (IIII)I
    public int addTurning(final int junctionId, final int signalGrpId, final int sectionInId, final int sectionOutId) {
        try {
            this.network.addTurning(junctionId, signalGrpId, sectionInId, sectionOutId);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    /**
     * Jeden Step durch Aimsun aufgerufen. Nimmt "Rohdaten" f�r Turnings an, wie
     * sie von Aimsun ausgegeben werden. Diese m�ssen noch (unter Verwendung des
     * Zeitpunkts des letzten Resets) aufbereitet werden.
     *
     * @param sectionInId    Id der Section, die in das zugeh�rige {@link Turning} f�hrt
     * @param sectionOutId   Id der Section, die aus dem zugeh�rigen {@link Turning}
     *                       herausf�hrt
     * @param time           Die aktuelle Zeit
     * @param flow           Verkehrsst�rke veh/h
     * @param travelTime     Travel Time : Average sec
     * @param delayTime      Delay Time : Average sec
     * @param stopTime       Stop Time : Average sec
     * @param queueLength    Average Queue Length (veh)
     * @param numStops       Number of Stops (#/Veh)
     * @param averageSpeed   Speed : Average km/h
     * @param speedDeviation Speed : Deviation km/h
     * @param maxQueue       Maximum Queue Length (veh)
     * @return {@code -1}, if exception is thrown, otherwise {@code 0}
     */
    // API: addTurningRawStatisticalData(IIFIFFFFFFFF)I
    public int addTurningRawStatisticalData(final int sectionInId, final int sectionOutId, final float time,
                                            final int flow, final float travelTime, final float delayTime, final float stopTime,
                                            final float queueLength, final float numStops, final float averageSpeed, final float speedDeviation,
                                            final float maxQueue) {
        try {
            Turning turning = network.getTurning(sectionInId, sectionOutId);

            final RawStatisticalDataContainer rawData = new RawStatisticalDataContainer(time, this.lastStatResetTime, flow,
                    travelTime, delayTime, stopTime, queueLength, numStops, averageSpeed, speedDeviation, maxQueue);
            turning.addRawStatisticalDataEntry(rawData);

            if (DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE) {
                turning.getFlowForecaster().addValueForForecast(time, flow);
            }
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }

        return 0;
    }

    /**
     * Gets the flow of a {@link Section} from AIMSUN.
     *
     * @param sectionID identifier of the {@link Section}
     * @param flow      of the {@link Section} per tick
     * @return {@code -1}, if exception is thrown, otherwise {@code 0}
     */
    // API: addSectionRawFlow(II)I
    public int addSectionRawFlow(final int sectionID, final int flow) {
        if (time < statisticsInterval) {
            return 0;
        }

        final Section section = network.getSection(sectionID);
        if (section == null) {
            return -1;
        }

        if (!DefaultParams.ROUTING_PROTOCOL.equals("NONE")) {
            section.getFlowForecaster().addValueForForecast(time, flow);
        }
        section.setFlow(flow);

        return 0;
    }

    /**
     * Test, ob sich ein EA im Leerlauf befindet und betraut diesen ggf. mit
     * der nächsten Optimierungsaufgabe.
     */
    public void checkEAStatus() {
        if (!this.taskList.isEmpty()) {
            final EAConnection availableEA = getAvailableEA();
            if (availableEA != null) {
                startEA(availableEA);
            }
        }
    }

    /**
     * Check, ob sich alle EAs im Leerlauf befinden, so dass Ebene 0/1 beschleinigt laufen kann.
     */
    public void checkLayer2Busy() {
        for (EAConnection connection : eaList) {
            if (!connection.isEaReady()) {
                return;
            }
        }

        setLayer2Busy(false);
    }

    private void connectRegistry() {
        try {
            this.rmiRegistry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        } catch (RemoteException e) {
            newInfo("Could not create RMI-Registry. Test, if Registry is already running.");
            try {
                this.rmiRegistry = LocateRegistry.getRegistry();
            } catch (RemoteException e1) {
                newWarning("Start of the RMI-Registry failed. Exception: ");
                this.lastException = e1;
                setChanged();
                notifyObservers("New Exception");
                this.rmiRegistry = null;
                return;
            }
        }
        newInfo("Connection with Registry established.");
    }

    /**
     * Erzeugt ein neues Netzwerk, das dann dem Manager zugeordnet wird. Dieses
     * Netzwerk ist initial.
     *
     * @param networkName Name des Netzwerks.
     * @return Erzeugtes Netzwerk.
     */
    // API: createNetwork(S)
    public AimsunNetwork createNetwork(String networkName) {
        if (networkName.lastIndexOf(".ang") > 0) {
            networkName = networkName.substring(0, networkName.lastIndexOf(".ang"));
        }
        this.network = new AimsunNetwork(networkName);

        if (countObservers() < 1) {
            final MainFrame gui = new MainFrame();
            gui.setTitle("OTC Manager relaunch");
            gui.setVisible(true);
        }

        setChanged();
        notifyObservers("Network loaded");
        return this.network;
    }

    // API: finalizeInit()V
    public void finalizeInit() {
        setChanged();
        notifyObservers("Network initialised");

        if (DefaultParams.AID_ACTIVE) {
            this.network.getNodes().forEach(OTCNode::initialiseAIDComponent);
            this.network.getNodes().forEach(OTCNode::finalizeAIDComponent);
        }
    }

    /**
     * Erzeugt einen Festzeit-Controller f�r die angegebene {@link AimsunJunction}.
     *
     * @param junctionId Id der {@link AimsunJunction}
     * @param phaseIds   Ids der zu ber�cksichtigenden Phasen. Reihenfolge in diesem
     *                   Array legt die Reihenfolge der Phasen im Controller fest.
     * @param maxGreens  Die Freigabezeiten der Phasen, Zuordnung �ber Reihenfolge im
     *                   Array.
     * @throws OTCManagerException wenn die {@link AimsunJunction}-Id ung�ltig ist oder die
     *                             angegebene {@link AimsunJunction} nicht extern gesteuert
     *                             wird.
     */
    public void generateFTCforJunction(final int junctionId, final int[] phaseIds, final float[] maxGreens)
            throws OTCManagerException {
        final AimsunJunction junction = this.network.getJunction(junctionId);
        if (junction == null) {
            this.lastWarning = this.lastWarning.concat("generate FTC: Junction-Id " + junctionId + " is invalid.");
            setChanged();
            notifyObservers("New Warning");
            return;
        }

        if (!junction.isControlled()) {
            this.lastWarning = this.lastWarning.concat("generate FTC: Junction-Id " + junctionId
                    + " is not externally controlled.");
            setChanged();
            notifyObservers("New Warning");
            return;
        }

        try {
            List<Integer> phases = new ArrayList<>();
            Collections.addAll(phases, Arrays.stream(phaseIds).boxed().toArray(Integer[]::new));

            final FixedTimeController tlc = new FixedTimeController(maxGreens, junction, phases);
            junction.setTrafficLightController(tlc);
        } catch (TLCException e) {
            this.lastException = e;
            setChanged();
            notifyObservers("New Exception");
        }
    }

    /**
     * Erzeugt einen NEMA-Controller f�r die angegebene
     * {@link AimsunJunction}.
     *
     * @param junctionId Id der {@link AimsunJunction}
     * @param phaseIds   Ids der zu ber�cksichtigenden Phasen. Reihenfolge in diesem
     *                   Array legt die Reihenfolge der Phasen im Controller fest.
     * @param maxGreens  Die Freigabezeiten der Phasen, Zuordnung �ber Reihenfolge im
     *                   Array.
     * @throws OTCManagerException wenn die {@link AimsunJunction}-Id ung�ltig ist oder die
     *                             angegebene {@link AimsunJunction} nicht extern gesteuert
     *                             wird.
     */
    public void generateNEMAforJunction(final int junctionId, final int[] phaseIds, final float[] maxGreens,
                                        final float[] maxInitialGreenTimes, final int[] iRecalls,
                                        final float[] maximumGaps, final float[] minimumGreenTimes,
                                        final float[] extensionSteps, final float[] redDelays)
            throws OTCManagerException {
        final AimsunJunction junction = this.network.getJunction(junctionId);
        if (junction == null) {
            this.lastWarning = this.lastWarning.concat("generateNEMAforJunction: Junction-Id " + junctionId
                    + " is invalid.");
            setChanged();
            notifyObservers("New Warning");
            return;
        }

        if (!junction.isControlled()) {
            this.lastWarning = this.lastWarning.concat("generate NEMA: Junction-Id " + junctionId
                    + " is not externally controlled.");
            setChanged();
            notifyObservers("New Warning");
            return;
        }

        try {
            List<Integer> phases = new ArrayList<>();
            Collections.addAll(phases, Arrays.stream(phaseIds).boxed().toArray(Integer[]::new));

            final Recall[] recalls = new Recall[iRecalls.length];
            for (int i = 0; i < iRecalls.length; i++) {
                recalls[i] = Recall.getRecallForId(iRecalls[i]);
            }
            final NEMAController nema = new NEMAController(maxGreens, junction, phases, recalls, maxInitialGreenTimes,
                    maximumGaps, minimumGreenTimes, extensionSteps, redDelays);
            junction.setTrafficLightController(nema);
        } catch (TLCException e) {
            this.lastException = e;
            setChanged();
            notifyObservers("New Exception");
        }
    }

    /**
     * Gibt einen freien EA (bzw. dessen {@code EAConnection} zurück.
     *
     * @return ein freier EA, falls verfügbar; sonst {@code null}
     */
    private EAConnection getAvailableEA() {
        for (EAConnection connection : this.eaList) {
            if (connection.isEaReady()) {
                return connection;
            }
        }
        return null;
    }

    // API: getPhases ()[[I
    public int[][] getPhases() throws OTCManagerException {
        List<AimsunJunction> junctions = network.getControlledJunctions();

        try {
            final int size = phases.length;
            for (int i = 0; i < size; i++) {
                AimsunJunction junction = junctions.get(i);
                this.phases[i][0] = junction.getId();
                this.phases[i][1] = junction.getCurrentPhase();
            }
        } catch (IndexOutOfBoundsException | NoSuchElementException ioobe) {
            throw new OTCManagerException("Anzahl der extern gesteuerten Junctions hat sich geändert.");
        }

        updateDateAndSleepTime();

        return this.phases;
    }

    /**
     * Slow down the simulation.
     */
    private void updateDateAndSleepTime() {
        if (slowIfLayer2Busy && this.isLayer2Busy) {
            final long currentTime = Calendar.getInstance().getTimeInMillis();
            final long sleepTime = this.lastRealTime - currentTime + (long) (simulationStepSize * 1000);
            if (sleepTime > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }
        }

        this.lastRealTime = Calendar.getInstance().getTimeInMillis();
    }

    /**
     * Liefert die Id der aktuellen Replication der Simulation.
     */
    public int getReplicationID() {
        return this.replicationID;
    }

    /**
     * Setzt die Id der aktuellen Replication der Simulation.
     */
    // API: setReplicationID(I)V
    public void setReplicationID(final int replicationID) {
        this.replicationID = replicationID;

        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        this.filenamePrefix = network.getName() + "_" + experiment + "_" + this.replicationID + "_"
                + date.getYear() + date.getMonth() + date.getDayOfMonth() + "_" + time.getHour() + time.getMinute();

        loadLastPopulationFile();
    }

    public List<EAConnection> getEaList() {
        return this.eaList;
    }

    /**
     * Get the latest exception.
     */
    public Exception getException() {
        return this.lastException;
    }

    /**
     * Gibt einen Pr�fix f�r Logdateien zur�ck, der u.a. die simulierte
     * Replication sowie den Simulationszeitpunkt enth�lt.
     *
     * @return Präfix für Logdateien
     */
    public String getFilenamePrefix() {
        return this.filenamePrefix;
    }

    /**
     * Returns the latest info message and clears it.
     */
    public String getInfo() {
        final String info = this.lastInfo;
        this.lastInfo = "";
        return info;
    }

    /**
     * Gibt das Netzwerk-Objekt zurück, das diesem OTCManager zugeordnet ist.
     *
     * @return Instanz vom AimsunNetwork dieses Managers.
     */
    public AimsunNetwork getNetwork() {
        return this.network;
    }

    /**
     * Communicate routing information to AIMSUN.
     */
    // API: getRoutingData()[[I
    public int[][] getRoutingData() {
        if (this.time < RoutingManager.getInstance().getNextProtocolRun()) {
            return new int[0][0];
        }

        result = new int[0][0];
        int[][] table;

        for (AimsunJunction junction : this.network.getControlledJunctions()) {
            RoutingComponent rc = junction.getNode().getRoutingComponent();

            // Routing active?
            if (rc != null) {
                table = rc.getRoutingTable();
                if (table.length != 0) {
                    result = joinArrays(result, table);
                }
            }
        }

        return result;
    }

    /**
     * Liefert die aktuelle Zeit der Simulation in seconds.
     *
     * @return seconds since simulation start
     */
    public float getTime() {
        return this.time;
    }

    /**
     * Setzt die aktuelle Zeit.
     *
     * @param time Aktuelle Zeit in der Simulation
     */
    // API: setTime(F)V
    public void setTime(final float time) {
        this.time = time;

        DPSSManager.getInstance().executePSS(time);
        RoutingManager.getInstance().processRoutingProtocol(time);
        DisturbanceManager.getInstance().step(time);
        PublicTransportManager.getInstance().reset();

        if (!isLayer2Attached) {
            setChanged();
            notifyObservers("Time updated");
        }
    }

    /**
     * Returns the latest warning message and clears it.
     */
    public String getWarning() {
        final String warning = this.lastWarning;
        this.lastWarning = "";
        return warning;
    }

    public int getSystemSeed() {
        return this.replicationID;
    }

    /**
     * Initialisiert den Manager.
     *
     * @param isRestart Wird auf {@code true} gesetzt, wenn ein Restart erfolgt
     *                  (z.B. zwischen zwei aufeinanderfolgenden Optimierungsl�ufen
     *                  f�r das selbe Modell). Dann werden einige Parameter wie die
     *                  Information, ob Layer2 vorhanden ist, �bernommen, statt
     *                  zur�ckgesetzt zu werden.
     */
    private void init(final boolean isRestart) {
        this.time = -1;
        this.replicationID = -1;
        this.lastInfo = "";
        this.lastWarning = "";
        this.network = null;

        DefaultForecastParameters.readPropertyFile("java\\");

        DPSSManager.getInstance().initParameters();

        // Besondere Anpassungen f�r Neustart
        if (!isRestart) {
            // Bei einem Neustart (restart == false) kann noch kein Layer2
            // angebunden sein. Um dem Anwender Zeit zu geben, Layer2 zu
            // starten, wird isLayer2Busy auf true gesetzt.
            this.isLayer2Busy = true;
            this.isLayer2Present = false;

            slowIfLayer2Busy = DefaultParams.L2_SLOW_IF_LAYER2_BUSY;
        }
    }

    // API: ()V
    public void initJunctions() {
        try {
            this.network.initSections();
            this.network.initJunctions();
            this.phases = new int[network.getNumControlledJunctions()][2];
        } catch (Exception e) {
            lastException = e;
            setChanged();
            notifyObservers("New Exception");
        }
    }

    /**
     * Initialisiert die Subdetektoren, erzeugt f�r jeden Subdetektor eine
     * fortlaufende Id, baut eine Liste mit diesen Ids auf. Ruft direkt die
     * gleichlautende Methode der Klasse AimsunNetwork auf.
     *
     * @see AimsunNetwork#initSubDetectors()
     */
    // API: initSubDetectors()V
    public void initSubDetectors() {
        try {
            network.initSubDetectors();
        } catch (Exception e) {
            lastException = e;
            setChanged();
            notifyObservers("New Exception");
        }
    }

    public boolean isLayer2Busy() {
        return isLayer2Busy;
    }

    public void setLayer2Busy(final boolean busy) {
        this.isLayer2Busy = busy;
        if (!busy && this.isAimsunFinishing) {
            this.isAimsunFinishing = false;
            EventManager.setEvent();
        }
    }

    public boolean isLayer2Present() {
        return isLayer2Present;
    }

    public void setLayer2Present(final boolean presence) {
        this.isLayer2Present = presence;
        setChanged();
        notifyObservers("Layer2 Present");
    }

    /**
     * Concats two arrays into one.
     *
     * @return concated array
     */
    private int[][] joinArrays(final int[][] array, final int[][] extension) {
        int[][] join = new int[array.length + extension.length][3];

        System.arraycopy(array, 0, join, 0, array.length);
        System.arraycopy(extension, 0, join, array.length, extension.length);

        return join;
    }

    private void loadLastPopulationFile() {
        if (lastLCSPopFile.isEmpty()) {
            for (AimsunJunction junction : network.getControlledJunctions()) {
                final OTCNode node = junction.getNode();

                String suffix = node.getTLCSelector().getClass().getName();
                suffix = suffix.substring(suffix.lastIndexOf(".") + 1);
                final String popfile = lastLCSPopFile + "_" + node.getId() + "_" + suffix + ".txt";
                node.getTLCSelector().loadMappingFromFile(popfile);
            }
        } else {
            newInfo("Please start Layer 2");
        }
    }

    // API: newInfo
    public void newInfo(final String text) {
        this.lastInfo = this.lastInfo.concat(text);
        setChanged();
        notifyObservers("New Info");
    }

    // API: newWarning
    public void newWarning(final String text) {
        this.lastWarning = this.lastWarning.concat(text);
        setChanged();
        notifyObservers("New Warning");
    }

    /**
     * Wird von Aimsun-API in AAPIFinish aufgerufen, um festzustellen, ob auf
     * Ebene 2 noch eine Optimierung l�uft. Falls ja, wird 0 zur�ckgegeben und
     * das Flag isAimsunFinishing gesetzt, was dazu f�hrt, dass nach Abschluss
     * des letzten Optimierungslaufs ein Windows-Event ausgel�st wird, auf das
     * Aimsun wartet. Falls keine Optimierung mehr l�uft, wird 1 zur�ckgegeben
     * und Aimsun kann den Lauf direkt beenden.
     *
     * @return 0 wenn noch eine Optimierung l�uft, sonst 1.
     */
    // API: readyForFinish()I
    public int readyForFinish() {
        AbstractAIDAlgorithm.finishAllInstances();
        if (isLayer2Present && isLayer2Busy) {
            isAimsunFinishing = true;
            return 0;
        }

        long duration = (Clock.systemUTC().millis() - startSimulationTime) / 1000;
        log.info("Simulation duration: " + duration + " seconds");

        return 1;
    }

    /**
     * Setzt den Zeitpunkt, zu dem die statistischen Daten von Aimsun zuletzt
     * zur�ckgesetzt wurden.
     *
     * @param time Zeitpunkt des Reset
     */
    // API: resetStatisticsTime(F)V
    public void resetStatisticsTime(final float time) {
        this.lastStatResetTime = time;
    }

    /**
     * Wird aufgerufen, wenn eine Simulation neu gestartet werden soll, um zu
     * bestimmen, ob das Netzwerk schon �bertragen wurde oder nicht. Informiert
     * die Observer �ber den Restart, so da� ggf. n�tige Initialisierungen
     * vorgenommen werden k�nnen.
     *
     * @return 0: Kein Netzwerk vorhanden, 1: Netzwerk vorhanden.
     */
    // API: restart()I
    public int restart() {
        if (this.network == null) {
            return 0;
        }

        this.lastLCSPopFile = this.filenamePrefix;

        if (DefaultParams.L2_LOG_LCS_MAPPING) {
            saveMappingToFile();
        }

        this.network.restart();

        setChanged();
        notifyObservers("Simulation restarted");

        this.lastWarning = lastWarning.concat("Simulation restarted");
        setChanged();
        notifyObservers("New Warning");

        init(true);

        return 0;
    }

    /**
     * Speichert die Zuordnung von TLCs zu Verkehrssituationen (z.B. in Form von
     * LCS-Regeln) f�r alle Knoten in Dateien.
     */
    private void saveMappingToFile() {
        for (AimsunJunction junction : this.network.getControlledJunctions()) {
            final OTCNode node = junction.getNode();
            if (node == null) {
                continue;
            }

            String suffix = node.getTLCSelector().getClass().getName();
            suffix = suffix.substring(suffix.lastIndexOf(".") + 1);
            final String filename = "logs/" + this.filenamePrefix + "_" + node.getId() + "_" + suffix + ".txt";
            node.getTLCSelector().saveMappingToFile(filename);
        }
    }

    // API: setExperiment(I)V
    public void setExperiment(final int experiment) {
        this.experiment = experiment;
    }

    // API: setDetectorCapabilities(IZZZZZZZ)I
    public int setDetectorCapabilities(final int detectorId, final boolean count, final boolean presence,
                                       final boolean speed, final boolean occupancy, final boolean headway, final boolean density,
                                       final boolean equippedVehicle) {
        try {
            this.network.setDetectorCapabilities(detectorId, count, presence, speed, occupancy, headway, density,
                    equippedVehicle);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    // API: setDetectorDestinations(I[I)I
    public int setDetectorDestinations(final int id, final int[] destinationIds) {
        try {
            this.network.setDetectorDestinations(id, destinationIds);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }
        return 0;
    }

    /**
     * Setzt den Wert aller SubDetectors (Features) des angegebenen Detectors.
     *
     * @param detectorId Id des Detectors, dessen SubDetectors aktualisiert werden
     *                   sollen.
     * @param values     Array mit den Werten f�r alle SubDetectors. Die Anzahl der
     *                   Werte im Array mu� gleich DetectorCapabilities.NUM sein, d.h.
     *                   auch f�r nicht aktive Features mu� ein Wert �bergeben werden.
     * @return {@code -1}, if exception is thrown, otherwise {@code 0}
     */
    // API: setDetectorValue(I[F)I
    public int setDetectorValue(final int detectorId, final float[] values) {
        final Detector detector = this.network.getDetectors().get(detectorId);
        if (detector == null) {
            this.lastWarning = this.lastWarning
                    .concat("setDetectorValue: Detector-Id " + detectorId + "invalid.");
            setChanged();
            notifyObservers("New Warning");
            return -1;
        }

        try {
            detector.setValues(this.time, values);
        } catch (OTCManagerException ome) {
            this.lastException = ome;
            setChanged();
            notifyObservers("New Exception");
            return -1;
        }

        return 0;
    }

    /**
     * Set the number of lanes for a {@link Section}.
     *
     * @param sectionId identifier of {@link Section}
     * @param nbLanes   of the {@link Section}
     * @return {@code -1}, if exception is thrown, otherwise {@code 0}
     */
    // API: setSectionNbLanes(II)I
    public int setSectionNbLanes(final int sectionId, final int nbLanes) {
        final Section section = this.network.getSection(sectionId);
        if (section == null) {
            return -1;
        }
        section.setNumberOfLanes(nbLanes);
        return 0;
    }

    /**
     * Setzt das Intervall, in dem von Aimsun statistische Daten gesammelt
     * werden.
     *
     * @param interval Zeitintervall, in dem Daten gesammelt werden.
     */
    // API: setStatisticsInterval(F)V
    public void setStatisticsInterval(final float interval) {
        this.statisticsInterval = interval;
    }

    /**
     * Setzt die aktuelle Zeit.
     *
     * @param time Aktuelle Zeit in der Simulation
     */
    public void setTimeForTests(final float time) {
        this.time = time;
    }

    /**
     * Übergibt eine Aimsun Policy ID, deren Status (aktiv/inaktiv) abgefragt werden soll
     *
     * @return Die Policy ID
     */
    // API: getPolicyToQuery()I
    public int getPolicyToQuery() {
        // this method is called every update cycle from the API in a loop, which breaks when this method returns -1
        return AimsunPolicyStatus.getNextPolicy();
    }

    /**
     * Setzt den Status für eine Policy ID auf Status aktiv/inaktiv
     *
     * @param policyID Die Policy ID
     * @param status   Policy aktiv/inaktiv
     */
    // API: setPolicyStatus(IZ)V
    public void setPolicyStatus(int policyID, boolean status) {
        AimsunPolicyStatus.setStatus(policyID, status);
    }

    /**
     * Definiert die jeweils erste und letzte Fahrspur, die einem
     * {@link Turning} zugeordnet ist (ein- und ausgangsseitig).
     *
     * @param sectionInId  Id der InSection {@link Section#getId()}
     * @param sectionOutId Id der OutSection {@link Section#getId()}
     * @param flo          Erste Spur der Quellsection, die zu diesem {@link Turning}
     *                     gehört
     * @param llo          Letzte Spur der Quellsection, die zu diesem {@link Turning}
     *                     gehört
     * @param fld          Erste Spur der Zielsection, die zu diesem {@link Turning}
     *                     gehört
     * @param lld          Letzte Spur der Zielsection, die zu diesem {@link Turning}
     *                     gehört
     * @return 0 bei Erfolg, -1 wenn die InSectionId ungültig ist, -2 wenn der
     * InSection kein {@link Turning} nach OutSection zugeordnet ist.
     */
    // API: setTurningLanes(IIIIII)I
    public int setTurningLanes(final int sectionInId, final int sectionOutId, final int flo, final int llo,
                               final int fld, final int lld) {
        final Section inSection = this.network.getSection(sectionInId);
        if (inSection == null) {
            return -1;
        }

        final Turning turning = inSection.getTurningMap().get(sectionOutId);
        if (turning == null) {
            return -2;
        }

        turning.setLanes(flo, llo, fld, lld);
        return 0;
    }

    /**
     * Übergibt eine zu bearbeitende Optimierungsaufgabe an den als Parameter
     * angegebenen EA, der verfügbar sein muss.
     *
     * @param ea der zu startende EA
     * @see #getAvailableEA()
     */
    private void startEA(final EAConnection ea) {
        if (ea != null) {
            final OptimisationTask task = this.taskList.remove(this.taskList.size() - 1);

            ea.setEaReady(false);
            ea.setOptimisationTask(task);
            final Thread thread = new Thread(ea);
            thread.start();
        }
    }

    /**
     * Reads a string that is printed to AKIPrintString() at the end of each update cycle
     * @return console string
     */
    // API: "()Ljava/lang/String;"
    public String getPrintStreamOutput()
    {
        return OutPrintLn.getPrintStreamOutput();
    }
}
