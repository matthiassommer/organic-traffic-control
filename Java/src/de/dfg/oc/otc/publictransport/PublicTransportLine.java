package de.dfg.oc.otc.publictransport;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Phase;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.Turning;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class represents a Public Transport Line in the Simulation
 * <p>
 * Created by Dominik on 31.07.2014.
 */
public class PublicTransportLine {
    /**
     * The ID of the Public Transport Line
     */
    private final int id;

    /**
     * List of all PublicTransportControllers of this PublicTransportLine.
     */
    private final List<PublicTransportController> publicTransportControllers;

    public PublicTransportLine(int id, List<Section> sections) {
        this.id = id;
        this.publicTransportControllers = new ArrayList<>();

        initPublicTransportControllers(sections, id);
    }

    /**
     * Generate a List of {@Link PublicTransportDetector}s from the given public transport line.
     *
     * @param sections the sections of the public transport line
     * @param lineID   the ID of the public transport lines
     * @return the total number of {@Link PublicTransportDetector}s from all public transport lines
     */
    private void initPublicTransportControllers(List<Section> sections, int lineID) {
        for (int i = 0; i < sections.size() - 1; i++) {
            Detector detector = findDetector(sections.get(i).getId());

            if (detector != null) {
                try {
                    Section section = sections.get(i);
                    AimsunJunction junction = section.getNextJunction();

                    if (junction != null) {
                        Turning turning = junction.getTurning(section.getId(), sections.get(i + 1).getId());

                        if (turning != null) {
                            List<Phase> phases = junction.getPhasesForTurnings(turning.getId());
                            PublicTransportController publicTransportController = new PublicTransportController(detector, junction, phases, section, lineID);

                            this.publicTransportControllers.add(publicTransportController);
                        }
                    }
                } catch (OTCManagerException e) {
                    throw new OTCManagerException("initPublicTransportController failed" + e.getMessage());
                }
            }
        }
    }

    /**
     * This method finds the most fitting detector for the given section.
     *
     * @param sectionID the ID of the section
     * @return the found {@Link Detector}
     */
    private Detector findDetector(int sectionID) {
        // List of all Detectors of the simulation
        Map<Integer, Detector> detectorMap = OTCManager.getInstance().getNetwork().getDetectors();
        Detector detector = null;

        for (Map.Entry<Integer, Detector> entry : detectorMap.entrySet()) {
            Detector tempDetector = entry.getValue();

            // Check if detector has EquippedVehicle capability
            if (tempDetector.getSectionId() == sectionID && isEquipped(tempDetector)) {
                if (detector != null) {
                    // Get the detector most distant to the end of the section
                    if (detector.getPositionBegin() > tempDetector.getPositionBegin()) {
                        detector = tempDetector;
                    }
                } else {
                    detector = tempDetector;
                }
            }
        }
        return detector;
    }

    /**
     * This method will return the {@Link PublicTransportDetector} for the given parameters.
     *
     * @param id the ID of the {@Link Detector}
     * @return the found {@Link PublicTransportDetector}
     */
    public PublicTransportController getPublicTransportController(int id) throws OTCManagerException {
        for (PublicTransportController detector : this.publicTransportControllers) {
            if (detector.getDetector().getId() == id && detector.getLineID() == this.id) {
                return detector;
            }
        }
        throw new OTCManagerException("No PT-Controller found.");
    }

    /**
     * This method triggers the checkup for a TLC reset in all {@Link PublicTransportDetector}s.
     */
    public void manageTLCReset() {
        this.publicTransportControllers.forEach(de.dfg.oc.otc.publictransport.PublicTransportController::resetTLC);
    }

    /**
     * This method checks if the {@Link Detector} has the Equipped Vehicle Capability.
     *
     * @param detector the {@Link Detector}
     * @return true if Detector has Equipped Vehicle capability
     */
    private boolean isEquipped(Detector detector) {
        return detector.getSubDetector(DetectorCapabilities.EQUIPPEDVEHICLE).isEnabled();
    }

    public List<PublicTransportController> getPublicTransportControllers() {
        return this.publicTransportControllers;
    }

    public int getID() {
        return this.id;
    }
}