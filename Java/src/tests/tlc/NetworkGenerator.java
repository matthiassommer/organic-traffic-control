package tests.tlc;

import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;

/**
 * Erzeugt ein Verkehrsnetz mit einem Knoten, der eine parametrisierbare Anzahl
 * Arme hat.
 *
 * @author rochner
 */
public abstract class NetworkGenerator {
    private static final int roadType = 49;

    /**
     * Erzeugt ein Verkehrsnetz mit einem Knoten, der numberArms Arme hat, an
     * jedem Arm eine InSection und eine OutSection, auf jeder InSection einen
     * Detector mit Count, Presence und Headway-Fähigkeit, vor jeder InSection
     * eine weitere Section mit Count Detector für Zählschleife. Es gibt numArms
     * Phasen, die jeweils alle Verkehrsströme einer InSection zusammenfassen.
     *
     * @param numberArms Anzahl der Arme des Knotens im Netz.
     * @return Das Netzwerk-Objekt.
     */
    public static AimsunNetwork generateTestNetwork(final int numberArms) {
        AimsunNetwork network = new AimsunNetwork("Testnetz");
        int[] destinations = new int[numberArms - 1];
        int[] destinationsIn = new int[1];
        int[] destinationsNull = new int[0];
        float speedlimit = 50;
        float capacity = 1000;

        for (int i = 0; i < numberArms; i++) {
            for (int j = 0; j < numberArms - 1; j++) {
                destinations[j] = j >= i ? j + 12 : j + 11;
            }
            // InSections
            network.addSection(i + 1, roadType, numberArms - 1, 10.0f, destinations, speedlimit, capacity);
            // OutSections
            network.addSection(i + 11, roadType, 0, 10.0f, destinationsNull, speedlimit, capacity);
            destinationsIn[0] = i + 1;
            network.addSection(i + 21, roadType, 1, 10.0f, destinationsIn, speedlimit, capacity);
            network.addDetector(i + 1, i + 1, 0.2f, 0.4f, 0, 1);
            network.setDetectorCapabilities(i + 1, true, true, false, false, true, false, false);
            network.setDetectorDestinations(i + 1, destinations);
            network.addDetector(i + 11, i + 21, 0.2f, 0.4f, 0, 1);
            network.setDetectorCapabilities(i + 11, true, true, false, false, true, false, false);
            network.setDetectorDestinations(i + 1, destinationsIn);
        }
        network.initSubDetectors();
        network.addJunction(1, 2, "");
        for (int i = 0; i < numberArms; i++) {
            network.addPhase(i + 1, false, 1);
            network.addSignalGroup(i + 1, 1);

            for (int j = 0; j < numberArms - 1; j++) {
                network.addTurning(1, i + 1, i + 1, j >= i ? j + 12 : j + 11);
            }

            network.addSignalGrpPhase(i + 1, i + 1, 1);
        }
        network.initJunctions();
        return network;
    }
}
