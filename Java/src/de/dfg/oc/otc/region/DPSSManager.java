package de.dfg.oc.otc.region;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.TLCException;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.Layer1Exception;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager for the distributed progressive signal system.
 * DPSSManager is implemented with the singleton pattern.
 */
public final class DPSSManager {
    private static final Logger log = Logger.getLogger(DPSSManager.class);
    private static final DPSSManager INSTANCE = new DPSSManager();
    /**
     * Flag, ob gerade eine Berechnung der gr�nen Welle erfolgt.
     */
    private boolean activeRunForPSS;
    /**
     * Interval, nach dem die aktuelle Umlaufzeit �berpr�ft wird.
     */
    private float checkPSSInterval = 1000;
    /**
     * Flag indicating whether decentral organised PSS is active or not.
     */
    private boolean decentralPSSactive;
    /**
     * Flag, ob RegionalManager genutzt werden soll.
     */
    private boolean isRegionActive;
    /**
     * ID der PSS Phase, die als n�chstes ausgef�hrt wird.
     */
    private int nextPhaseForPSS;
    /**
     * Time of next check of green wave.
     */
    private float nextTimeForPSSCheck = -1;
    /**
     * Time of next complete calculation of green wave.
     */
    private float nextTimeForPSSRun = -1;
    private float nextTimeForRegionInfo = 300;
    /**
     * Time of next complete calculation of green wave by the regional manager.
     */
    private float nextTimeForRegionRun = 250;
    /**
     * Interval, nach dem die gesamte gr�ne Welle neu berechnet wird.
     */
    private float recalculatePSSInterval = 54000;
    /**
     * Möchte ein Knoten Änderungen am PSS?
     */
    private boolean updateDPSS;
    /**
     * Ab welcher DPSS-Phase sind Änderungen gewünscht?
     */
    private int updateDPSSFromPhase = -1;

    private DPSSManager() {
    }

    public static DPSSManager getInstance() {
        return INSTANCE;
    }

    /**
     * Bestimmt (durch LCS-Aufruf) eine passende Steuerung f�r Knoten, die nicht
     * an einer gr�nen Welle teilnehmen.
     *
     * @param junction ein OTC-gesteuerter Knoten
     */
    private void determineTLCforNonDPSSJunction(final AimsunJunction junction) {
        final OTCNode node = junction.getNode();
        // Nur Knoten, die an keiner grünen Welle teilnehmen
        if (!node.isPartOfPSS()) {
            // Verteile Reward und bestimme neuen TLC.
            // Check der TLC-Mindestlaufzeit erfolgt innerhalb von distributeRewardAndSelectAction()
            try {
                final TrafficLightControllerParameters tlcParams = node.distributeRewardAndSelectAction();
                if (tlcParams.hashCode() != junction.getActiveTLC().getParameters().hashCode()) {
                    // Neuer TLC unterscheidet sich von aktuellem TLC: Wechsel!
                    node.changeTLC(tlcParams);
                }
            } catch (OTCManagerException e) {
                log.error(e.getMessage());
            } catch (Layer1Exception e) {
                // no logging
            }
        }
    }

    /**
     * Main routing for the DPSS mechanism.
     */
    public void executePSS(final float time) {
        // Aufruf der PSS-Berechnung im Regional Manager
        if (this.isRegionActive && time > nextTimeForRegionRun && nextTimeForRegionRun > 0) {
            RegionalManager.getInstance().calculatePSS();
            this.nextTimeForRegionRun = time + recalculatePSSInterval;
        }

        if (decentralPSSactive && !activeRunForPSS && time > nextTimeForPSSRun && nextTimeForPSSRun > 0) {
            // Warteintervall abgelaufen: Start des PSS Mechanismus
            this.activeRunForPSS = true;
            this.nextPhaseForPSS = 0;
        }

        try {
            // Loop über alle gesteuerten Knoten
            for (AimsunJunction junction : OTCManager.getInstance().getNetwork().getControlledJunctions()) {
                if (this.isRegionActive) {
                    final OTCNodeRegion node = (OTCNodeRegion) junction.getNode();

                    node.activateRuleGeneration(time);

                    // Node bei Regional Manager anmelden
                    if (time == 200) {
                        node.registerAtRegionalManager();
                    } else if (time > nextTimeForRegionInfo) {
                        if (DefaultParams.PSS_LOG) {
                            final String description = node.getPSSDescription();
                            log.info(time + ": " + description);
                        }
                    }

                    node.replaceTempTLC();
                } else if (decentralPSSactive) {
                    final OTCNodeSynchronized node = (OTCNodeSynchronized) junction.getNode();

                    node.activateRuleGeneration(time);
                    recalculateDPSS(time, node);
                    node.replaceTempTLC();
                }

                // Bestimme Steuerung für Knoten, die nicht an grüner Welle teilnehmen
                determineTLCforNonDPSSJunction(junction);
                junction.getActiveTLC().step(time);
            }

            if (decentralPSSactive) {
                performDPSSUpdate(time);
                updateDPSSControlVariables(time);
            } else if (this.isRegionActive) {
                if (time > nextTimeForRegionInfo) {
                    this.nextTimeForRegionInfo = nextTimeForRegionRun + 50;
                }
            }
        } catch (TLCException e) {
            throw new OTCManagerException("TLC: " + e.getMessage());
        }
    }

    /**
     * Return a list with all established DPSS with their participating nodes.
     *
     * @return list of all DPSS
     */
    public List<List<Integer>> getEstablishedDPSSs() {
        final AimsunNetwork network = OTCManager.getInstance().getNetwork();

        if (network == null) {
            return Collections.emptyList();
        }

        List<List<Integer>> establishedDPSSs = new ArrayList<>();

        for (AimsunJunction junction : network.getControlledJunctions()) {
            List<Integer> nodeIDs = new ArrayList<>();

            OTCNodeSynchronized node = (OTCNodeSynchronized) junction.getNode();

            if (node.isBeginOfPSS()) {
                nodeIDs.add(node.getId());
                do {
                    node = node.getPrimarySuccessor();
                    nodeIDs.add(node.getId());
                } while (!node.isEndOfPSS());

                establishedDPSSs.add(nodeIDs);
            }
        }

        if (establishedDPSSs.isEmpty()) {
            return Collections.emptyList();
        }

        return establishedDPSSs;
    }

    public float getNextTimeForPSSCheck() {
        return nextTimeForPSSCheck;
    }

    public float getNextTimeForPSSRun() {
        return nextTimeForPSSRun;
    }

    /**
     * setup initial parameter settings
     */
    public void initParameters() {
        // Anpassungen f�r DPSS
        this.nextPhaseForPSS = 0;
        this.updateDPSS = false;

        this.decentralPSSactive = DefaultParams.PSS_DECENTRAL_ACTIVE;
        this.isRegionActive = DefaultParams.PSS_REGION_ACTIVE;

        this.checkPSSInterval = DefaultParams.PSS_CHECK_INTERVAL;
        this.recalculatePSSInterval = DefaultParams.RECALCULATE_PSS_INTERVAL;

        // Zeitpunkt f�r PSS-Aufbau und Aktualisierung festlegen
        this.activeRunForPSS = false;
        final int warmUpDuration = DefaultParams.L1_WARMUP_TIME;

        // DPSS
        this.nextTimeForPSSRun = warmUpDuration + 100;
        this.nextTimeForPSSCheck = warmUpDuration + 100 + this.checkPSSInterval;

        // Regional Manager
        this.nextTimeForRegionRun = warmUpDuration + this.nextTimeForRegionRun;
        this.nextTimeForRegionInfo = warmUpDuration + this.nextTimeForRegionInfo;
    }

    public boolean isDecentralPSSactive() {
        return decentralPSSactive;
    }

    public boolean isRegionActive() {
        return isRegionActive;
    }

    /**
     * �berpr�ft �nderungsw�nsche von DPSS-Knoten und veranlasst ggf. deren
     * Umsetzung.
     *
     * @param time die aktuelle Simulationszeit
     */
    private void performDPSSUpdate(final float time) {
        // Auswerten der Ergebnisse der �berpr�fung und Durchf�hren der
        // notwendigen �nderungen
        if (!activeRunForPSS && time > nextTimeForPSSCheck && nextTimeForPSSCheck > 0) {
            // �nderungsw�nsche einsammeln
            for (AimsunJunction junction : OTCManager.getInstance().getNetwork().getControlledJunctions()) {
                final OTCNodeSynchronized node = (OTCNodeSynchronized) junction.getNode();

                if (node.isBeginOfPSS() && node.isRunSynchPhase()) {
                    // �nderungwunsch zwischenspeichern!
                    this.updateDPSS = true;

                    // Art der �nderung zwischenspeichern
                    if (updateDPSSFromPhase == -1) {
                        this.updateDPSSFromPhase = node.getNextSynchPhase();
                    } else {
                        this.updateDPSSFromPhase = Math.min(updateDPSSFromPhase, node.getNextSynchPhase());
                    }
                }
            }

            // �nderungsw�nsche auswerten
            if (updateDPSS && updateDPSSFromPhase == 0) {
                // Neue Partnerschaften!
                this.activeRunForPSS = true;
                this.nextPhaseForPSS = updateDPSSFromPhase;
                this.nextTimeForPSSRun = time + 0.25f;
                // H�her als aktuelle Zeit, damit von eventuell
                // folgenden Knoten der Schleife nicht
                // bereits in diesem Durchlauf vollzogen.
                this.updateDPSSFromPhase = -1;
                this.updateDPSS = false;
            } else if (updateDPSS && updateDPSSFromPhase == 6) {
                // Nur neue ACT f�r betroffene PSS!
                for (AimsunJunction junction : OTCManager.getInstance().getNetwork().getControlledJunctions()) {
                    final OTCNodeSynchronized node = (OTCNodeSynchronized) junction.getNode();

                    if (node.isBeginOfPSS() && node.isRunSynchPhase()) {
                        node.runSynchronisationMechanism(6);
                    }
                }
            }
        }
    }

    /**
     * F�hrt die f�r die dezentrale Neuberechnung oder �berpr�fung einer gr�nen
     * Welle notwendigen Schritte an einem Knoten durch.
     *
     * @param time die aktuelle Simulationszeit
     * @param node ein OTC-gesteuerter Knoten
     */
    private void recalculateDPSS(final float time, final OTCNodeSynchronized node) {
        // Vollständige Neuberechnung der grünen Welle, da "nextTimeForPSSRun" erreicht
        if (this.activeRunForPSS && time > this.nextTimeForPSSRun && this.nextTimeForPSSRun > 0) {
            // Nächster Schritt des PSS Mechanismus steht an
            if (this.nextPhaseForPSS < 7 && this.nextPhaseForPSS >= 0) {
                // Führe aktuellen Schritt des PSS-Mechanismus
                // aus
                node.runSynchronisationMechanism(this.nextPhaseForPSS);
            } else if (this.nextPhaseForPSS == 7) {
                // Letzter Schritt: Ausgabe der PSS-Beschreibung
                if (DefaultParams.PSS_LOG) {
                    log.info(node.getPSSDescription());
                }

                // Speichern des n�chsten Zeitpunkts f�r den Knoten
                node.setTimeToNextSynchPhase(time + this.recalculatePSSInterval);
            }
        }
        // Check der bestehenden grünen Wellen, da "nextTimeForPSSCheck" erreicht
        else if (!this.activeRunForPSS && time > this.nextTimeForPSSCheck && this.nextTimeForPSSCheck > 0) {
            // Check durch Wellenalgorithmus, beginnend beim
            // 1. Knoten einer gr�nen Welle. �nderungen werden
            // nicht durchgef�hrt, sondern lediglich beim ersten
            // Knoten der Welle vermerkt (runSynchPhase, nextPhaseForPSS)
            if (node.isBeginOfPSS()) {
                node.checkChangeDemand();
            }
        }
    }

    /**
     * Aktualisiert die zur DPSS-Berechnung notwendigen Steuerungsattribute.
     *
     * @param time die aktuelle Simulationszeit
     */
    private void updateDPSSControlVariables(final float time) {
        // Update der Werte
        if (this.activeRunForPSS && time > this.nextTimeForPSSRun && this.nextTimeForPSSRun > 0) {
            // Eine Phase des PSS Mechanismus ist durchgelaufen:
            // N�chste Phasenschritt oder n�chsten Aufruf setzen
            if (this.nextPhaseForPSS < 7) {
                // Nächste Phase setzen
                this.nextPhaseForPSS++;
                // Nächsten Zeitpunkt setzen
                this.nextTimeForPSSRun = time + 5;
            } else {
                // Durchlauf fertig
                this.activeRunForPSS = false;
                this.nextPhaseForPSS = 0;
                // setzen des n�chsten Aufrufe der PSS Berechnung und Überpr�fung
                this.nextTimeForPSSRun = time + this.recalculatePSSInterval;
                this.nextTimeForPSSCheck = time + this.checkPSSInterval;
            }
        } else if (!this.activeRunForPSS && time > this.nextTimeForPSSCheck && this.nextTimeForPSSCheck > 0) {
            // Es ist gerade eine �berpr�fung gelaufen
            // Set next interval
            this.nextTimeForPSSCheck = time + this.checkPSSInterval;
        }
    }
}
