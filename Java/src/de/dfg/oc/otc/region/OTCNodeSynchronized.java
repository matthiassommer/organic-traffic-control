package de.dfg.oc.otc.region;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.AbstractTLC;
import de.dfg.oc.otc.layer0.tlc.TLCException;
import de.dfg.oc.otc.layer0.tlc.TLCTypes;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.Layer1Exception;
import de.dfg.oc.otc.layer1.controller.ClassifierException;
import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.layer1.observer.monitoring.StatisticsCapabilities;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.*;
import org.apache.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code OTCNode}-Erweiterung - erlaubt die Synchronisierung von Knoten
 * ("gr�ne Welle").
 *
 * @author hpr
 */
public class OTCNodeSynchronized extends OTCNode {
    private static final Logger log = Logger.getLogger(OTCNodeSynchronized.class);
    /**
     * Wert gibt an, inwieweit sich die "agreed cycle time" einer gr�nen Welle
     * ver�ndern darf, bevor die �nderung im PSS umgesetzt wird.
     */
    private final float agreedCycleTimeDifference;
    /**
     * L�nge des Intervalls, �ber das die Verkehrsstr�me berechnet werden.
     */
    private final float intervalLengthForStream;
    /**
     * Groe�te gewuenschte Umlaufzeit der an der gruenen Welle beteiligten
     * Knoten.
     */
    private int agreedCycleTime;
    /**
     * Ist dies der erste Knoten einer gr�nen Welle?
     */
    private boolean beginOfPSS;
    /**
     * Flag, ob pred bereits best�tigt ist.
     */
    private boolean confirmedPredecessor;
    /**
     * Gew�nschte Umlaufzeit dieses Knotens.
     */
    private int desiredCycleTime;
    /**
     * Ist dies der letzte Knoten einer gr�nen Welle?
     */
    private boolean endOfPSS;
    /**
     * Wert gibt an, um wieviel die prediction eines neuen TLC besser sein muss,
     * um w�hrend eines aktiven PSS geschaltet zu werden.
     */
    private final float minimumPredictionDifference;
    /**
     * Enth�lt die Nachbarknoten f�r diesen {@code OTCNode}. Die ID der
     * Nachbarknoten wird durch die {@code Map} auf das zugeh�rige
     * {@code OTCNode}-Objekt abgebildet.
     */
    private Map<Integer, OTCNode> neighbours;
    private int nextSynchPhase = -1;
    /**
     * Versatzzeit des Knotens.
     */
    private int offset;
    /**
     * Der Vorg�nger in der gr�nen Welle.
     */
    private OTCNodeSynchronized predecessor;
    /**
     * Flag, ob predecessor zweite Wahl ist.
     */
    private boolean predecessorIsSecondChoice;
    /**
     * Der (prim�re) Nachfolger in der gr�nen Welle.
     */
    private OTCNodeSynchronized primarySuccessor;
    private boolean primStreamPreliminaryNo;
    private boolean queriedSecondPred;
    /**
     * Offset wird auf Basis der letzten durchschnittlichen Warteschlange vor
     * dem synchronisierten Turning angepasst.
     */
    private int queueBasedAdjustment;
    /**
     * Bei der Synchronisierung von Knoten wird dieser TLC geschaltet, nachdem
     * die gew�nschte Versatzzeit erreicht worden ist.
     */
    private TrafficLightControllerParameters replaceTLC;
    /**
     * Parameter, die f�r die Steuerung des Synchronisationsmechanismus genutzt
     * werden, wenn der Aufruf au�erhalb des normalen Zyklus erfolgt.
     */
    private boolean runSynchPhase;
    /**
     * Zweitstaerkster Strom von dem dieser Knoten bedient wird.
     */
    private TrafficStream secondaryStream;
    /**
     * Liste aller angemeldeten potentiellen Nachfolger.
     */
    private List<OTCNodeSynchronized> successorList;
    /**
     * Staerkster Strom von dem dieser Knoten bedient wird.
     */
    private TrafficStream synchronisedStream;
    /**
     * Startzeitpunkt der synchronisierten Phase (Umlaufsekunde).
     */
    private int syncStart;
    /**
     * Bei der Synchronisierung von Knoten wird dieser tempor�re TLC geschaltet,
     * um die gew�nschte Versatzzeit zu erreichen.
     */
    private TrafficLightControllerParameters tempTLC;
    /**
     * Bei der Synchronisierung von Knoten wird ein tempor�rer TLC geschaltet,
     * um die gew�nschte Versatzzeit zu erreichen. Die Variable bestimmt den
     * Zeitpunkt, zu dem der tempor�re TLC aktiviert wird.
     */
    private float timeToActivateTempTLC = -1;
    private float timeToNextSynchPhase = -1;
    /**
     * Bei der Synchronisierung von Knoten wird ein tempor�rer TLC geschaltet,
     * um die gew�nschte Versatzzeit zu erreichen. Die Variable bestimmt den
     * Zeitpunkt, zu dem der tempor�re TLC deaktiviert wird, weil die gew�nschte
     * Versatzzeit erreicht ist.
     */
    private float timeToDeactivateTempTLC = -1;
    /**
     * Startzeitpunkt der gr�nen Welle am ersten Knoten.
     */
    private float timeZeroForPSS;
    /**
     * Enth�lt f�r jeden Nachbarknoten die lokal am st�rksten belastete
     * Abbiegebeziehung in dessen Richtung.
     */
    private Map<Integer, TrafficStream> trafficStreams;
    /**
     * Flag gibt an, ob zur Berechnung des Vorg�ngers die von den Nachbarn
     * bereitgestellten Streaminformationen genutzt werden sollen (
     * {@code true}) oder die lokalen turning-infos ({@code false}).
     */
    private final boolean useNeighbourStreams;

    public OTCNodeSynchronized(final AimsunJunction junction, final Attribute attribute) {
        super(junction, attribute);
        this.desiredCycleTime = junction.getActiveTLC().getCycleTime();

        this.useNeighbourStreams = DefaultParams.PSS_USE_NEIGHBOR_STREAMS;
        this.minimumPredictionDifference = DefaultParams.PSS_MIN_PREDICTION_DIFFERENCE;
        this.intervalLengthForStream = DefaultParams.PSS_INTERVAL_LENGTH_FOR_STREAM;
        this.agreedCycleTimeDifference = DefaultParams.PSS_ACTDIFF;
    }

    /**
     * If the current situation requires a new TLC while the PSS is active and
     * no changes for the whole PSS are necessary, this method selects the new
     * TLC, adapts the offsets and activates the TLC using the same temp-TLC
     * mechanism as for normal changes.
     */
    private void adaptTLCForActivePSS() {
        TrafficLightControllerParameters newTLCP;

        try {
            // Bestimme und verteile Bewertung
            final float evaluation = getEvaluation(DataSource.STATISTICS, Attribute.LOS,
                    TrafficType.INDIVIDUAL_TRAFFIC, true);
            getTLCSelector().distributeReward(evaluation);

            // Bestimme Situation und wähle neuen TLC
            final float[] situation = getSituationWithFlowForecast(DataSource.STATISTICS);
            newTLCP = getTLCSelector().selectAction(situation, agreedCycleTime);
        } catch (Layer1Exception | ClassifierException | OTCManagerException e) {
            // Fehler aufgetreten: Versuche, bisherigen TLC anzupassen
            getJunction().getActiveTLC().getParameters();
            log.error("Error while determining new TLC: ", e);
            return;
        }

        adjustOffsetForInternalTLCChange(newTLCP);

        // Schalten des neuen TLC
        changeTLCWithOffset(newTLCP);
    }

    /**
     * Methode berechnet die neue Versatzzeit bei einem TLC-Wechsel, der nur
     * diesen Knoten betrifft und nicht die gesamte g�ne Welle.
     *
     * @param newTLCP
     */
    private void adjustOffsetForInternalTLCChange(final TrafficLightControllerParameters newTLCP) {
        final TrafficLightControllerParameters currentTLCP = getJunction().getActiveTLC().getParameters();
        if (newTLCP.getType() != currentTLCP.getType()) {
            // Wechsel des Controller-Typs - keine Anpassung m�glich
            throw new TLCException("Change of TLC types detected - no comparison possible!");
        }

        // Hole synchronisierte Phasen ID
        final int synchPhaseID = determinePhaseForPSS();

        // Bestimme Dauer bis Beginn dieser Phase
        final float durationCurrent = currentTLCP.getStartOfPhase(synchPhaseID);

        // Bestimme Dauer bis Beginn entsprechender Phase bei neuem TLC
        final float durationNext = newTLCP.getStartOfPhase(synchPhaseID);

        // Bestimme Differenz
        final float adjustment = durationCurrent - durationNext;

        // Berechne Anpassung des Offsets, bzw. neuen Offset
        this.offset += Math.round(adjustment);
    }

    /**
     * Methode berechnet die Anpassung der Versatzzeit in Abh�ngigkeit von
     * durchschnittlich bestehenden Fahrzeugschlangen f�r das synchronisierte
     * Turning. Setzt den Wert der Variablen {@code queueBasedAdjustment}.
     */
    private void adjustOffsetUsingQueues() {
        // Bestimme das passende Turning (Verbindung zwischen pred und suc)
        // Wenn node = beginOfPSS, keine Anpassung n�tig
        if (!beginOfPSS) {
            // Knoten hat Vorg�nger -> predecessor

            // Eingehende Section bestimmen
            final List<Section> path = predecessor.getOffsetForNode(this.getId()).getPath();
            int sectionID = -1;

            if (!path.isEmpty()) {
                final Section section = path.get(path.size() - 1);
                sectionID = section.getId();
            }

            // Wenn es eingehende Section gibt, Turning bestimmen
            if (sectionID >= 0) {
                // Hole alle Turnings
                final List<Turning> turns = getJunction().getTurnings(TrafficType.INDIVIDUAL_TRAFFIC);
                if (!turns.isEmpty()) {
                    int turningID = findBestTurning(sectionID);
                    if (turningID < 0) {
                        return;
                    }

                    // Bestimme die Warteschlange an dem Turning
                    float avQueue = -1;
                    try {
                        avQueue = getLayer1Observer().getStatisticsObserver(TrafficType.INDIVIDUAL_TRAFFIC)
                                .getAverageValue(turningID, StatisticsCapabilities.QUEUELENGTH, 270.0f);
                    } catch (OTCManagerException e) {
                        log.error("Error while determining current queue length at" + "node " + getId() + " : "
                                + e);
                    }

                    float clearingTime = 0;
                    // Errechne die ben�tigte Abflusszeit
                    if (avQueue > 0) {
                        clearingTime = avQueue + 2;
                    }

                    // Errechne die Anpassung - runden
                    int adjustment = (int) (clearingTime + 0.5f);

                    // Normalisiere den errechneten Wert
                    if (adjustment > 0) {
                        // Bestimme die maximale Phasendauer
                        final int maxPhaseDuration = getPhaseDurationForTurning(turningID);
                        final int tlcDuration = getJunction().getActiveTLC().getCycleTime();
                        // Warteschlangenaufbau, wenn Phase rot (tlcDuration
                        // - phaseDuration).
                        // Da Durchschnitt, tatsächliche Warteschlange höher

                        int diff = tlcDuration - maxPhaseDuration;
                        if (diff <= 0) {
                            diff = 1;
                        }

                        adjustment *= tlcDuration / diff * 2;

                        // Wenn maxPhase/2 > adjustment, Verringerung
                        if (adjustment > maxPhaseDuration / 2) {
                            adjustment = maxPhaseDuration / 2;
                        }
                    }

                    queueBasedAdjustment = adjustment;
                }
            } else {
                // Keine turnings vorhanden
                log.error("There are no turnings for this junction. ID: " + getJunction().getId());
                queueBasedAdjustment = 0;
            }
        } else {
            // Kein Turning f�r Predecessor gefunden - Fehler
            log.error("No matching turning for predecessor found when adjusting offset");
            queueBasedAdjustment = 0;
        }
    }

    private int findBestTurning(int sectionID) {
        // W�hle alle Turnings, die von predSection abgehen
        final List<Turning> turnings = getJunction().getTurningsForIncomingSectionID(sectionID);

        // W�hle das Turning zu successor, sonst gr��tes
        int turningID = -1;
        int turnWithHighestFlow = -1;

        if (!turnings.isEmpty()) {
            // Es bestehen Turnings vom pred
            if (turnings.size() == 1) {
                turningID = turnings.get(0).getId();
            } else {
                // Bestimme Section ID zu Nachfolger
                int sucSectionID = -1;
                if (primarySuccessor != null) {
                    sucSectionID = getOffsetForNode(primarySuccessor.getId()).getPath().get(0).getId();
                }

                // Suche das beste raus
                float stream = -1;

                for (Turning turning : turnings) {
                    if (!this.endOfPSS && sucSectionID > 0) {
                        // Setze aktuelles Turning
                        if (turning.getOutSection().getId() == sucSectionID) {
                            turningID = turning.getId();
                        }
                    }

                    float tempStream = getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC,
                            intervalLengthForStream, turning.getId(), StatisticsCapabilities.FLOW);

                    if (!Float.isNaN(tempStream) && tempStream > stream) {
                        stream = tempStream;
                        turnWithHighestFlow = turning.getId();
                    }
                }
            }
        }

        if (turningID < 0 && turnWithHighestFlow < 0) {
            log.error("No matching turnings for this junction (ID: " + getJunction().getId()
                    + ") found!");
            queueBasedAdjustment = 0;
            return -1;
        } else {
            if (turningID < 0 && turnWithHighestFlow > 0) {
                // Kein passendes Turning gefunden -> w�hle gr�sstes
                return turnWithHighestFlow;
            }
        }
        return turningID;
    }

    /**
     * Wird genutzt, um ein Matching zwischen Vorg�nger und m�glichen
     * Nachfolgern durchzuf�hren. Versendet REJ oder ACK an angemeldete
     * Successors. Diese Methode setzt den(/die) Nachfolgeknoten.
     */
    private void calculateGreenWave() {
        // Auswahl des besten m�glichen Successors in Abh�ngigkeit von m�gl.
        // Feedback
        int bestSuccID = -1;
        final Map<Integer, Integer> successorIDs = getOrderedSuccessorIDsForPred(-1);

        if (!successorIDs.isEmpty()) {
            bestSuccID = successorIDs.get(1);
        }

        OTCNodeSynchronized bestSuccessor = null;
        if (bestSuccID > 0 && neighbours.containsKey(bestSuccID)) {
            bestSuccessor = (OTCNodeSynchronized) neighbours.get(bestSuccID);
        }

        // Setzen des Successor Knotens
        if (bestSuccessor != null) {
            primarySuccessor = bestSuccessor;
        }

        // Setzen des Predecessor Knotens
        if (predecessorIsSecondChoice && secondaryStream != null
                && neighbours.containsKey(secondaryStream.getOriginNodeID())) {
            // Vorg�nger ist zweite Wahl
            predecessor = (OTCNodeSynchronized) neighbours.get(secondaryStream.getOriginNodeID());
        } else if (!predecessorIsSecondChoice && synchronisedStream != null
                && !(confirmedPredecessor && predecessor == null)
                && neighbours.containsKey(synchronisedStream.getOriginNodeID())) {
            // Vorg�nger ist erste Wahl und hat nicht abgelehnt
            predecessor = (OTCNodeSynchronized) neighbours.get(synchronisedStream.getOriginNodeID());
        } // else: kein Vorg�nger gefunden

        // Best�tigen / ablehnen der angemeldeten Nachfolger
        for (OTCNodeSynchronized node : successorList) {
            boolean result;

            if (primarySuccessor != null && node.getId() == primarySuccessor.getId()) {
                result = node.notifySuccessor(getId(), true, false);
                log.debug("Node " + getId() + "- Check ergibt: Node " + primarySuccessor.getId()
                        + " sagt " + result);
            } else {
                node.notifySuccessor(getId(), false, false);
                result = true;
            }

            // Fehler aufgetreten?
            if (!result) {
                // Mein Nachfolger will nun doch nicht mehr
                primarySuccessor = null;
                // TODO hier noch was machen?
            }
        }

        // aktive gruene Welle?
        if (predecessor != null || primarySuccessor != null) {
            setPartOfPSS(true);
        }

        if (isPartOfPSS() && predecessor == null) {
            beginOfPSS = true;
        }

        if (isPartOfPSS() && primarySuccessor == null) {
            endOfPSS = true;
        }
    }

    /**
     * Bestimmt die besten Vorg�ngerknoten.
     *
     * @param local Flag gibt an, ob Berechnung auf rein lokalen Informationen
     *              beruht (Turningstatistiken - {@code true}) oder auf den
     *              von Nachbarn bereitgestellten Stream-Infos ({@code false}
     *              ). Im zweiten Fall werden Wege, die das Netz verlassen nicht
     *              ber�cksichtigt!
     * @return HashMap enth�lt die m�glichen Vorg�nger. Key ist Integer und gibt
     * die Reihenfolge an - beginnend mit 1. Wenn Eintrag null ist,
     * entspricht dies einem fiktiven Knoten (Centroid, etc.).
     */
    private Map<Integer, OTCNodeSynchronized> calculatePredecessors(final boolean local) {
        if (local) {
            return findLocalPredessors();
        }
        return findPredessorsByStreams();
    }

    private Map<Integer, OTCNodeSynchronized> findLocalPredessors() {
        final Map<Integer, OTCNodeSynchronized> preds = new HashMap<>();
        float bestStream = 0;
        float secondBestStream = 0;
        int nodeID = -1;
        int secNodeID = -1;

        final List<Section> insections = getJunction().getInSections();
        for (Section section : insections) {
            // verfolge den Weg zur�ck bis zum Nachbarn und setze die ID
            // des gefundenen Nachbarn, andernfalls -1
            int neighbourID = -1;
            List<Integer> nodes = section.determineSendingNodes(new ArrayList<>(), new ArrayList<>());

            // Wähle besten Knoten aus
            for (Integer node : nodes) {
                // Suche die h�chste ID aus
                if (node > neighbourID) {
                    neighbourID = node;
                }
                // TODO Bessere Auswahl bei mehreren gefundenen IDs?
            }

            // Hole alle zugeh�rigen Turnings mit zugeh�rigen FLOWS
            List<Turning> turnsForSection = this.getJunction().getTurningsForIncomingSectionID(section.getId());
            // W�hle den h�chsten Wert eines Turnings als Stream
            // f�r diesen Nachbarn. Wenn Nachbar unbekannt
            // (=> Centroid?) bleibt Nachbar-ID -1
            for (Turning turning : turnsForSection) {
                float stream = this.getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC,
                        intervalLengthForStream, turning.getId(), StatisticsCapabilities.FLOW);
                if (stream > bestStream) {
                    // �bertragen der Info von erstem auf zweiten
                    // Stream
                    secondBestStream = bestStream;
                    secNodeID = nodeID;

                    // Setzen des neuen besten Stroms
                    bestStream = stream;
                    nodeID = neighbourID;
                } else if (stream > secondBestStream) {
                    secondBestStream = stream;
                    secNodeID = neighbourID;
                }
            }
        }

        // W�hle besten (+ zweitbesten) Nachbarn
        // Wenn Nachbar -1, kein Strom, sonst setzen
        if (nodeID >= 0 && neighbours.containsKey(nodeID)) {
            preds.put(1, (OTCNodeSynchronized) neighbours.get(nodeID));
        } else {
            preds.put(1, null);
        }

        if (secNodeID >= 0 && neighbours.containsKey(secNodeID)) {
            preds.put(2, (OTCNodeSynchronized) neighbours.get(secNodeID));
        } else {
            preds.put(2, null);
        }
        return preds;
    }

    private Map<Integer, OTCNodeSynchronized> findPredessorsByStreams() {
        final Map<Integer, OTCNodeSynchronized> preds = new HashMap<>();
        float bestFlow = 0;
        float secondBestFlow = 0;
        TrafficStream streamOne = null;
        TrafficStream streamTwo = null;

        // Iteriere �ber Nachbarknoten und bestimme den gr��ten ankommenden
        // Strom.
        for (OTCNode neighbour : neighbours.values()) {
            float stream = ((OTCNodeSynchronized) neighbour).getTrafficStreamForNode(this.getId())
                    .getStreamStrength();
            TrafficStream trafficStream = ((OTCNodeSynchronized) neighbour).getTrafficStreamForNode(this.getId());

            if (!Float.isNaN(stream) && stream >= bestFlow) {
                // Zuerst update des zweiten Stroms
                if (streamOne != null) {
                    secondBestFlow = bestFlow;
                    streamTwo = streamOne;
                }
                // Updaten des besten Stroms
                bestFlow = stream;
                streamOne = trafficStream;
            } else {
                if (!Float.isNaN(stream) && stream > secondBestFlow && streamOne != trafficStream) {
                    streamTwo = trafficStream;
                    secondBestFlow = stream;
                }
            }
        }

        // Setzen der R�ckgabewerte
        if (streamOne != null && neighbours.containsKey(streamOne.getOriginNodeID())) {
            preds.put(1, (OTCNodeSynchronized) neighbours.get(streamOne.getOriginNodeID()));
        } else {
            preds.put(1, null);
        }

        if (streamTwo != null && neighbours.containsKey(streamTwo.getOriginNodeID())) {
            preds.put(2, (OTCNodeSynchronized) neighbours.get(streamTwo.getOriginNodeID()));
        } else {
            preds.put(2, null);
        }
        return preds;
    }

    /**
     * Bestimmt die am st�rksten belastete Abbiegebeziehung in Richtung jedes
     * Nachbarknotens. Die ermittelten Str�me werden in der
     * {@code HashMap trafficStreams} abgelegt.
     */
    private void calculateStreams() {
        // Check consistency
        if (neighbours == null) {
            neighbours = this.getJunction().getNeighbouringNodes();
        }

        // Streams neu setzen, um Parallelit�t zu gew�hrleisten
        final Map<Integer, TrafficStream> newTrafficStreams = new HashMap<>();

        for (OTCNode neighbour : neighbours.values()) {
            float strongestStream = 0;
            Turning bestStream = null;

            // Get all turnings connected with the section leading to the
            // particular neighbour
            List<Turning> neighTurning = this.getJunction().getTurningsForNeighbour(neighbour.getJunction().getId());
            TrafficStream streamForNode = null;

            // Iterate over set of turnings, choose highest FLOW-value
            if (!neighTurning.isEmpty()) {
                for (Turning turning : neighTurning) {
                    float stream = this.getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC,
                            intervalLengthForStream, turning.getId(), StatisticsCapabilities.FLOW);

                    if (!Float.isNaN(stream) && stream >= strongestStream) {
                        strongestStream = stream;
                        bestStream = turning;
                    }
                }
                streamForNode = new TrafficStream(this.getId(), neighbour.getId(), strongestStream, 0);
            }

            /**
             * An dieser Stelle steht der beste Strom fest, dazu muessen jetzt
             * noch der Offset und die zugeh�rige Phase (evtl. mit Phasenzeit)
             * geholt werden.
             */
            // Ueberpruefung, ob bester Strom gefunden
            if (bestStream != null && strongestStream > 0) {
                // Hole alle Phasen, in denen das Turning bedient wird
                List<Phase> phases = this.getJunction().getPhasesForTurnings(bestStream.getId());
                float relStartTime = 0;

                if (phases.isEmpty()) {
                    // Keiner Phase zugeordnet
                    relStartTime = Float.NaN;
                } else if (phases.size() == 1) {
                    // Beginn der Phase rausfinden
                    relStartTime = getEstimatedPhaseStart(phases.get(0).getId());
                } else {
                    // Erst die wichtigste Phase rausfinden
                    // Turning kommt in mehreren Phasen vor - w�hle die Phase
                    // mit der
                    // l�ngsten Dauer (da Verkehr ~ Dauer)
                    float duration = 0;

                    for (Phase phase : phases) {
                        float tempDuration = phase.getDefaultDuration();
                        if (!Float.isNaN(tempDuration) && duration < tempDuration) {
                            // Wenn bedingung erf�llt, setzen der Phase.
                            duration = tempDuration;
                            // Dann Beginn der Phase rausfinden
                            relStartTime = getEstimatedPhaseStart(phase.getId());
                        }
                    }
                }
                streamForNode.setSynchTime(relStartTime);
            }

            // Wenn keine Str�me berechnet werden k�nnen, setze default
            if (streamForNode == null) {
                streamForNode = new TrafficStream(this.getId(), neighbour.getId(), Float.NaN, Float.NaN);
            }

            newTrafficStreams.put(neighbour.getId(), streamForNode);
        }
        trafficStreams = newTrafficStreams;
    }

    /**
     * Wechselt zum als Parameter �bergebenen TLC unter Beachtung der
     * Versatzzeit.
     *
     * @param parameters Parameter, die den TLC beschreiben.
     */
    private void changeTLCWithOffset(final TrafficLightControllerParameters parameters) {
        switch (parameters.getType()) {
            case TLCTypes.FIXEDTIME:
                replaceTLC = parameters;
                determineTempTLCParams();
                break;
            case TLCTypes.FIXEDTIMERECALL:
                // TODO Implement
                throw new TLCException(
                        "Wechsel eines 'FixedTimeRecall'-Controllers unter Beachtung der Versatzzeit ist nicht möglich.");
            case TLCTypes.NEMA:
                replaceTLC = parameters;
                determineTempTLCParams();
                break;
            // TODO Überarbeiten
            default:
                throw new TLCException("Parametersatz hat unbekannten Typ.");
        }
    }

    /**
     * �berpr�ft die bestehende gr�ne Welle: (i) Werden neue Partnerschaften
     * gew�nscht? (ii) Wird eine neue agreedCycleTime gew�nscht? Ggf. werden
     * {@code runSynchPhase} und {@code nextSynchPhase} am
     * Anfangsknoten entsprechend gesetzt, die �nderungen selbst werden hier
     * jedoch nicht angesto�en.
     *
     * @see #checkChangeDemand(boolean, int, boolean)
     */
    final void checkChangeDemand() {
        checkChangeDemand(false, -1, true);
    }

    /**
     * Rekursive Methode / Wellenalgorithmus zur �berpr�fung der bestehenden
     * gr�ne Welle: (i) Werden neue Partnerschaften gew�nscht? (ii) Wird eine
     * neue agreedCycleTime (ACT) gew�nscht? Ggf. werden {@code runSynchPhase}
     * und {@code nextSynchPhase} am Anfangsknoten entsprechend gesetzt,
     * die �nderungen selbst werden hier jedoch nicht angesto�en.
     *
     * @param newPartnerships W�nschen die bereits �berpr�ften Knoten neue Partner?
     * @param newACT          W�nschen die bereits �berpr�ften Knoten eine neue ACT?
     * @param direction       Hinrichtung ({@code true}) oder R�ckrichtung (
     *                        {@code false}) der Welle?
     */
    private void checkChangeDemand(boolean newPartnerships, int newACT, final boolean direction) {
        // TODO Falls "this" nicht erster Knoten einer grünen Welle, gehe zum Anfang

        // Durchlaufe gr�ne Welle zum Ende
        if (direction) {
            if (checkPartnerships()) {
                newPartnerships = true;
            }

            // Check ACT
            try {
                final float[] situation = getSituationWithFlowForecast(DataSource.STATISTICS);
                final int desiredCycleTime = getTLCSelector().determineDesiredCycleTime(situation);
                newACT = Math.max(desiredCycleTime, newACT);
            } catch (Layer1Exception | ClassifierException | OTCManagerException e) {
                log.error(e);
            }

            if (!endOfPSS) {
                // Aktualisiere Nachfolgerknoten
                primarySuccessor.checkChangeDemand(newPartnerships, newACT, true);
            } else {
                // Starte Rückrichtung
                checkChangeDemand(newPartnerships, newACT, false);
            }
        } else {
            // Rückrichtung
            // Lokale Anpassung gewünscht?
            if (!newPartnerships && !(Math.abs(newACT - agreedCycleTime) > agreedCycleTimeDifference)) {
                // F�r die gesamte gr�ne welle werden keine �nderungen gew�nscht
                // -> lokale Anpassung m�glich
                log.debug("Check jetzt lokal Rückrichtung");

                if (checkTLC()) {
                    // Besserer TLC gefunden - auswählen und schalten
                    adaptTLCForActivePSS();
                }
            }

            if (!beginOfPSS) {
                // Durchleiten der Welle zum Vorgänger
                predecessor.checkChangeDemand(newPartnerships, newACT, false);
            } else {
                // Erster Knoten wurde erreicht
                boolean changeDueToNewACT = false;
                int actForLog = agreedCycleTime;

                // Änderungen innerhalb der Welle gewünscht
                if (newPartnerships) {
                    // Neue Partnerschaften gewünscht!
                    runSynchPhase = true;
                    nextSynchPhase = 0;

                    log.debug("Neue Partnerschaften notwendig");
                } else if (Math.abs(newACT - agreedCycleTime) > agreedCycleTimeDifference) {
                    changeDueToNewACT = true;

                    // Neue ACT gewünscht!
                    runSynchPhase = true;
                    nextSynchPhase = 6;

                    actForLog = newACT;
                }
                logPSSUpdate(newPartnerships, changeDueToNewACT, actForLog);
            }
        }
    }

    /**
     * Method checks, if successor eq. predecessor. In this case, the PSS is
     * inconsistent, false will be returned. True otherwise.
     *
     * @return
     */
    private boolean checkLocalPSS() {
        return !(predecessor != null && primarySuccessor != null && predecessor == primarySuccessor);
    }

    /**
     * Pr�ft, ob dieser Knoten weiterhin mit seinem aktuellen Vorg�nger und
     * Nachfolger zufrieden ist oder ob andere Partner gew�nscht werden. Ggf.
     * werden {@code runSynchPhase} und {@code nextSynchPhase}
     * entsprechend gesetzt. ("Ich bin mit der Gesamtsituation unzufrieden!")
     *
     * @return {@code true}, falls andere Partner gew�nscht werden;
     * {@code false} sonst
     */
    private boolean checkPartnerships() {
        // Aktualisieren der n�tigen Werte
        calculateStreams();

        // Check, ob Vorg�nger- und Nachfolgerknoten in der gr�nen Welle
        // weiterhin passen
        OTCNode neighbour = null;

        final Map<Integer, OTCNodeSynchronized> posPreds = calculatePredecessors(!useNeighbourStreams);
        if (!posPreds.isEmpty() && posPreds.containsKey(1)) {
            neighbour = posPreds.get(1);
        }

        // Jetzt ist der beste Nachbar bekannt, �berpr�fen, ob noch der gleiche
        final boolean noSynchronisedStream = synchronisedStream == null || synchronisedStream.getOriginNodeID() < 0;
        if (!(neighbour == null && noSynchronisedStream)
                && (neighbour == null || synchronisedStream == null || neighbour.getId() != synchronisedStream
                .getOriginNodeID())) {
            /*
             * a) Kein Vorg�nger mehr, vorher schon b) Neuer Vorg�nger gefunden,
			 * vorher keiner c) Anderer Vorg�nger als vorher
			 */
            // �nderungen gew�nscht!
            runSynchPhase = true;
            // Neue Partner gew�nscht!
            nextSynchPhase = 0;
            timeToNextSynchPhase = 0;

            return true;
        }
        return false;
    }

    /**
     * Pr�ft, ob dieser Knoten bei gleicher {@code agreedCycleTime} einen
     * anderen TLC schalten m�chte.
     *
     * @return select new TLC
     */
    private boolean checkTLC() {
        boolean changeNeeded = false;
        final float minTimeToNextPPSCalculation = 300;

        // Lokale �nderung des TLC nur m�glich, wenn Abstand zu n�chstem
        // Berechnungsinterval gro� genug
        final float time = OTCManager.getInstance().getTime();
        if (timeToNextSynchPhase - time > minTimeToNextPPSCalculation) {
            // Check, ob lokale �nderung gew�nscht
            if (verifyCurrentTLC()) {
                changeNeeded = true;

                log.debug(time + ": Node " + this.getId() + " would like to locally change TLC");
            }
        }

        return changeNeeded;
    }

    /**
     * Methode �berpr�ft, ob der Eingang des �bergebenen Turnings von einem
     * m�glichen Vorg�ngerknoten (definiert durch ID) aus erreicht werden kann.
     *
     * @param turning The Turning
     * @param predID  The ID of the predecessor node
     * @return {@code true} if reachable, {@code false} otherwise
     */
    private boolean checkTurningOrigin(final Turning turning, final int predID) {
        if (turning == null || predID < 0) {
            return false;
        }

        // Bestimme alle m�glichen Vorg�nger des Turnings
        final Section insection = turning.getInSection();
        final List<Integer> predecessors = insection.determineSendingNodes(new ArrayList<>(),
                new ArrayList<>());

        // Test, ob m�gl. Vorg�nger gefunden
        if (predecessors.isEmpty()) {
            return false;
        }

        // Bestimme, ob predecessor erreichbar
        for (int id : predecessors) {
            if (id == predID) {
                return true;
            }
        }

        return false;
    }

    /**
     * Method used to choose the best successor in the first step: If a
     * predecessor is known, the Turning with the highest FLOW from this
     * predecessor will be determined and the corresponding node ID returned.
     * Otherwise the Truning with the highest FLOW over all will be selected and
     * the corresponding Node ID determined.
     *
     * @return
     */
    private int chooseTempSuccessorID() {
        int successorID = -1;

        // Bestimme den aktuell besten Vorg�nger
        final int predecessorID = determineCurrentPredecessorID();

        if (successorList.isEmpty()) {
            // Keine angemeldeten Successors
            return successorID;
        } else {
            // Bestimme den Nachfolger, der gemaess lokalen Infos am besten
            // zu dem aktuell gewaehlten Vorgaenger passt
            final Collection<Turning> turnings = new ArrayList<>();

            // Hole alle Turnings
            final List<Turning> turns = getJunction().getTurnings(TrafficType.INDIVIDUAL_TRAFFIC);

            if (predecessorID > 0) {
                // Vorgaenger gewaehlt - sortiere alle unpassenden raus
                turnings.addAll(turns.stream().filter(turning -> checkTurningOrigin(turning, predecessorID)).map(turning -> turning).collect(Collectors.toList()));
            } else {
                turnings.addAll(turns);
            }

            // Waehle das mit dem groessten FLOW aus
            Turning turning = null;
            float bestFlow = -1;

            for (Turning turn : turnings) {
                final float flow = getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC,
                        intervalLengthForStream, turn.getId(), StatisticsCapabilities.FLOW);
                if (flow > bestFlow) {
                    bestFlow = flow;
                    turning = turn;
                }

            }

            // Waehle den entsprechenden Nachfolgeknoten aus
            if (turning == null) {
                return -2;
            }

            // Hole den Pfad zur naechsten Junction
            final Section outSection = turning.getOutSection();
            final List<Section> path = outSection.findPathsToNextJunction(new ArrayList<>());

            if (path == null || path.isEmpty()) {
                return -3;
            }

            final Section lastSection = path.get(path.size() - 1);
            if (lastSection == null || lastSection.getNextJunction() == null
                    || lastSection.getNextJunction().getNode() == null) {
                return -4;
            }

            // Bestimme die ID des naechsten Knotens
            successorID = lastSection.getNextJunction().getNode().getId();
        }

        return successorID;
    }

    /**
     * Method determines the ID of the currently chosen predecessor node
     * depending on the status of the confirmation process.
     *
     * @return ID of the {@code OTCNode} which is currently chosen as
     * predecessor
     */
    private int determineCurrentPredecessorID() {
        int id = -1;
        if (predecessor != null && confirmedPredecessor) {
            // Predecessor best�tigt
            id = predecessor.getId();
        } else if (predecessor != null) {
            // Noch keine endgueltige Bestaetigung vom Pred
            id = predecessor.getId();
        } else if (!primStreamPreliminaryNo) {
            // Erster Strom ist die aktuelle Wahl und kein Centroid
            if (synchronisedStream != null) {
                id = synchronisedStream.getOriginNodeID();
            }
        }
        return id;
    }

    /**
     * Determines the offset for this node. The results depends on the
     * predecessor's offset, the start of the predecessor's synchronized phase,
     * the distance between the predecessor and this node and the start of this
     * node's synchronized phase.
     */
    private void determineOffsetAndSelectTLC() {
        // Bestimme Phase, die Vorg�nger und Nachfolger verbindet
        final int syncPhase = determinePhaseForPSS();

        // Passenden TLC ausw�hlen, aggregierte Umlaufzeit beachten
        TrafficLightControllerParameters newTLC;
        try {
            // Analog zu OTCManager.distributeRewardAndSelectAction, aber
            // ohne(!) �berpr�fung der timeNextEvaluation und tlcChangeDelay:

            // Bestimme und verteile Bewertung
            final float evaluation = getEvaluation(DataSource.STATISTICS, Attribute.LOS,
                    TrafficType.INDIVIDUAL_TRAFFIC, true);
            getTLCSelector().distributeReward(evaluation);

            // Bestimme Situation und wähle neuen TLC
            final float[] situation = getSituationWithFlowForecast(DataSource.STATISTICS);
            newTLC = getTLCSelector().selectAction(situation, agreedCycleTime);
        } catch (Layer1Exception | ClassifierException | OTCManagerException e) {
            // Fehler aufgetreten: Versuche, bisherigen TLC anzupassen
            newTLC = getJunction().getActiveTLC().getParameters().adaptCycleTime(this.agreedCycleTime);
            log.error("There was a problem when adapting the TLC for synchronisation: ", e);
        }

        // Bestimme Start der synchronisierten Phase im neuen TLC
        syncStart = newTLC.getStartOfPhase(syncPhase);

        adjustOwnOffest();

        if (predecessor == null) {
            // Erster Knoten gibt Startzeitpunkt der grünen Welle vor
            this.timeZeroForPSS = OTCManager.getInstance().getTime() + this.agreedCycleTime;
        } else {
            // Nachfolgeknoten übernehmen Startzeitpunkt vom Vorgänger
            this.timeZeroForPSS = predecessor.timeZeroForPSS;
        }

        changeTLCWithOffset(newTLC);

        // Weiter mit dem Nachfolger
        if (!this.endOfPSS) {
            this.primarySuccessor.determineOffsetAndSelectTLC();
        }
    }

    private void adjustOwnOffest() {
        if (predecessor != null) {
            // Versatzzeit des Vorg�ngers
            int predecessorOffset = predecessor.offset;

            // Start der synchronisierten Phase des Vorg�ngers
            int predecessorSyncStart = predecessor.syncStart;

            // Zeitbedarf f�r Wegstrecke vom Vorg�nger
            int timeDistance = predecessor.getOffsetForNode(getId()).getOffset();

            // Eigene Versatzzeit
            // Abbau der Schlange
            adjustOffsetUsingQueues();
            this.offset = (predecessorOffset + predecessorSyncStart + timeDistance - syncStart - queueBasedAdjustment)
                    % this.agreedCycleTime;

            if (offset < 0) {
                this.offset += this.agreedCycleTime;
            }
        }
    }

    /**
     * Determines the phase id of this node's synchronized phase.
     *
     * @return phase id of this node's synchronized phase
     */
    private int determinePhaseForPSS() {
        Turning turningForPSS;
        if (beginOfPSS) {
            turningForPSS = findTurningWithStrongestFlow();
        } else {
            turningForPSS = findNextTurningWithStrongestFlow();
        }

        // Determine phase serving the turning
        List<Phase> candidatePhasesForPSS = null;
        if (turningForPSS != null) {
            candidatePhasesForPSS = this.getJunction().getPhasesForTurnings(turningForPSS.getId());
        }

        // TODO Falls mehrere Phasen das Turning enthalten, wähle die Phase,
        // deren Länge am besten zur synchronisierten Phase des Vorgängers passt
        if (candidatePhasesForPSS != null && !candidatePhasesForPSS.isEmpty()) {
            final Phase phaseForPSS = candidatePhasesForPSS.get(0);
            return phaseForPSS.getId();
        }

        return -1;
    }

    /**
     * This is not the first node of the PSS. The synchronized
     * phase needs to link predecessor and successor in the PSS.
     *
     * @return
     */
    private Turning findNextTurningWithStrongestFlow() {
        Turning turningForPSS = null;

        // Determine incoming section for synchronized traffic
        final Offset offsetPredecessor = predecessor.getOffsetForNode(getId());
        final Section inSectionForPSS = offsetPredecessor.getPath().get(offsetPredecessor.getPath().size() - 1);

        // Determine outgoing section for synchronized traffic
        if (!this.endOfPSS) {
            // This node is not the last node of the PSS
            final Offset offsetSuccessor = getOffsetForNode(primarySuccessor.getId());
            final Section outSectionForPSS = offsetSuccessor.getPath().get(0);

            // Determine turning connecting predecessor and successor
            for (Turning turning : this.getJunction().getTurnings(TrafficType.INDIVIDUAL_TRAFFIC)) {
                if (turning.getInSection() == inSectionForPSS && turning.getOutSection() == outSectionForPSS) {
                    turningForPSS = turning;
                    break;
                }
            }
        } else {
            // This is the last node of the PSS. Synchronisation
            // considers the strongest turning movement connection the
            // predecessor to any successor
            float maxFlow = 0;
            for (Turning turning : this.getJunction().getTurnings(TrafficType.INDIVIDUAL_TRAFFIC)) {
                if (turning.getInSection() == inSectionForPSS) {
                    float turningFlow = this.getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC,
                            intervalLengthForStream, turning.getId(), StatisticsCapabilities.FLOW);

                    if (maxFlow < turningFlow) {
                        maxFlow = turningFlow;
                        turningForPSS = turning;
                    }
                }
            }
        }
        return turningForPSS;
    }

    /**
     * This is the first node of the PSS.
     * Synchronization will consider the strongest turning movement
     * reaching the successor.
     *
     * @return
     */
    private Turning findTurningWithStrongestFlow() {
        Turning turningForPSS = null;

        final List<Turning> turnings = getJunction().getTurningsForNeighbour(primarySuccessor.getId());

        float maxFlow = 0;
        for (Turning turning : turnings) {
            float flow = getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC,
                    intervalLengthForStream, turning.getId(), StatisticsCapabilities.FLOW);
            if (maxFlow < flow) {
                maxFlow = flow;
                turningForPSS = turning;
            }
        }
        return turningForPSS;
    }

    /**
     * Used to decide on which incoming stream this node wants to synchronise.
     * From all incoming streams, the largest stream is selected and stored in
     * the {@code synchronizedStream} attribute.
     */
    private void determineStreamForSynchronisation() {
        // Reset der letzten Synchronisierung
        this.synchronisedStream = null;
        this.secondaryStream = null;

        // Aktualisieren der Basisinfos
        if (neighbours.isEmpty()) {
            findNeighbours();
        }

        // Bestimmen der m�glichen Vorg�nger
        final Map<Integer, OTCNodeSynchronized> posPreds = calculatePredecessors(!useNeighbourStreams);

        // Setzen der Streams
        if (!posPreds.isEmpty()) {
            if (posPreds.containsKey(1) && posPreds.get(1) != null) {
                OTCNodeSynchronized node = posPreds.get(1);
                this.synchronisedStream = node.getTrafficStreamForNode(this.getId());
            }

            if (posPreds.containsKey(2) && posPreds.get(2) != null) {
                OTCNodeSynchronized node = posPreds.get(2);
                this.secondaryStream = node.getTrafficStreamForNode(this.getId());
            }
        }
    }

    /**
     * Bestimmt einen tempor�r zu aktivierenden TLC, um die Versatzzeit zu
     * erreichen.
     *
     * @see #tempTLC
     */
    private void determineTempTLCParams() {
        switch (this.getJunction().getActiveTLC().getParameters().getType()) {
            case TLCTypes.FIXEDTIME:
                final AbstractTLC activeTLC = this.getJunction().getActiveTLC();
                final int idActivePhase = activeTLC.getCurrentPhaseID();
                final float time = OTCManager.getInstance().getTime();
                final float activePhaseLasts = time - this.getJunction().getActiveTLC().getTimeOfLastChange();

                final TrafficLightControllerParameters activeTLCParams = activeTLC.getParameters();

                final float[] durations = activeTLCParams.getGreenTimes();

                float remaining = 0;
                for (int i = idActivePhase - 1; i < durations.length; i++) {
                    remaining += durations[i];
                }
                remaining -= activePhaseLasts;

                timeToActivateTempTLC = time + remaining;
                setTlcChangeDelay(timeToActivateTempTLC);

                this.timeToDeactivateTempTLC = time + this.agreedCycleTime + this.offset;

                // Erzeuge "Zwischen-TLC"
                int tlcCycle = new Double(Math.floor(this.timeToDeactivateTempTLC - timeToActivateTempTLC)).intValue();

                try {
                    this.tempTLC = activeTLCParams.adaptCycleTime(tlcCycle);
                } catch (TLCException e) {
                    // tempTLCCycle was too short
                    tlcCycle += this.agreedCycleTime;
                    this.tempTLC = activeTLCParams.adaptCycleTime(tlcCycle);
                    this.timeToDeactivateTempTLC = time + 2 * this.agreedCycleTime + this.offset;
                }
            case TLCTypes.NEMA:
                //TODO Überarbeiten
                final AbstractTLC activeNEMATLC = this.getJunction().getActiveTLC();
                final int idActiveNEMAPhase = activeNEMATLC.getCurrentPhaseID();
                final float NEMAtime = OTCManager.getInstance().getTime();
                final float activeNEMAPhaseLasts = NEMAtime - this.getJunction().getActiveTLC().getTimeOfLastChange();

                final TrafficLightControllerParameters activeTLCNEMAParams = activeNEMATLC.getParameters();

                final float[] NEMAdurations = activeTLCNEMAParams.getGreenTimes();

                float NEMAremaining = 0;
                for (int i = idActiveNEMAPhase - 1; i < NEMAdurations.length; i++) {
                    NEMAremaining += NEMAdurations[i];
                }
                NEMAremaining -= activeNEMAPhaseLasts;

                timeToActivateTempTLC = NEMAtime + NEMAremaining;
                setTlcChangeDelay(timeToActivateTempTLC);

                this.timeToDeactivateTempTLC = NEMAtime + this.agreedCycleTime + this.offset;

                // Erzeuge "Zwischen-TLC"
                int tlcNEMACycle = new Double(Math.floor(this.timeToDeactivateTempTLC - timeToActivateTempTLC)).intValue();

                try {
                    this.tempTLC = activeTLCNEMAParams.adaptCycleTime(tlcNEMACycle);
                } catch (TLCException e) {
                    // tempTLCCycle was too short
                    tlcNEMACycle += this.agreedCycleTime;
                    this.tempTLC = activeTLCNEMAParams.adaptCycleTime(tlcNEMACycle);
                    this.timeToDeactivateTempTLC = NEMAtime + 2 * this.agreedCycleTime + this.offset;
                }
            default:
                // TODO Implement other TLC-types
        }
    }

    /**
     * Ermittelt, ob dieser Knoten Teil einer gr�nen Welle ist.
     */
    private void finalisePSSMechanism() {
        // Check der Einstellungen am lokalen Knoten
        if (!checkLocalPSS()) {
            // Lokale Einstellungen sind inkonsistent

            // Bereinigen
            predecessor.unsubscribeSuccessor(this.getId());
            predecessor = null;
        } // Else: Lokale Einstellungen sind konsistent

        // ActivePSS Flag setzen
        if (predecessor == null && primarySuccessor == null) {
            setPartOfPSS(false);
        } else {
            setPartOfPSS(true);
        }

        // begin und end setzen
        if (isPartOfPSS()) {
            beginOfPSS = predecessor == null;
            endOfPSS = primarySuccessor == null;
        } else {
            beginOfPSS = false;
            endOfPSS = false;
        }
    }

    /**
     * @return the nextSynchPhase
     */
    final int getNextSynchPhase() {
        return nextSynchPhase;
    }

    /**
     * Liefert die agreedCycleTime der gr�nen Welle, zu der dieser Knoten
     * geh�rt.
     *
     * @return the agreedCycleTime
     */
    public final int getAgreedCycleTime() {
        return this.agreedCycleTime;
    }

    /**
     * Liefert den Offset zwischen zwei benachbarten Kreuzungen. Wenn die
     * Kreuzungen nicht benachbart sind wird {@code null} zur�ckgeliefert.
     *
     * @param nodeID Die OTCNode ID der anfragenden Kreuzung
     * @return Der Offset, der zwischen dieser Kreuzung und der anfragenden
     * bestehen muss.
     */
    private Offset getOffsetForNode(final int nodeID) {
        if (neighbours.containsKey(nodeID)) {
            final List<Offset> offsets = this.getJunction().getOffsetForNode(
                    neighbours.get(nodeID).getJunction().getId());

            if (offsets == null || offsets.isEmpty()) {
                throw new OTCManagerException("Node is not known as neighbor");
            } else if (offsets.size() == 1) {
                return offsets.get(0);
            } else {
                float lengthBest = -1;
                Offset returnOffset = null;

                for (Offset offset : offsets) {
                    List<Section> sectionPath = offset.getPath();
                    if (sectionPath != null && !sectionPath.isEmpty()) {
                        float path = 0;
                        for (Section section : sectionPath) {
                            if (section != null && !Float.isNaN(section.getLength())) {
                                path += section.getLength();
                            }
                        }
                        if (path > 0 && path < lengthBest || lengthBest <= 0 && path > 0) {
                            lengthBest = path;
                            returnOffset = offset;
                        }
                    }
                }

                // Wenn alle Wege keine L�nge haben: R�ckgabe vom ersten
                // gefundenen
                if (returnOffset == null) {
                    returnOffset = offsets.get(0);
                }
                return returnOffset;
            }
        }
        throw new OTCManagerException("Node is not known as neighbor");
    }

    /**
     * Method used to determine all possible successors and returns a ordered
     * collection. Ordering is done by descending flow.
     *
     * @return A {@code HashMap} containing the {@code OTCNode}-IDs of
     * the neighbours as values. The keys are set using the order,
     * starting with one.
     */
    private Map<Integer, Integer> getOrderedSuccessorIDsForPred(final int givenPred) {
        Map<Integer, Integer> posSuccessorIDs = new HashMap<>();

        // Bestimme den aktuell besten Vorg�nger
        int predID;
        if (givenPred <= 0) {
            predID = determineCurrentPredecessorID();
        } else {
            predID = givenPred;
        }

        if (successorList.isEmpty()) {
            // Keine angemeldeten Successors
            return posSuccessorIDs;
        } else if (successorList.size() == 1) {
            // Ein angemeldeter Successor
            if (predID <= 0) {
                // Kein Predecessor zu ber�cksichtigen, alle Nachbarn �bernehmen
                if (successorList.get(0) != null) {
                    posSuccessorIDs.put(1, successorList.get(0).getId());
                }
            } else {
                // Predecessor bekannt: W�hle nur Nachbarn mit Verbindung
                final int id = successorList.get(0).getId();
                final List<Turning> turns = getJunction().getTurningsForNeighbour(id);

                if (turns.isEmpty()) {
                    return posSuccessorIDs;
                } else {
                    Turning finalTurning = null;
                    for (Turning turn : turns) {
                        if (checkTurningOrigin(turn, predID)) {
                            finalTurning = turn;
                        }
                    }
                    // Geht ein Mindestmass des Vorgaengerstromes auf das
                    // gewaehlte Turning ueber?
                    if (verifyTurning(finalTurning, predID)) {
                        posSuccessorIDs.put(1, id);
                    }

                    return posSuccessorIDs;
                }
            }
        } else {
            // Mehrere angemeldete moegliche Nachfolgeknoten
            final Map<Integer, OTCNodeSynchronized> posSucc = new HashMap<>();

            for (OTCNodeSynchronized successor : successorList) {
                if (predID < 0) {
                    // Kein Predecessor vorhanden: Alle Nachbarn ber�cksichtigen
                    if (successor != null) {
                        posSucc.put(successor.getId(), successor);
                    }
                } else {
                    // Ber�cksichtige nur die erreichbaren Nachbarn
                    List<Turning> turnings = getJunction().getTurningsForNeighbour(successor.getId());

                    // Check ob dieses Turning vom pred erreichbar ist
                    turnings.stream().filter(turning -> checkTurningOrigin(turning, predID)).forEach(turning -> {
                        // Ueberpruefe das Turning: Nimmt mindestens
                        // ein Drittel des Stroms vom pred auf?
                        if (verifyTurning(turning, predID)) {
                            posSucc.put(successor.getId(), successor);
                        }
                    });
                }
            }

            posSuccessorIDs = orderPossibleSuccessorsByFlow(posSucc);
        }

        return posSuccessorIDs;
    }

    /**
     * Methode liefert die Dauer der Phase zur�ck, in der das �bergebene Turning
     * bedient wird. Bei Vorkommen in mehreren Phasen wird die l�ngste Dauer
     * zur�ckgegeben.
     *
     * @param turningID Die
     * @return
     */
    private int getPhaseDurationForTurning(final int turningID) {
        final List<Integer> phases = getJunction().getPhaseIds();
        float phaseDuration = -1;

        for (int i : phases) {
            Phase phase = this.getJunction().getPhaseById(i);
            if (!phase.isInterphase()) {
                List<SignalGroup> signalGroups = phase.getSignalGroups();

                for (SignalGroup signalGroup : signalGroups) {
                    List<Turning> turnings = signalGroup.getTurnings();

                    for (Turning turning : turnings) {
                        if (turning.getId() == turningID) {
                            if (phase.getDefaultDuration() >= phaseDuration) {
                                phaseDuration = phase.getDefaultDuration();
                            }
                        }
                    }
                }
            }
        }
        return (int) phaseDuration;
    }

    final void setPredecessor(final OTCNodeSynchronized predecessor) {
        this.predecessor = predecessor;
    }

    final OTCNodeSynchronized getPrimarySuccessor() {
        return primarySuccessor;
    }

    final void setPrimarySuccessor(final OTCNodeSynchronized primarySuccessor) {
        this.primarySuccessor = primarySuccessor;
    }

    /**
     * Liefert einen String zurück, der eine komplette Beschreibung einer
     * eventuell errechneten grünen Welle zur�ckliefert.
     *
     * @return Die Beschreibung der grünen Welle.
     */
    final String getPSSDescription() {
        final StringBuilder description = new StringBuilder();
        final String linesep = System.getProperty("line.separator");

        description.append("Node ").append(this);

        if (isPartOfPSS()) {
            description.append(" (Active PSS)").append(linesep);
        } else {
            description.append(" (No active PSS)").append(linesep);
        }

        if (predecessor != null) {
            description.append("predecessor: node ").append(predecessor).append(linesep);
        }

        if (primarySuccessor != null) {
            description.append("successor: node ").append(primarySuccessor).append(linesep);
        }

        if (synchronisedStream != null) {
            description.append("chosen predecessor: ").append(synchronisedStream).append(linesep);
        }

        if (secondaryStream != null) {
            description.append("second best predecessor: ").append(secondaryStream).append(linesep);
        }

        // Alle TrafficStreams
        description.append("1) Results of the TrafficStream calculation: ").append(linesep);
        if (trafficStreams != null && !trafficStreams.isEmpty()) {
            int i = 1;
            for (TrafficStream theStream : trafficStreams.values()) {
                description.append("- TS (").append(i).append("): ").append(theStream).append(linesep);
                i++;
            }
        }

        // Alle gefundenen Nachbarn
        description.append("2) Neighbourhood information: ").append(linesep);
        if (!neighbours.isEmpty()) {
            for (OTCNode nodeNeighbour : neighbours.values()) {
                description.append(nodeNeighbour.getId()).append(" ").append(nodeNeighbour.getName()).append(", ");
            }
            description.append(linesep);
        }

        // Alle angemeldeten Nachfolger
        if (successorList != null && !successorList.isEmpty()) {
            description.append("3) Following successors have been registered: ").append(linesep);
            int j = 1;
            for (OTCNode tempNodeForIt : successorList) {
                description.append("Successor ").append(j).append(" is node ").append(tempNodeForIt).append(linesep);
                j++;
            }
        }

        // Informationen zur ausgehandelten Umlaufzeit
        description.append("Desired cycle time: ").append(desiredCycleTime).append(linesep);
        if (replaceTLC != null) {
            description.append("Cycle time in PSS will be: ").append(new Float(replaceTLC.getCycleTime()).intValue()).append(linesep);
        }

        description.append("Agreed cycle time: ").append(agreedCycleTime).append(linesep);
        if (predecessor != null) {
            description.append("Predecessor's agreed cycle time: ").append(predecessor.agreedCycleTime).append(linesep);
        }

        if (primarySuccessor != null) {
            description.append("Successor's agreed cycle time: ").append(primarySuccessor.agreedCycleTime).append(linesep);
        }

        if (predecessor != null) {
            description.append("Predecessor's offset: ").append(predecessor.offset).append(linesep);
            description.append("Start of predecessor's " + "synchronized phase: ").append(predecessor.syncStart).append(linesep);
            description.append("Distance from predecessor: ").append(predecessor.getOffsetForNode(this.getId())).append(linesep);
        }

        description.append("Start of own synchronized phase: ").append(this.syncStart).append(linesep);
        description.append("Determined offset: ").append(this.offset).append(linesep);
        description.append("Determined adjustment caused by queues: ").append(this.queueBasedAdjustment).append(linesep);
        description.append("beginOfPSS ").append(this.beginOfPSS).append(", endOfPSS ").append(this.endOfPSS).append(linesep);

        if (this.tempTLC != null) {
            description.append("TLC CHANGE:").append(linesep);
            description.append("timeToActivateTempTLC: ").append(this.timeToActivateTempTLC).append(linesep);
            description.append("timeToDeactivateTempTLC: ").append(this.timeToDeactivateTempTLC).append(linesep);
            description.append("tempTLC: ").append(this.tempTLC).append(linesep);
        }

        if (this.replaceTLC != null) {
            description.append("replaceTLC: ").append(this.replaceTLC).append(linesep);
        }

        return description.toString();
    }

    /**
     * Used to determine the (strongest) traffic stream for the particular
     * neighbour.
     *
     * @param id the id of the neighbour's node ({@code OTCNode.getId()})
     * @return the Traffic Stream relevant for this neighbour
     */
    private TrafficStream getTrafficStreamForNode(final int id) {
        if (trafficStreams.isEmpty()) {
            calculateStreams();
        }

        if (trafficStreams.containsKey(id)) {
            return this.trafficStreams.get(id);
        } else {
            return new TrafficStream(-1, id, Float.NaN, Float.NaN);
        }
    }

    public final void initialisePSSValues() {
        this.agreedCycleTime = 0;
        this.desiredCycleTime = 0;

        this.successorList = new ArrayList<>(3);
        this.trafficStreams = new HashMap<>(5);

        this.predecessor = null;
        this.primarySuccessor = null;
        this.confirmedPredecessor = false;

        this.beginOfPSS = false;
        this.endOfPSS = false;
        this.predecessorIsSecondChoice = false;
        this.queriedSecondPred = false;
        this.primStreamPreliminaryNo = false;

        this.runSynchPhase = false;
        this.nextSynchPhase = -1;
        this.timeToNextSynchPhase = -1;

        this.offset = 0;
        this.syncStart = 0;
        this.timeZeroForPSS = 0;

        this.timeToActivateTempTLC = -1;
        this.tempTLC = null;
        this.timeToDeactivateTempTLC = -1;

        this.synchronisedStream = null;
        this.secondaryStream = null;

        setPartOfPSS(false);
    }

    final boolean isBeginOfPSS() {
        return beginOfPSS;
    }

    final void setBeginOfPSS(final boolean beginOfPSS) {
        this.beginOfPSS = beginOfPSS;
    }

    final boolean isEndOfPSS() {
        return endOfPSS;
    }

    final void setEndOfPSS(final boolean endOfPSS) {
        this.endOfPSS = endOfPSS;
    }

    final boolean isRunSynchPhase() {
        return runSynchPhase;
    }

    /**
     * Logs the updates of PSSs and their reason (new partnerships or ACT).
     *
     * @param newPartners
     * @param newACT
     * @param act
     * @see #checkChangeDemand()
     */
    private void logPSSUpdate(final boolean newPartners, final boolean newACT, final int act) {
        try {
            final FileOutputStream popSizeLogFile = new FileOutputStream("logs/"
                    + OTCManager.getInstance().getFilenamePrefix() + "_DPSSUpdateLog.csv", true);
            final PrintStream popSizesLog = new PrintStream(popSizeLogFile);

            int reason = 0;

            // See Cases in [TPR+08]!
            if (newPartners) {
                reason = 4;
            } else if (newACT) {
                reason = 2;
            }
            // TODO Case 1: Local change of TLC!

            // Log time; id of first PSS node; act for pss; reason for change;
            final Formatter f = new Formatter();
            popSizesLog.println(f.format("%.2f; %d; %d; %d;", OTCManager.getInstance().getTime(), this.getId(),
                    agreedCycleTime, reason));
            // Future
            popSizesLog.println(f.format("%.2f; %d; %d; %d;", OTCManager.getInstance().getTime(), this.getId(), act,
                    reason));

            popSizesLog.close();
            popSizeLogFile.close();
            f.close();
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * Handelt die Umlaufzeit innerhalb der grünen Welle aus.
     */
    private void negotiateCycleTime() {
        this.runSynchPhase = false;
        this.nextSynchPhase = -1;

        // Aushandeln der Umlaufzeit nur bei aktiver grüner Welle
        if (isPartOfPSS() && beginOfPSS) {
            updateCycleTime(true);
        }
    }

    /**
     * Wird von den potentiellen Vorg�ngern genutzt, um eine gr�ne Welle zu
     * best�tigen oder abzulehnen.
     *
     * @param predecessorID die id des potentiellen Vorg�ngers
     * @param answer        Flag, das angibt, ob Vorg�nger mit diesem Knoten eine gr�ne
     *                      Welle etablieren will (true) oder nicht (false)
     * @param preliminary   Flag, ob Benachrichtigung vorl�ufig (true) oder endg�ltig
     *                      (false) ist
     * @return Flag, ob Nachricht verarbeitet werden konnte
     */
    private boolean notifySuccessor(final int predecessorID, final boolean answer, final boolean preliminary) {
        if (synchronisedStream != null && synchronisedStream.getOriginNodeID() == predecessorID) {
            return setPredecessor(predecessorID, preliminary, answer);
        } else if (secondaryStream != null && secondaryStream.getOriginNodeID() == predecessorID) {
            // Antwort von zweitbestem Strom
            if (answer) {
                if (confirmedPredecessor && predecessor != null) {
                    return false;
                }
                predecessor = (OTCNodeSynchronized) neighbours.get(predecessorID);
                confirmedPredecessor = !preliminary;
                predecessorIsSecondChoice = true;
            }
            return true;
        } else {
            log.debug("Received notification from unknown predecessor!");
        }
        return false;
    }

    private boolean setPredecessor(int predecessorID, boolean preliminary, boolean answer) {
        if (preliminary) {
            if (answer) {
                // Vorgänger bestätigt
                predecessor = (OTCNodeSynchronized) neighbours.get(predecessorID);
                confirmedPredecessor = false;
            } else {
                // Vorgänger lehnt ab
                predecessor = null;
                confirmedPredecessor = false;
                primStreamPreliminaryNo = true;

                // Neuen Vorg�nger w�hlen
                if (secondaryStream != null
                        && secondaryStream.getOriginNodeID() != synchronisedStream.getOriginNodeID()
                        && secondaryStream.getOriginNodeID() != determineCurrentPredecessorID()) {
                    // Zweite Wahl eines Vorg�ngers m�glich - �berpr�fung

                    // Waehlen des zweitbesten Vorgaengers
                    final OTCNodeSynchronized preliminaryPred = (OTCNodeSynchronized) neighbours
                            .get(secondaryStream.getOriginNodeID());
                    if (verifyPSSForNode(preliminaryPred)) {
                        // Keine Konflikte gefunden: Anmeldung
                        preliminaryPred.registerSuccessor(this.getId());
                        queriedSecondPred = true;
                        log.debug("Second choice for node " + getId() + " is node " + preliminaryPred.getId());
                    } else {
                        // Sonst: Keine zweite Wahl m�glich
                        log.debug("Second choice for node " + getId() + " REJECTED ! (Node "
                                + preliminaryPred.getId() + ")");
                    }
                }
            }
        } else {
            if (answer) {
                if (confirmedPredecessor && predecessor != null) {
                    return false;
                }

                // Vorg�nger best�tigt gr�ne Welle
                confirmedPredecessor = true;
                // Setzen des Predecessors
                predecessor = (OTCNodeSynchronized) neighbours.get(predecessorID);
            } else {
                if (predecessorIsSecondChoice) {
                    // TODO hier irgendwas machen?
                } else {
                    // Vorg�nger lehnt ab
                    // reset, wenn vorher akzeptiert wurde
                    predecessor = null;
                    confirmedPredecessor = true;

                    if (!queriedSecondPred) {
                        // Neue predecessor Wahl
                        if (secondaryStream != null
                                && secondaryStream.getOriginNodeID() != synchronisedStream.getOriginNodeID()) {
                            final OTCNodeSynchronized preliminaryPred = (OTCNodeSynchronized) neighbours
                                    .get(secondaryStream.getOriginNodeID());
                            preliminaryPred.registerSuccessor(this.getId());
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Method used to determine an order for a collection of nodes. Therefore,
     * the best stream for each node will be identified and the nodes will be
     * ordered by the descending flow values.
     *
     * @param unorderedSuccs The unordered possible successors
     * @return A {@code HashMap} containing the {@code OTCNode}-IDs of
     * the neighbours as values. The keys are set using the order,
     * starting with one.
     */
    private Map<Integer, Integer> orderPossibleSuccessorsByFlow(final Map<Integer, OTCNodeSynchronized> unorderedSuccs) {
        final Map<Integer, Integer> orderedSuccs = new HashMap<>();

        if (unorderedSuccs.isEmpty()) {
            return orderedSuccs;
        } else if (unorderedSuccs.size() == 1) {
            final int id = unorderedSuccs.values().iterator().next().getId();
            orderedSuccs.put(1, id);
        } else {
            boolean usePred = true;
            final int predID = determineCurrentPredecessorID();
            if (predID <= 0) {
                usePred = false;
            }
            final List<TrafficStream> toBeSorted = new ArrayList<>();

            // Bestimme f�r jeden Node den FLOW
            for (OTCNodeSynchronized node : unorderedSuccs.values()) {
                List<Turning> turnings = getJunction().getTurningsForNeighbour(node.getId());

                if (turnings != null && !turnings.isEmpty()) {
                    float bestStream = -1;

                    for (Turning turn : turnings) {
                        if (!usePred || checkTurningOrigin(turn, predID)) {
                            float stream = getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC,
                                    intervalLengthForStream, turn.getId(), StatisticsCapabilities.FLOW);

                            if (!Float.isNaN(stream) && stream > bestStream) {
                                bestStream = stream;
                            }
                        }
                        // else: turning kommt nicht vom pred
                    }

                    // Best Stream
                    TrafficStream stream = new TrafficStream(getId(), node.getId(), bestStream, -1f);
                    toBeSorted.add(stream);
                }
            }
            // Sortiere die Knoten nach dem FLOW-Wert
            final List<TrafficStream> sortedNodes = sortNodes(toBeSorted);

            // F�lle den R�ckgabeContainer
            int posID = 1;
            for (TrafficStream tmpTS : sortedNodes) {
                orderedSuccs.put(posID, tmpTS.getTargetNodeID());
                posID++;
            }

        }
        return orderedSuccs;
    }

    /**
     * Bestimmt die Nachbarknoten f�r diesen {@code OTCNode}.
     */
    final void findNeighbours() {
        if (neighbours == null) {
            this.neighbours = getJunction().getNeighbouringNodes();
        }
    }

    /**
     * Wird genutzt, um sich als Nachfolger einer gr�nen Welle zu registrieren.
     *
     * @param successorID die Knoten-ID des Nachfolgers
     * @return R�ckgabe gibt an, ob Registrierung erfolgreich war oder nicht
     */
    private void registerSuccessor(final int successorID) {
        findNeighbours();

        if (neighbours.containsKey(successorID) && !successorList.contains(neighbours.get(successorID))) {
            // Hinzuf�gen zu Liste der angemeldeten Successors
            this.successorList.add((OTCNodeSynchronized) neighbours.get(successorID));
        }
    }

    /**
     * Aktiviert den tempor�ren TLC oder den f�r die gr�ne Welle ausgew�hlten
     * TLC zum richtigen Zeitpunkt, um die gew�nschte Versatzzeit einzuhalten.
     *
     * @see #tempTLC
     * @see #replaceTLC
     */
    final void replaceTempTLC() {
        if (this.timeToActivateTempTLC > 0 && this.timeToActivateTempTLC <= OTCManager.getInstance().getTime()) {
            changeTLC(this.tempTLC);

            // Zeit bis zum n�chsten erlaubten TLC-Wechsel verk�rzen, da der
            // aktivierte TLC nur zwischenzeitlich aktiv ist, um die Versatzzeit
            // zu erreichen
            setTlcChangeDelay(this.timeToDeactivateTempTLC);

            // tempTLC zur�cksetzen
            this.timeToActivateTempTLC = -1;
            this.tempTLC = null;
        }

        if (this.timeToDeactivateTempTLC > 0 && this.timeToDeactivateTempTLC <= OTCManager.getInstance().getTime()) {
            changeTLC(this.replaceTLC);

            // replaceTLC zur�cksetzen
            this.timeToDeactivateTempTLC = -1;
            this.replaceTLC = null;
        }
    }

    /**
     * Provides preliminary information for registered successors.
     */
    private void runSuccessorTest() {
        if (!successorList.isEmpty()) {
            // Wähle den besten Nachfolger
            final int id = chooseTempSuccessorID();

            log.debug("Knoten " + getId() + " waehlt als Nachfolger Knoten " + id);

            // Vorl�ufiges Ergebnis bekanntmachen
            for (OTCNodeSynchronized node : successorList) {
                if (node.getId() == id) {
                    node.notifySuccessor(this.getId(), true, true);
                    log.debug("Node " + getId() + " sends Node " + node.getId() + " ACK");
                } else {
                    node.notifySuccessor(this.getId(), false, true);
                    log.debug("Node " + getId() + " sends Node " + node.getId() + " REJECT");
                }
            }
        }
    }

    /**
     * Hauptmethode zur dezentralen Erzeugung gr�ner Wellen.
     */
    final void runSynchronisationMechanism(final int step) {
        if (step == 0) {
            initialisePSSValues();
        } else if (step == 1) {
            // Vorbereitung und Berechnung der am st�rksten belasteten Abbiegebeziehungen
            findNeighbours();
            calculateStreams();
        } else if (step == 2) {
            // Bestimmen des gew�nschten Vorg�ngerknotens
            determineStreamForSynchronisation();

            if (synchronisedStream != null) {
                // Benachrichtige Vorg�nger
                predecessor = (OTCNodeSynchronized) neighbours.get(synchronisedStream.getOriginNodeID());
                predecessor.registerSuccessor(this.getId());
                confirmedPredecessor = false;
            } // else: Keine Nachbarn
        } else if (step == 3) {
            // Bestimmen des gew�nschten Nachfolgers
            runSuccessorTest();
        } else if (step == 4) {
            // �berpr�fe gr�ne Welle: Vorg�nger/Nachfolger-Matching +
            // ACK/REJ
            calculateGreenWave();
        } else if (step == 5) {
            // Abschlie�en der Partnerfindung
            finalisePSSMechanism();
        } else if (step == 6) {
            // Aushandeln der Umlaufzeit und anschlie�endes Setzen der
            // TLCs
            negotiateCycleTime();
        } else {
            log.warn("Knoten " + getId() + ": Ung�ltiger Aufruf runSynchronisationMechanism " + step);
        }
    }

    final void setConfirmedPredecessor(final boolean confirmedPredecessor) {
        this.confirmedPredecessor = confirmedPredecessor;
    }

    public final void setDesiredCycleTime(final int desiredCycleTime) {
        this.desiredCycleTime = desiredCycleTime;
    }

    final void setPredecessorIsSecondChoice(final boolean predecessorIsSecondChoice) {
        this.predecessorIsSecondChoice = predecessorIsSecondChoice;
    }

    final void setTimeToNextSynchPhase(final float timeToNextSynchPhase) {
        this.timeToNextSynchPhase = timeToNextSynchPhase;
    }

    /**
     * Method used to sort a {@code ArrayList} of
     * {@code TrafficStream} objects with respect to the flow-attribute.
     * The order is defined by the flow DESC.
     *
     * @param toBeSorted The {@code List} containing the
     *                   {@code TrafficStream} objects, which should be sorted
     * @return The ordered list
     */
    private List<TrafficStream> sortNodes(final List<TrafficStream> toBeSorted) {
        final List<TrafficStream> nodes = new ArrayList<>();

        if (toBeSorted.size() == 1) {
            nodes.add(toBeSorted.get(0));
        } else if (!toBeSorted.isEmpty()) {
            for (TrafficStream stream : toBeSorted) {
                // Bestimme Position in neuer Liste
                int position = -1;

                if (!nodes.isEmpty()) {
                    TrafficStream curStream;

                    boolean keepRunning = true;
                    int i = 0;
                    Iterator<TrafficStream> tmpIt = nodes.iterator();

                    while (keepRunning) {
                        curStream = tmpIt.next();

                        // Position gefunden?
                        if (curStream.getStreamStrength() > stream.getStreamStrength()) {
                            position = i;
                            keepRunning = false;
                        }

                        i++;

                        // Abbruch?
                        if (keepRunning && !tmpIt.hasNext()) {
                            keepRunning = false;
                        }
                    }
                } else {
                    position = 0;
                }

                // F�ge an Position ein
                if (position < 0) {
                    // Aktueller Stream ist h�chster: hinten ran
                    nodes.add(stream);
                } else {
                    nodes.add(position, stream);
                }
            }
        }

        // Invertiere die Reihenfolge (nun st�rkster Strom am Anfang)
        Collections.reverse(nodes);

        return nodes;
    }

    /**
     * Unsubscribe an already registered successor.
     */
    private void unsubscribeSuccessor(final int sucID) {
        if (neighbours.containsKey(sucID) && successorList.contains(neighbours.get(sucID))) {
            // Successor bekannt - abmelden
            successorList.remove(neighbours.get(sucID));
            if (primarySuccessor.getId() == sucID) {
                primarySuccessor = null;
            }
        } else {
            log.warn("Unknown successor tried to unsubscribe - successor ID:" + sucID + " at node: " + getId());
        }
    }

    /**
     * Dient dem Aushandeln der Umlaufzeit. Die jeweiligen Nachbarn k�nnen bei
     * �nderungen ihrer {@code AgreedCycleTime} mitteilen, dass eine
     * �berpr�fung notwendig ist.
     *
     * @param direction {@code true}, falls die gr�ne Welle zum Ende hin
     *                  durchlaufen wird, anderenfalls {@code false}
     */
    private void updateCycleTime(final boolean direction) {
        // Umlaufzeit muss nur ausgehandelt werden, wenn eine grüne Welle
        // besteht
        if (isPartOfPSS()) {
            if (direction) {
                // Hinrichtung der Welle

                // Aktualisiere DCT, ACT und Verantwortlichkeit
                try {
                    final float[] situation = getSituationWithFlowForecast(DataSource.STATISTICS);
                    this.desiredCycleTime = getTLCSelector().determineDesiredCycleTime(situation);
                    log.debug(getId() + " DCT " + this.desiredCycleTime);
                } catch (Layer1Exception | ClassifierException | OTCManagerException e) {
                    log.error(OTCManager.getInstance().getTime() + ": " + e);
                }
                this.agreedCycleTime = this.desiredCycleTime;

                if (!beginOfPSS) {
                    // Vergleich mit agreedCycleTime des Vorg�ngers
                    final int predecessorCycleTime = predecessor.agreedCycleTime;
                    if (agreedCycleTime < predecessorCycleTime) {
                        this.agreedCycleTime = predecessorCycleTime;
                    }
                }

                if (!endOfPSS) {
                    // Aktiviere Vergleich beim Nachfolger
                    primarySuccessor.updateCycleTime(true);
                } else {
                    // Letzter Knoten erreicht: Beginne R�ckrichtung
                    predecessor.updateCycleTime(false);
                }
            } else {
                // R�ckrichtung der Welle
                final int successorCycleTime = primarySuccessor.agreedCycleTime;
                // Vergleich mit agreedCycleTime des Nachfolgers
                if (agreedCycleTime < successorCycleTime) {
                    this.agreedCycleTime = successorCycleTime;
                }

                if (!beginOfPSS) {
                    // Aktiviere Vergleich beim Vorg�nger
                    predecessor.updateCycleTime(false);
                } else {
                    // Erster Knoten erreicht, weiter mit Versatzzeiten
                    determineOffsetAndSelectTLC();
                }
            }
        } else {
            log.warn("UpdateCycleTime() called for node " + getId() + " although node is not part of a PSS!");
        }
    }

    /**
     * Methode �berpr�ft den aktuell geschalteten TLC und vergleicht ihn mit der
     * aktuell vom LCS ausgew�hlten Aktion. Wenn die Abweichung eine bestimmte
     * Toleranzgrenze �berschreitet, liefert sie {@code true} zur�ck, sonst
     * {@code false}.
     *
     * @return {@code true} wenn signifikante Abweichung,
     * {@code false} sonst
     */
    private boolean verifyCurrentTLC() {
        if (log.isDebugEnabled()) {
            log.debug("Verifying TLC " + getId());
        }

        float[] situation;
        TrafficLightControllerParameters desiredTLC;

        // Hole die gew�nschten aktuellen TLC Einstellungen (unter Vorgabe der
        // aktuellen ACT)
        try {
            situation = getSituationWithFlowForecast(DataSource.STATISTICS);
            desiredTLC = getTLCSelector().determineDesiredTLC(situation, agreedCycleTime);
        } catch (Layer1Exception | ClassifierException | OTCManagerException e) {
            log.error("Unable to verify TLC - invalid situation." + e.getMessage());
            return false;
        }

        final TrafficLightControllerParameters currentTLC = this.getJunction().getActiveTLC().getParameters();

        // Vergleiche current und desired TLC
        if (desiredTLC != null && desiredTLC.getType() == currentTLC.getType()) {
            // Vergleiche TLCs auf Gleichheit
            if (currentTLC.hashCode() == desiredTLC.hashCode()) {
                log.debug("Desired TLC is equal to current TLC - no change needed.");
                return false;
            } else {
                // Neuer TLC unterscheidet sich von aktuellem - �berpr�fung
                // Unterscheide die verschiedenen TLC Typen
                switch (currentTLC.getType()) {
                    case TLCTypes.FIXEDTIME:
                        // Prediction des aktuellen TLC
                        final float predictionCurrentAction = getTLCSelector().determinePredictionForActiveAction(
                                currentTLC.hashCode());

                        // Prediction des neuen TLC
                        final float predictionNextAction = getTLCSelector().determinePredictionForAction(
                                desiredTLC.hashCode(), situation, agreedCycleTime);

                        final float difference = predictionNextAction - predictionCurrentAction;

                        if (predictionCurrentAction < 0 || predictionNextAction < 0) {
                            log.debug("Unable to estimate prediction for one old/new TLC");
                            return false;
                        }

                        if (difference > 0 && difference > minimumPredictionDifference) {
                            return true;
                        }
                        break;
                    case TLCTypes.FIXEDTIMERECALL:
                        // TODO Fehlt
                        break;
                    case TLCTypes.NEMA:
                        // Prediction des aktuellen TLC
                        final float predictionCurrentNEMAAction = getTLCSelector().determinePredictionForActiveAction(
                                currentTLC.hashCode());

                        // Prediction des neuen TLC
                        final float predictionNextNEMAAction = getTLCSelector().determinePredictionForAction(
                                desiredTLC.hashCode(), situation, agreedCycleTime);

                        final float NEMAdifference = predictionNextNEMAAction - predictionCurrentNEMAAction;

                        if (predictionCurrentNEMAAction < 0 || predictionNextNEMAAction < 0) {
                            log.debug("Unable to estimate prediction for one old/new TLC");
                            return false;
                        }

                        if (NEMAdifference > 0 && NEMAdifference > minimumPredictionDifference) {
                            return true;
                        }
                        break;
                    // TODO Überarbeiten
                    default:
                        throw new TLCException("Received invalid TLC - unknown type.");
                }
            }
        } else {
            throw new TLCException("Received invalid TLC Params - unknown type or null.");
        }

        return false;
    }

    /**
     * Method verifies current status of DPSS.
     *
     * @param nodes     Already contacted nodes (by ID)
     * @param direction {@code true} direction to begin of DPSS,
     *                  {@code false} direction to end of DPSS
     * @return List of all nodes on path
     */
    private List<Integer> verifyPSS(final Collection<Integer> nodes, final boolean direction) {
        log.debug("current node " + getId());

        List<Integer> clonedList = new ArrayList<>();
        clonedList.addAll(nodes);

        // add myself
        clonedList.add(getId());

        // Verify
        if (nodes.contains(getId())) {
            return clonedList;
        }

        // Unterscheidung nach Richtung
        if (direction) {
            // Richtung: Welle nach vorne durch
            final int predId = determineCurrentPredecessorID();
            if (predId <= 0 || !neighbours.containsKey(predId)) {
                // Kein Vorgänger gefunden
                return clonedList;
            } else {
                final OTCNodeSynchronized pred = (OTCNodeSynchronized) neighbours.get(predId);
                return pred.verifyPSS(clonedList, true);
            }
        } else {
            // Richtung: Welle nach hinten durchlaufen
            final int sucID = determineCurrentPredecessorID();
            if (sucID <= 0 || !neighbours.containsKey(sucID)) {
                // Kein Nachfolger gefunden
                return clonedList;
            } else {
                final OTCNodeSynchronized sucNode = (OTCNodeSynchronized) neighbours.get(sucID);
                return sucNode.verifyPSS(clonedList, false);
            }
        }
    }

    /**
     * Checks if the DPSS stays consistent by using second choice predecessor.
     *
     * @param posPred The possible predecessor
     * @return Consistent or not
     */
    private boolean verifyPSSForNode(final OTCNodeSynchronized posPred) {
        final Collection<Integer> checked = new ArrayList<>();

        // M�glicher Pred sammelt seinen Pfad ein
        final List<Integer> nodesOnPathPred = posPred.verifyPSS(new ArrayList<>(), true);
        for (int node : nodesOnPathPred) {
            if (checked.contains(node)) {
                log.warn("Invalid path found!");
                return false;
            } else {
                checked.add(node);
            }
        }

        // Auswählen des aktuellen Nachfolgers
        final Map<Integer, Integer> posSuccs = getOrderedSuccessorIDsForPred(posPred.getId());
        OTCNodeSynchronized posSuc = null;
        if (!posSuccs.isEmpty()) {
            posSuc = (OTCNodeSynchronized) neighbours.get(posSuccs.get(1));
        }

        // Check des Nachfolgepfades
        List<Integer> nodesOnPathSuc = null;
        if (posSuc != null) {
            nodesOnPathSuc = posSuc.verifyPSS(new ArrayList<>(), false);
        }

        if (nodesOnPathSuc != null) {
            for (Integer node : nodesOnPathSuc) {
                if (checked.contains(node)) {
                    log.warn("Invalid path found!");
                    return false;
                } else {
                    checked.add(node);
                }
            }
        }

        return true;
    }

    /**
     * @param toBeVerified
     * @param predID
     * @return
     */
    private boolean verifyTurning(final Turning toBeVerified, final int predID) {
        if (toBeVerified == null || predID < 0) {
            return false;
        }

        // Bestimme den Wert des zu ueberpruefenden Turnings
        final float flow = getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC, intervalLengthForStream,
                toBeVerified.getId(), StatisticsCapabilities.FLOW);
        if (Float.isNaN(flow) || Float.isInfinite(flow) || flow <= 0) {
            return false;
        }

        // Hole alle Turning die von dem Predecessor bedient werden
        final Collection<Turning> turns = new ArrayList<>();
        final List<Turning> allTurns = getJunction().getTurnings(TrafficType.INDIVIDUAL_TRAFFIC);
        turns.addAll(allTurns.stream().filter(turning -> checkTurningOrigin(turning, predID)).map(turning -> turning).collect(Collectors.toList()));
        if (turns.isEmpty()) {
            return false;
        }

        // Bestimme den gesamten FLOW
        float sumFlow = 0;
        for (Turning turning : turns) {
            if (turning.getId() > 0) {
                float curFlow = getLayer1Observer().getTurningStatistic(TrafficType.INDIVIDUAL_TRAFFIC,
                        intervalLengthForStream, turning.getId(), StatisticsCapabilities.FLOW);
                if (!Float.isNaN(curFlow)) {
                    sumFlow += curFlow;
                }
            }
        }

        return flow / sumFlow > 0.34f;
    }
}
