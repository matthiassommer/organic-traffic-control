package de.dfg.oc.otc.manager;

import de.dfg.oc.otc.aid.AIDModule;
import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.*;
import de.dfg.oc.otc.layer0.tlc.fixedTimeController.FixedTimeController;
import de.dfg.oc.otc.layer0.tlc.fixedTimeController.FixedTimeRecallController;
import de.dfg.oc.otc.layer0.tlc.nemaController.NEMAController;
import de.dfg.oc.otc.layer1.Layer1Exception;
import de.dfg.oc.otc.layer1.controller.AbstractTLCSelector;
import de.dfg.oc.otc.layer1.controller.ClassifierException;
import de.dfg.oc.otc.layer1.controller.xcscic.XCSCIC;
import de.dfg.oc.otc.layer1.controller.xcsic.XCSIC;
import de.dfg.oc.otc.layer1.controller.xcst.XCST;
import de.dfg.oc.otc.layer1.observer.*;
import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticsCapabilities;
import de.dfg.oc.otc.layer2.OptimisationTask;
import de.dfg.oc.otc.manager.aimsun.*;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;
import de.dfg.oc.otc.aid.disturbance.Disturbance;
import de.dfg.oc.otc.aid.disturbance.DisturbanceTLCCreator;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingComponent;
import forecasting.DefaultForecastParameters;

import java.io.File;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diese Klasse bildet einen "Node" ab, also die vollst�ndige an einem Knoten
 * vorhandene Funktionalit�t, bestehend aus den Ebenen 0 bis 2. Wird nicht
 * Controller genannt, um nicht mit dem Controller eines Observer/Controllers
 * verwechselt zu werden.
 *
 * @author rochner
 */
public class OTCNode {
    /**
     * Map, die jedem Turning den aktuellen Fluss zuordnet. Ein Turning wird
     * dabei durch einen String der Form "from_section_id;to_section_id"
     * repr�sentiert.
     */
    private final Map<String, Float> turnToFlow = new HashMap<>();
    /**
     * The component for the incident detection.
     */
    private AIDModule aidModule;
    /**
     * Die Kreuzung, f�r die dieser Node zust�ndig ist.
     */
    private final AimsunJunction junction;
    /**
     * Der Observer auf Ebene 1.
     */
    private final Layer1Observer layer1Observer;
    /**
     * Defines the id of the according traffic light controller.
     */
    private int controller;
    /**
     * Flag indicating if node takes part in a PSS.
     */
    private boolean partOfPSS;
    /**
     * Gibt die aktuelle Kapazit�t des Knotens in Abh�ngigkeit von den default
     * Phasendauern an (veh/h).
     */
    private float capacity;
    /**
     * Gibt den default Wert für die Kapazität an.
     */
    private float defaultCapacity;
    /**
     * Zeitraum, �ber den der Mittelwert f�r die Bewertung gebildet wird.
     */
    private float evaluationInterval = -1;
    /**
     * Schalter, mit dem die Ebene 1 abgeschaltet werden kann.
     */
    private boolean lcsActive;
    /**
     * Anzahl Zyklen, die zwischen zwei aufeinanderfolgenden Bewertungen liegen
     * sollten.
     */
    private int minCyclesBetweenEvaluations;
    /**
     * Anzahl Zyklen, die ein neuer TLC mindestens aktiv bleibt, bevor er
     * ausgetauscht werden kann.
     */
    private int minCyclesChangeDelay;
    /**
     * Anzahl an Umlaufzeiten, die das evaluationInterval ergeben sollen. (Ein
     * gr��erer Wert verringert die Schwankungen in der Situationserkennung.)
     */
    private int numCyclesEvaluationInterval;
    /**
     * Routingeinheit, die f�r diesen Knoten zust�ndig ist.
     */
    private RoutingComponent routingComponent;
    /**
     * Zeitpunkt, zu dem zuletzt der TLC ausgetauscht wurde.
     */
    private float timeLastTLCChange = -1;
    /**
     * Zeitpunkt, zu dem die n�chste Bewertung erfolgen soll bzw. fr�hestens
     * sinnvoll w�re.
     */
    private float timeNextEvaluation = -1;
    /**
     * Zeitpunkt, zu dem fr�hestens ein Wechsel des aktuellen TLC erfolgen kann.
     */
    private float tlcChangeDelay = -1;
    /**
     * Der Selektionsmechanismus (z.B. das LCS) auf Ebene 1.
     */
    private AbstractTLCSelector tlcSelector;
    /**
     * Creates a TLC for disturbance situations.
     */
    private final DisturbanceTLCCreator tlcChanger;
    private final SituationAnalyser situationAnalyser;

    public OTCNode(final AimsunJunction junction, final Attribute attribute) throws OTCManagerException {
        this.junction = junction;
        this.layer1Observer = new Layer1Observer(junction);
        initialiseDefaultParams();

        this.tlcChanger = new DisturbanceTLCCreator(this);

        if (junction.isControlled()) {
            chooseTLCSelector(attribute);

            if (!junction.hasActiveTLC()) {
                generateTLC();
            }

            this.tlcSelector.setDefaultTLCParams(junction.getActiveTLC().getParameters());
            this.evaluationInterval = junction.getActiveTLC().getCycleTime() * numCyclesEvaluationInterval;
            this.lcsActive = DefaultParams.L1_ACTIVE;

            communicateNewCycleTimeToForecastComponents(this.minCyclesChangeDelay * junction.getActiveTLC().getCycleTime());
        } else {
            final List<Phase> phases = junction.getPhases();
            int cycleTime = 0;

            for (Phase phase : phases) {
                float phaseDuration = phase.getDefaultMaximalDuration();
                if (!Float.isNaN(phaseDuration)) {
                    cycleTime += phaseDuration;
                }
            }
            this.evaluationInterval = cycleTime * numCyclesEvaluationInterval;
        }

        situationAnalyser = new SituationAnalyser(this);
    }

    public SituationAnalyser getSituationAnalyser() {
        return situationAnalyser;
    }

    /**
     * Methode berechnet und aktualisiert die Kapazit�t des Knotens bei jedem
     * Wechsel eines TrafficLightControllers.
     */
    private void setCapacityOfNode() {
        float simpleCapacity = defaultCapacity;

        final List<Phase> phases = junction.getPhases();
        if (!phases.isEmpty()) {
            float duration = 0;
            for (Phase phase : phases) {
                duration += phase.getDefaultDuration();
            }

            final float averageDuration = duration / phases.size();
            if (Float.isNaN(averageDuration) || averageDuration < 0) {
                // ERROR: set default value
                simpleCapacity = 2400;
                System.out.println("Result of capacity calculation is invalid!");
            } else if (averageDuration < 6) {
                // Mindestfreigabedauer ist 5 sec
                simpleCapacity = 3000;
            } else if (averageDuration <= 10) {
                simpleCapacity = 2400 + 150 * (10 - averageDuration);
            } else {
                simpleCapacity = 2000;
            }
        }

        if (simpleCapacity > 1) {
            this.capacity = simpleCapacity * getLanesFactor();
        } else {
            // Es gibt wohl Sections ohne lanes -> ERROR
            this.capacity = this.defaultCapacity;
        }
    }

    private float getLanesFactor() {
        // TODO Zur Zeit wird keine Verbindung zwischen den Signalphasen
        // und den Spuren hergestellt.
        final List<Section> sectionsIn = junction.getInSections();
        final List<Section> sectionsOut = junction.getOutSections();

        int counter = 0;
        int numberLanes = 0;
        for (Section section : sectionsIn) {
            numberLanes += section.getNumberOfLanes();
            counter++;
        }
        final float factor1 = numberLanes / counter;

        counter = 0;
        numberLanes = 0;
        for (Section section : sectionsOut) {
            numberLanes += section.getNumberOfLanes();
            counter++;
        }
        final float factor2 = numberLanes / counter;

        // Wenn # Ein- und Ausg�nge unterschiedlich -> Engpass nehmen
        return Math.min(factor1, factor2);
    }

    /**
     * Erzeugt aus dem �bergebenen Parametersatz einen passenden
     * TrafficLightController und tauscht den TLC der Kreuzung, f�r die dieser
     * Node zust�ndig ist, gegen diesen neu erzeugten aus.
     *
     * @param parameters Parameter, die den TLC beschreiben.
     * @throws de.dfg.oc.otc.layer1.Layer1Exception wenn der aktuelle TLC noch in der Totzeit ist, der �bergebene
     *                                              Parametersatz nicht zur Junction passt oder sonstige Fehler
     *                                              bzw. Inkonsistenzen aufweist
     * @see TrafficLightControllerParameters
     */
    public final void changeTLC(final TrafficLightControllerParameters parameters) throws Layer1Exception {
        final float currentTime = OTCManager.getInstance().getTime();

        if (currentTime < this.tlcChangeDelay) {
            throw new Layer1Exception("Node " + this.junction.getId()
                    + ": TLC cannot be changed yet, tlcChangeDelay = " + this.tlcChangeDelay + ", currentTime = "
                    + currentTime);
        }

        if (this.junction.getId() != parameters.getIds()[0]) {
            throw new Layer1Exception("This is " + this.junction.getId() + ", parameter set is for junction" + parameters.getIds()[0]);
        }

        final AbstractTLC activeTLC = this.junction.getActiveTLC();

        // Check: Ist es wirklich ein neuer TLC?
        // Test nur bei Einzelknoten, nicht im PSS.
        if (!this.partOfPSS && activeTLC.getParameters().hashCode() == parameters.hashCode()) {
            return;
        }

        int newCycleTime;
        switch (parameters.getType()) {
            case TLCTypes.FIXEDTIME:
                newCycleTime = switchFixedTimeTLC(parameters, activeTLC);
                break;
            case TLCTypes.FIXEDTIMERECALL:
                newCycleTime = switchRecallTLC(parameters, activeTLC);
                break;
            case TLCTypes.NEMA:
                newCycleTime = switchNemaTLC(parameters, activeTLC);
                break;
            default:
                throw new TLCException("Parameter type unknown.");
        }

        this.tlcChangeDelay = currentTime + newCycleTime * this.minCyclesChangeDelay;
        this.timeNextEvaluation = this.tlcChangeDelay;
        this.timeLastTLCChange = currentTime;
        this.evaluationInterval = this.numCyclesEvaluationInterval * newCycleTime;

        // Neuer TLC: Die Kapazit�t wird neu berechnet
        setCapacityOfNode();

        communicateNewCycleTimeToForecastComponents(this.minCyclesChangeDelay * newCycleTime);
    }

    /**
     * Generates from the set of parameters a fitting TrafficLightController
     * and switches it with the current TLC of this junction.
     * <p>
     * This method has been modified to allow a TLC change every cycle turn
     * instead of the standard delaytime of two cycle turns.
     *
     * @param parameters parameters, which describe the TLC.
     * @see TrafficLightControllerParameters
     */
    public final void changeTLC_PT(final TrafficLightControllerParameters parameters) {
        int temp = this.minCyclesChangeDelay;
        this.minCyclesChangeDelay = 1;

        changeTLC(parameters);

        this.minCyclesChangeDelay = temp;
    }

    /**
     * Choose TLC selector based on config file settings.
     */
    private void chooseTLCSelector(final Attribute attribute) {
        switch (DefaultParams.TLC_SELECTOR) {
            case 3:
                this.tlcSelector = new XCST(this, attribute);
                break;
            case 1:
                this.tlcSelector = new XCSIC(this, attribute);
                break;
            case 2:
                this.tlcSelector = new XCSCIC(this, attribute);
                break;
            default:
                this.tlcSelector = new XCST(this, attribute);
                break;
        }
    }

    /**
     * Send the new cycle time of the new TLC of this node to the forecast
     * storages of all turnings and outgoing sections of this junction.
     *
     * @param newCycleTime cycle time of new TLC
     */
    private void communicateNewCycleTimeToForecastComponents(final int newCycleTime) {
        final List<Turning> turnings = this.junction.getTurnings(TrafficType.INDIVIDUAL_TRAFFIC);
        turnings.stream().filter(turning -> turning.getFlowForecaster() != null).forEach(turning ->
                turning.getFlowForecaster().setStorageSize(Math.round(newCycleTime / OTCManager.getSimulationStepSize())));

        final List<Section> sections = this.junction.getOutSections();
        sections.stream().filter(section -> section.getFlowForecaster() != null).forEach(section -> {
            section.getFlowForecaster().setStorageSize(Math.round(newCycleTime / OTCManager.getSimulationStepSize()));
        });
    }

    private void estimatePhaseStart(final int phaseID, float time) {
        // über die Phasen iterieren und die Zeiten addieren, bis Beginn der
        // gesuchten Phase erreicht.
        boolean runLoop = true;
        int counter = 0;
        final List<Phase> phases = junction.getPhases();

        while (runLoop) {
            final Phase phase = phases.get(counter);
            if (phase.getId() == phaseID) {
                runLoop = false;
            } else {
                // �berpr�fen auf NaN, ansonsten aufaddieren
                if (!Float.isNaN(phase.getDefaultDuration())) {
                    time += phase.getDefaultDuration();
                }
            }
            counter++;
        }
    }

    final void finalizeAIDComponent() {
        this.aidModule.finalizeInitialisation();
    }

    private void generateTLC() {
        switch (controller) {
            case TLCTypes.FIXEDTIME:
                this.junction.generateDefaultFTC();
                break;
            case TLCTypes.FIXEDTIMERECALL:
                this.junction.generateDefaultFTRC();
                break;
            case TLCTypes.NEMA:
                this.junction.generateDefaultNEMA();
                break;
        }
    }

    /**
     * Gibt die jeweils aktuelle Kapazit�t des Knotens zur�ck.
     *
     * @return Kapazit�t des Knotens in Veh/h
     */
    public final float getCapacity() {
        if (this.capacity <= 0) {
            setCapacityOfNode();
        }
        return this.capacity;
    }

    /**
     * Calculates the estimated start time of a given phase.
     *
     * @param phaseID the ID of the Phase
     * @return a float value representing the start time of the phase in
     * relation to the cycle time (cycle-second x)
     */
    protected final float getEstimatedPhaseStart(final int phaseID) {
        final AbstractTLC tlc = junction.getActiveTLC();
        float time = 0;

        if (tlc.getParameters().getType() == TLCTypes.FIXEDTIME) {
            estimatePhaseStart(phaseID, time);
        } else if (tlc.getParameters().getType() == TLCTypes.FIXEDTIMERECALL) {
            estimatePhaseStart(phaseID, time);
            // TODO Berücksichtigung des Recalls fehlt (Sinn?)
        } else if (tlc.getParameters().getType() == TLCTypes.NEMA) {
            estimatePhaseStart(phaseID, time);
            // TODO Berechnung für NEMA ändern
        } else {
            time = Float.NaN;
            System.out.println("Received TLC with unknown type");
        }
        return time;
    }

    public final float getEvaluation(final DataSource source, final Attribute attribute,
                                     final TrafficType trafficType, final boolean setTimer) {
        return PerformanceAnalyser.getEvaluationForNode(this, source, attribute, trafficType, setTimer);
    }

    public final float getEvaluation(final DataSource source, final Attribute attribute,
                                     final TrafficType trafficType, final float interval, final boolean setTimer) {
        return PerformanceAnalyser.getEvaluationForNode(this, source, attribute, trafficType, interval, setTimer);
    }

    /**
     * Bewertet den Knoten und wählt einen neuen TLC.
     *
     * @return der gew�hlte TLC-Parametersatz
     */
    public TrafficLightControllerParameters distributeRewardAndSelectAction() throws Layer1Exception, OTCManagerException {
        final float time = OTCManager.getInstance().getTime();

        // Prüfe Mindestzeit zwischen zwei Bewertungen
        boolean timeToChange = this.tlcChangeDelay <= time && this.timeNextEvaluation <= time;
        if (this.lcsActive && !OTCManager.isLayer2Attached() && timeToChange) {
            // Die Evaluation wird standardmäßig für den Individualverkehr gemacht (der, sofern nicht separat
            // erfasst, auch den ÖPNV enthält)
            float evaluation = getEvaluation(DataSource.STATISTICS, Attribute.LOS, TrafficType.INDIVIDUAL_TRAFFIC, true);
            float[] situation = getSituationWithFlowForecast(DataSource.STATISTICS);

            updateQueueValues();

            // Wichtig: Erst distributeReward(), dann selectAction().
            this.tlcSelector.distributeReward(evaluation);

            try {
                return this.tlcSelector.selectAction(situation, 0);
            } catch (ClassifierException e) {
                OTCManager.getInstance().newWarning("Controller selection failed: " + e.getMessage());
            }
        }

        throw new Layer1Exception(time + ": no TLC change.");
    }

    private void updateQueueValues() throws Layer1Exception, OTCManagerException {
        float[] queuemax = getSituation(DataSource.STATISTICS, StatisticsCapabilities.QUEUEMAX, evaluationInterval);
        float[] queueAvg = getSituation(DataSource.STATISTICS, StatisticsCapabilities.QUEUELENGTH, evaluationInterval);

        List<Turning> turnings = this.junction.getTurnings(TrafficType.INDIVIDUAL_TRAFFIC);
        int pos = 0;
        for (Turning turning : turnings) {
            turning.addQueueData(StatisticsCapabilities.QUEUEMAX, queuemax[pos]);
            turning.addQueueData(StatisticsCapabilities.QUEUELENGTH, queueAvg[pos]);
            pos++;
        }
    }

    /**
     * Aktiviert - sofern notwendig - die Regelerzeugung an einem Knoten. Die
     * erzeugte Regel wird lediglich in die Regelmenge aufgenommen, aber noch
     * nicht(!) unmittelbar aktiviert.
     *
     * @param time die aktuelle Zeit
     */
    public void activateRuleGeneration(final float time) {
        // Pr�fe, ob Knoten Teil einer gr�nen Welle
        if (this.partOfPSS) {
            // Bestimme aktuelle Situation und aktiviere ggf. Ebene
            // 2, ohne die erzeugte Steuerung umzusetzen
            // TODO Alle 5 Minuten?!
            if (this.lcsActive && !OTCManager.isLayer2Attached() && time % 300 == 0) {
                final float[] situation = getSituationWithFlowForecast(DataSource.STATISTICS);
                this.tlcSelector.generateMappingForSituation(situation);
            }
        }
    }

    /**
     * Gibt den Zeitraum zurück, über den der Mittelwert für die Bewertung
     * gebildet wird.
     *
     * @return Intervall für Mittelwertberechnung.
     */
    public final float getEvaluationInterval() {
        return evaluationInterval;
    }

    /**
     * Gibt den Verkehrsfluss f�r die �bergebene Signalgruppe zur�ck.
     * <p>
     * Wichtig: Zuvor {@code updateTurnToFlowMap()} ausf�hren!
     *
     * @param signalGroup eine Signalgruppe
     * @return der Verkehrsfluss f�r die Signalgruppe
     * @see #updateTurnToFlowMap()
     */
    public final float getFlowForSignalGroup(final SignalGroup signalGroup) {
        final List<Turning> turnings = signalGroup.getTurnings();

        float flow = 0;
        for (Turning turning : turnings) {
            flow += getFlowForTurning(turning);
        }

        return flow;
    }

    /**
     * Gibt f�r das �bergebene Turning den aktuelle Fluss zur�ck.
     * <p>
     * Wichtig: Zuvor {@code updateTurnToFlowMap()} ausf�hren!
     *
     * @param t ein Turning
     * @return der aktuelle Fluss f�r das Turning
     * @see #updateTurnToFlowMap()
     */
    public final float getFlowForTurning(final Turning t) {
        final String key = t.getInSection().getId() + ";" + t.getOutSection().getId();
        return this.turnToFlow.get(key);
    }

    /**
     * Gibt die Id der Junction zur�ck, der dieser Node zugeordnet ist.
     *
     * @return Id
     */
    public final int getId() {
        return this.junction.getId();
    }

    public final AimsunJunction getJunction() {
        return this.junction;
    }

    public final DetectorObserver getL1DetectorObserver() {
        return this.layer1Observer.getDetectorObserver();
    }

    public final StatisticsObserver getL1StatObserver() {
        return this.layer1Observer.getStatisticsObserver(TrafficType.INDIVIDUAL_TRAFFIC);
    }

    public final Layer1Observer getLayer1Observer() {
        return this.layer1Observer;
    }

    public final int getMinCyclesBetweenEvals() {
        return this.minCyclesBetweenEvaluations;
    }

    public final AIDModule getAIDComponent() {
        return this.aidModule;
    }

    public final String getName() {
        return this.junction.getName();
    }

    /**
     * Returns a list of phase ids extracted from a parameter set of a TLC.
     *
     * @param parameters    of a TLC
     * @param numUsedPhases
     * @return list of phase ids
     */
    private List<Integer> getPhaseIDs(final TrafficLightControllerParameters parameters, final int numUsedPhases) {
        final List<Integer> phaseIds = new ArrayList<>();
        final int[] ids = parameters.getIds();
        for (int i = 1; i <= numUsedPhases; i++) {
            phaseIds.add(ids[i]);
        }
        return phaseIds;
    }

    private Recall[] getRecalls(final TrafficLightControllerParameters parameters, final int numUsedPhases) {
        final Recall[] recalls = new Recall[numUsedPhases];
        final int[] ids = parameters.getIds();
        for (int i = 0; i < numUsedPhases; i++) {
            recalls[i] = Recall.getRecallForId(ids[numUsedPhases + 1 + i]);
        }
        return recalls;
    }

    public final RoutingComponent getRoutingComponent() {
        return this.routingComponent;
    }

    /**
     * Gibt ein Feld zur�ck, dass f�r jedes Turning des Knotens zun�chst die ID
     * der Eingangssection und anschlie�end die ID der Ausgangssection enth�lt.
     * Die Reihenfolge der Turnings entspricht dabei der Reihenfolge der
     * Attributwerte, wie sie von {@code getSituation()} geliefert werden.
     *
     * @return Array, das die Section-IDs enth�lt
     */
    public final int[] getSectionIDsForTurnings() {
        return this.layer1Observer.getStatisticsObserver(TrafficType.INDIVIDUAL_TRAFFIC).getSituationSectionIDs();
    }

    /**
     * Returns a situation for the  attribute flow and the evaluation interval.
     *
     * @param source of data
     * @return situation of flow
     */
    public final float[] getSituation(final DataSource source) {
        return getSituation(source, StatisticsCapabilities.FLOW, this.evaluationInterval);
    }

    /**
     * Returns a situation for a defined attribute and interval.
     *
     * @param source    of data
     * @param attribute for situation
     * @param interval  for situation
     * @return situation
     */
    public final float[] getSituation(final DataSource source, final int attribute, final float interval) {
        return situationAnalyser.getSituation(source, attribute, interval);
    }

    /**
     * Returns a situation forecast for the standard attribute flow and the evaluation
     * interval.
     *
     * @param source of data
     * @return situation of flow
     * @throws Layer1Exception
     * @throws OTCManagerException
     */
    protected final float[] getSituationWithFlowForecast(final DataSource source) throws Layer1Exception,
            OTCManagerException {
        if (DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE) {
            return situationAnalyser.getSituationWithFlowForecast(source, StatisticsCapabilities.FLOW,
                    this.evaluationInterval);
        } else {
            return getSituation(source);
        }
    }

    public final float getTimeLastTLCChange() {
        return this.timeLastTLCChange;
    }

    public final void setTimeNextEvaluation(final float timeNextEvaluation) {
        this.timeNextEvaluation = timeNextEvaluation;
    }

    /**
     * Liefert den Zeitpunkt, zu dem der derzeit aktive TLC fr�hestens
     * ausgetauscht werden kann.
     *
     * @return Zeitpunkt
     */
    public final float getTLCChangeDelay() {
        return this.tlcChangeDelay;
    }

    public void setTLCChangeDelay(float tlcChangeDelay) {
        this.tlcChangeDelay = tlcChangeDelay;
    }

    public final AbstractTLCSelector getTLCSelector() {
        return this.tlcSelector;
    }

    final void initialiseAIDComponent() {
        this.aidModule.initialiseComponent();
    }

    /**
     * Used to set the initial values for the parameters using the configuration
     * of the PropertyManager.
     */
    private void initialiseDefaultParams() {
        this.controller = DefaultParams.L0_CONTROLLER;
        this.defaultCapacity = DefaultParams.L0_DEFAULT_CAPACITY;
        this.minCyclesChangeDelay = DefaultParams.L0_MIN_CYCLES_DELAY;
        this.numCyclesEvaluationInterval = DefaultParams.L0_NUM_CYCLES_EVALUATION_INTERVAL;
        this.minCyclesBetweenEvaluations = DefaultParams.L0_MIN_CYCLES_BETWEEN_EVALUATIONS;

        if (DefaultParams.AID_ACTIVE) {
            this.aidModule = new AIDModule(junction);
        }

        try {
            this.routingComponent = ProtocolType.initRoutingComponent(this, DefaultParams.ROUTING_PROTOCOL, DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void initNewTLC(final AbstractTLC newTlc, final AbstractTLC activeTLC) {
        final int idActivePhase = activeTLC.getCurrentPhaseID();
        final float timeLastChange = activeTLC.getTimeOfLastChange();

        try {
            newTlc.init(idActivePhase, timeLastChange);
        } catch (TLCException e) {
            // aktuelle Phase nicht im neuen TLC enthalten
            final int similarPhase = newTlc.findSimilarPhase(this.junction.getPhaseById(idActivePhase));
            newTlc.init(similarPhase, timeLastChange);
        }

        this.junction.setTrafficLightController(newTlc);
    }

    public final boolean isPartOfPSS() {
        return this.partOfPSS;
    }

    protected final void setPartOfPSS(final boolean partOfPSS) {
        this.partOfPSS = partOfPSS;
    }

    public final void receiveLinkFromNeighbor(final Link link) {
        this.aidModule.transferLink(link);
    }

    public final void sendDetectorPairToNeighborNode(final AbstractDetectorGroup pair) {
        this.aidModule.addReceivedDetectorPair(pair);
    }

    /**
     * Set new delay for TLC change.
     */
    protected final void setTlcChangeDelay(final float tlcChangeDelay) {
        this.tlcChangeDelay = tlcChangeDelay;
    }

    /**
     * Activate a new fixed-time TLC.
     *
     * @return new cycletime
     */
    private int switchFixedTimeTLC(final TrafficLightControllerParameters parameters, final AbstractTLC activeTLC) {
        final int numPhases = junction.getNumPhases();

        if (parameters.getIds().length > numPhases + 1 || parameters.getGreenTimes().length > numPhases) {
            throw new TLCException(OTCManager.getInstance().getTime() + ": Number of phases for node " + getId()
                    + " is too big." + System.getProperty("line.separator") + parameters);
        }

        final int numUsedPhases = parameters.getIds().length - 1;
        if (parameters.getGreenTimes().length != numUsedPhases) {
            throw new TLCException(
                    "Anzahl Phasen im Parametersatz passt nicht zur Anzahl der �bergebenen Phasendauern!");
        }

        final List<Integer> phaseIds = getPhaseIDs(parameters, numUsedPhases);
        final AbstractTLC newTlc = new FixedTimeController(parameters.getGreenTimes(), junction, phaseIds);
        initNewTLC(newTlc, activeTLC);

        OTCManager.getInstance().newInfo("NEW TLC: NODE_ID " + getId() + ", TIME " + OTCManager.getInstance().getTime() + ": " + newTlc);

        return newTlc.getCycleTime();
    }

    /**
     * Activate a new NEMA TLC.
     *
     * @return new cycletime
     */
    private int switchNemaTLC(final TrafficLightControllerParameters parameters, final AbstractTLC activeTLC) {
        final int numPhases = junction.getNumPhases();
        if (parameters.getIds().length > numPhases * 2 + 1 || parameters.getGreenTimes().length > numPhases) {
            throw new TLCException("Anzahl Phasen im Parametersatz zu groß!");
        }

        final int numUsedPhases = (parameters.getIds().length - 1) / 2;
        if (parameters.getGreenTimes().length != numUsedPhases * 6) {
            throw new TLCException("Anzahl Phasen im Parametersatz passt nicht zur Anzahl übergebenen Werte!");
        }

        final float[] maxGreens = new float[numPhases];
        final float[] maxInits = new float[numPhases];
        final float[] maxGaps = new float[numPhases];
        final float[] minGreens = new float[numPhases];
        final float[] extSteps = new float[numPhases];
        final float[] reductionDelays = new float[numPhases];
        for (int i = 0; i < numUsedPhases; i++) {
            maxGreens[i] = parameters.getGreenTimes()[i];
            maxInits[i] = parameters.getGreenTimes()[numPhases + i];
            maxGaps[i] = parameters.getGreenTimes()[2 * numPhases + i];
            minGreens[i] = parameters.getGreenTimes()[3 * numPhases + i];
            extSteps[i] = parameters.getGreenTimes()[4 * numPhases + i];
            reductionDelays[i] = parameters.getGreenTimes()[5 * numPhases + i];
        }

        final List<Integer> phaseIds = getPhaseIDs(parameters, numUsedPhases);
        final Recall[] recalls = getRecalls(parameters, numUsedPhases);
        final AbstractTLC newTlc = new NEMAController(maxGreens, junction, phaseIds, recalls, maxInits, maxGaps,
                minGreens, extSteps, reductionDelays);
        initNewTLC(newTlc, activeTLC);

        return newTlc.getCycleTime();
    }

    /**
     * Activate a new recall TLC.
     *
     * @return new cycletime
     */
    private int switchRecallTLC(final TrafficLightControllerParameters parameters, final AbstractTLC activeTLC) {
        final int numPhases = junction.getNumPhases();

        if (parameters.getIds().length > numPhases * 2 + 1 || parameters.getGreenTimes().length > numPhases) {
            throw new TLCException(OTCManager.getInstance().getTime()
                    + ": Anzahl der Phasen im Parametersatz f�r Knoten " + getId() + " ist zu gro�."
                    + System.getProperty("line.separator") + parameters);
        }

        final int numUsedPhases = (parameters.getIds().length - 1) / 2;
        if (parameters.getGreenTimes().length != numUsedPhases) {
            throw new TLCException(
                    "Anzahl Phasen im Parametersatz passt nicht zur Anzahl der übergebenen Phasendauern!");
        }

        final List<Integer> phaseIds = getPhaseIDs(parameters, numUsedPhases);
        final Recall[] recalls = getRecalls(parameters, numUsedPhases);
        final AbstractTLC newTlc = new FixedTimeRecallController(parameters.getGreenTimes(), junction, phaseIds, recalls);
        initNewTLC(newTlc, activeTLC);

        return newTlc.getCycleTime();
    }

    @Override
    public final String toString() {
        return getId() + getName();
    }

    /**
     * St��t die Erzeugung einer neuen Regel durch Layer 2 an.
     *
     * @param time            Zeitpunkt, f�r den die Regel erzeugt werden soll; soll
     *                        zuk�nftig entfallen, wenn Layer 2 die Verkehrssituation allein
     *                        anhand der Werte aus {@code _situation} erzeugen kann.
     * @param situation       Array mit Parametern, die eine Verkehrssituation vollst�ndig
     *                        beschreiben.
     * @param attr            zu verwendendes Optimierungskriterium
     * @param cycleConstraint Nebenbedingung an die Umlaufzeit ({@code 0}, falls
     *                        beliebig)
     */
    private void triggerL2(final float time, final float[] situation, final Attribute attr, final int cycleConstraint) {
        final OTCManager otcm = OTCManager.getInstance();

        try {
            if (otcm.isLayer2Present()) {
                final int replication = otcm.getReplicationID();
                final File angFile = new File("angNodeFiles/" + otcm.getNetwork().getName() + "_" + junction.getId()
                        + ".ang");

                otcm.addTask(new OptimisationTask(angFile, junction.getId(), time, situation,
                        getSectionIDsForTurnings(), replication, null, attr, cycleConstraint));
            } else {
                otcm.newWarning("Layer 2 has not been activated since it is not present.");
            }
        } catch (AccessException e1) {
            otcm.newWarning("AccessException occurred while trying to reach Layer 2. Junction: " + junction.getId()
                    + " Time: " + time);
            System.err.println("AccessException occurred while trying to reach Layer 2." + e1.getMessage());
        } catch (RemoteException e1) {
            otcm.newWarning("RemoteException occurred while trying to reach Layer 2. Junction: " + junction.getId()
                    + " Time: " + time);
            System.err.println("RemoteException occurred while trying to reach Layer 2." + e1.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * St��t die Erzeugung einer neuen Regel durch Layer 2 an.
     *
     * @param situation       die Situation f�r die optimiert werden soll
     * @param cycleConstraint eine Nebenbedingung an die Umlaufzeit ({@code 0}, falls
     *                        beliebig)
     */
    public final void triggerL2(final float[] situation, final int cycleConstraint) {
        triggerL2(OTCManager.getInstance().getTime(), situation, tlcSelector.getAttribute(), cycleConstraint);
    }

    /**
     * Erzeugt eine Map, die jedem Turning den aktuellen Fluss zuordnet. Ein
     * Turning wird dabei durch einen String der Form
     * "from_section_id;to_section_id" repr�sentiert.
     */
    public final void updateTurnToFlowMap() {
        final float[] situation = getSituation(DataSource.STATISTICS);
        this.turnToFlow.clear();

        final int[] sectionIDs = getSectionIDsForTurnings();
        for (int i = 0; i < situation.length; i++) {
            final String key = sectionIDs[2 * i] + ";" + sectionIDs[2 * i + 1];
            this.turnToFlow.put(key, situation[i]);
        }
    }


    /**
     * Method to change TLC from Disturbance
     * called by DisturbanceManager every time a new disturbance is built.
     */
    public void adjustGreenTimesForIncomingIntersection(Disturbance disturbance) {
        tlcChanger.adjustGreenTimesForIncomingIntersection(disturbance, false);
    }

    public void adjustGreenTimesForOutgoingIntersection(Disturbance disturbance, Section section) {
        tlcChanger.adjustGreenTimesForOutgoingIntersection(disturbance, section, false);
    }

    /**
     * Method to reset TLC
     * called by DisturbanceManager every time a disturbance is stopped
     *
     * @param endingDisturbance the disturbance that has been stopped
     */
    public void resetTLC(Disturbance endingDisturbance) {
        tlcChanger.resetTLC(endingDisturbance);
    }
}