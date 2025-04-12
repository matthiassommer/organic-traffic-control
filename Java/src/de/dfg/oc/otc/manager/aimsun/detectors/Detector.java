package de.dfg.oc.otc.manager.aimsun.detectors;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.layer1.observer.monitoring.SubDetectorValue;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.util.Observable;

/**
 * Detector contains several SubDetectors, which have certain abilities to
 * monitor specific traffic attributes.
 */
public class Detector extends Observable {
    /**
     * First and last lane the Detector is able to monitor.
     */
    private final int firstLaneToCover;
    private final int lastLaneToCover;
    /**
     * Detector Identifier.
     */
    private final int id;
    /**
     * Position in section where Detector starts.
     */
    private final float positionBegin;
    /**
     * Position in section where Detector ends.
     */
    private final float positionEnd;
    /**
     * ID of section where detector is lying.
     */
    private final int sectionID;
    /**
     * list of associated SubDetectors.
     */
    private final SubDetector[] subDetectors;
    /**
     * IDs of sections, die (im Anschluss an die nächstfolgende Junction) Ziel
     * der vom Detektor erfassten Verkehrsströme sind.
     */
    private int[] destinations;
    private final DetectorCapabilities detectorCapabilities = new DetectorCapabilities();

    /**
     * Erzeugt ein Detector-Objekt.
     *
     * @param id        Detector Identifier.
     * @param sectionId Id der Section, in der der Detector liegt.
     * @param posBegin  Position innerhalb der Section, an der der Detector beginnt.
     * @param firstLane Die erste Fahrspur der Section, die von diesem Detektor
     *                  erfasst wird.
     * @param lastLane  Die letzte Fahrspur der Section, die von diesem Detektor
     *                  erfasst wird.
     * @param posEnd    Position innerhalb der Section, an der der Detector endet.
     */
    public Detector(final int id, final int sectionId, final float posBegin, final float posEnd, final int firstLane,
                    final int lastLane) {
        this.id = id;
        this.sectionID = sectionId;
        this.positionBegin = posBegin;
        this.positionEnd = posEnd;
        this.firstLaneToCover = firstLane;
        this.lastLaneToCover = lastLane;
        this.subDetectors = new SubDetector[DetectorCapabilities.NUM];

        // creates a SubDetector for every detector capability
        for (int i = 0; i < DetectorCapabilities.NUM; i++) {
            this.subDetectors[i] = new SubDetector();
            this.subDetectors[i].setDetectorIdentifier(id);
            this.subDetectors[i].setDetector(this);
            this.subDetectors[i].setFeature(i);
        }
    }

    public final int[] getDestinations() {
        return this.destinations;
    }

    public final void setDestinations(final int[] destinationIds) {
        this.destinations = destinationIds;
    }

    private String getDetectorDescription(String output, final boolean includeValue) {
        output = output.concat("<tr><td>Target sections: </td><td><b>");
        if (destinations != null) {
            for (int destination : destinations) {
                output = output.concat(destination + " ");
            }
        }
        output = output.concat("</b></td></tr>");

        output = output.concat("<tr><td>Capabilities: </td><td><b>");
        boolean hasSubdetectors = false;

        for (int i = 0; i < DetectorCapabilities.NUM; i++) {
            if (subDetectors[i].isEnabled()) {
                hasSubdetectors = true;
            }
            output = output.concat(subDetectors[i].isEnabled() ? detectorCapabilities.getDescription(i)
                    + " " : "");
        }
        output = output.concat("</b></td></tr></table>");

        if (hasSubdetectors) {
            output = output.concat("<h3>SubDetectors</h3><table><tr><th>Id</th><th>Feature</th>"
                    + (includeValue ? "<th>Current Value</th></tr>" : "</tr>"));
            for (int i = 0; i < DetectorCapabilities.NUM; i++) {
                if (subDetectors[i].isEnabled()) {
                    output = output.concat(subDetectors[i].getSimpleDescription(includeValue));
                }
            }
        }
        output = output.concat("</table>");

        return output;
    }

    /**
     * Gibt die erste Spur zur�ck, die von diesem Detektor �berdeckt wird.
     */
    public final int getFirstLane() {
        return this.firstLaneToCover;
    }

    public final int getId() {
        return this.id;
    }

    /**
     * Gibt die letzte Spur zur�ck, die von diesem Detektor �berdeckt wird.
     */
    public final int getLastlane() {
        return this.lastLaneToCover;
    }

    public final float getPositionBegin() {
        return this.positionBegin;
    }

    public final float getPositionEnd() {
        return this.positionEnd;
    }

    /**
     * Gibt die Id der Section zur�ck, auf der der Detektor liegt.
     */
    public final int getSectionId() {
        return this.sectionID;
    }

    public final String getSimpleDescription(final boolean includeValue) {
        final DecimalFormat formatter = new DecimalFormat("#.##");

        final String description = "<h2>Detector " + id + "</h2><table><tr><td>Position (begin): </td><td><b>"
                + formatter.format(positionBegin) + " m</b></td></tr><tr><td>Position (end): </td><td><b>"
                + formatter.format(positionEnd) + " m</b></td></tr>";

        return getDetectorDescription(description, includeValue);
    }

    public final SubDetector getSubDetector(final int feature) {
        return this.subDetectors[feature];
    }

    public final SubDetector[] getSubDetectors() {
        return this.subDetectors;
    }

    /**
     * @param index Welcher Subdetektor soll ausgelesen werden: 1: Count, 2:
     *              Presence, 3: Speed, 4: Occupied Time Percentage, 5: Headway,
     *              6: Density, 7: EquippedVehicle
     * @return Wert des jeweiligen Teildetektors oder -1, falls der Detektor
     * diese F�higkeit nicht besitzt.
     */
    public final SubDetectorValue getValue(final int index) throws OTCManagerException {
        if (index >= DetectorCapabilities.NUM) {
            throw new OTCManagerException("Can't receive value for unknown Subdetector.");
        }

        if (this.subDetectors[index].isEnabled()) {
            return this.subDetectors[index].getDetectorValue();
        }

        throw new OTCManagerException("Can't receive value.");
    }

    public final void reset() {
        for (int i = 0; i < DetectorCapabilities.NUM; i++) {
            this.subDetectors[i].reset();
        }
    }

    /**
     * Legt fest, welche Eigenschaften ein Detektor hat.
     *
     * @param count           Counter
     * @param presence        Presence Detector
     * @param speed           Speed Detector
     * @param occupancy       Occupancy Detector
     * @param headway         Headway Detector
     * @param density         Density Detector
     * @param equippedVehicle Kann "Equipped Vehicles" erkennen
     */
    public final void setCapabilities(final boolean count, final boolean presence, final boolean speed,
                                      final boolean occupancy, final boolean headway, final boolean density, final boolean equippedVehicle) {
        this.subDetectors[0].setEnabled(count);
        this.subDetectors[1].setEnabled(presence);
        this.subDetectors[2].setEnabled(speed);
        this.subDetectors[3].setEnabled(occupancy);
        this.subDetectors[4].setEnabled(headway);
        this.subDetectors[5].setEnabled(density);
        this.subDetectors[6].setEnabled(equippedVehicle);
    }

    private void setSubDetectorValue(final SubDetector subDetector, final float time, final float value) {
        if (value < 0) {
            // Wenn ein Detektorwert nicht erfasst werden konnte, wird
            // als Wert -3012 zur�ckgegeben. In diesen F�llen wird der
            // vorangegangene Wert des Detectors f�r den aktuellen
            // Zeitschritt �bernommen.
            if (subDetector.getValue() < 0) {
                return;
            }

            subDetector.setValue(new SubDetectorValue(time, subDetector.getValue()));
        } else {
            subDetector.setValue(new SubDetectorValue(time, value));
        }
    }

    /**
     * Aktualisiert die Detektoren.
     *
     * @param values Array mit den Werten: 0)Anzahl Fahrzeuge im letzten
     *               Zeitschritt 1)presence: war der Detektor im letzten
     *               Zeitschritt belegt (1.0) oder nicht (0.0) 2) speed: Mittlere
     *               Geschwindigkeit im letzten Zeitschritt 3) occupancy:
     *               Belegungsgrad im letzten Zeitschritt 4) headway:
     *               Durchschnittliche L�ckengr��e 5) density: Verkehrsdichte 6)
     *               equippedVehicle: Anzahl "Equipped Vehicle"
     */
    public final void setValues(final float time, final float[] values) throws OTCManagerException {
        if (values.length < DetectorCapabilities.NUM) {
            throw new OTCManagerException("List of values has too few entries.");
        }

        if (time < 0) {
            return;
        }

        for (int i = 0; i < DetectorCapabilities.NUM; i++) {
            final SubDetector subDetector = this.subDetectors[i];
            if (subDetector.isEnabled()) {
                setSubDetectorValue(subDetector, time, values[i]);
            }
        }

        DetectorDataValue value = new DetectorDataValue(time, values);
        setChanged();
        notifyObservers(value.clone());
    }

    public final String toString() {
        final DecimalFormat formatter = new DecimalFormat("#.##");
        final String linesep = System.getProperty("line.separator");

        String output = "Detector " + id + " on section " + sectionID + ", PositionBegin "
                + formatter.format(positionBegin) + ", PositionEnd " + formatter.format(positionEnd) + linesep;

        output = output.concat("Target sections: ");
        if (destinations != null) {
            for (int destination : destinations) {
                output = output.concat(destination + " ");
            }
        }
        output += linesep;

        output += "Capabilities: ";
        for (int i = 0; i < DetectorCapabilities.NUM; i++) {
            output = output.concat(subDetectors[i].isEnabled() + ", ");
        }

        output += linesep;
        for (int i = 0; i < DetectorCapabilities.NUM; i++) {
            if (subDetectors[i].isEnabled()) {
                output = output.concat(subDetectors[i].toString());
            }
        }

        return output;
    }
}
