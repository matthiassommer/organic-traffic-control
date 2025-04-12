package tests.testNetworks;

/**
 * @author rochner
 */
public class SimpleJunction extends AbstractTestNetwork {
    public void create(final int numberArms) {
        manager.createNetwork("Testnetwork.ang");

        int[] destinations = new int[numberArms - 1];
        int[] destinationsIn = new int[1];

        for (int i = 0; i < numberArms; i++) {
            for (int j = 0; j < numberArms - 1; j++) {
                destinations[j] = j >= i ? j + 12 : j + 11;
            }

            // InSections
            manager.addSection(i + 1, roadType, numberArms - 1, length, destinations, speedLimit, capacity);
            // OutSections
            manager.addSection(i + 11, roadType, 0, length, new int[0], speedLimit, capacity);

            destinationsIn[0] = i + 1;
            manager.addSection(i + 21, roadType, 1, length, destinationsIn, speedLimit, capacity);

            manager.addDetector(i + 1, i + 1, 0.2f, 0.4f, 0, 1, "Detector" + (i + 1));
            manager.setDetectorCapabilities(i + 1, true, true, false, false, true, false, false);
            manager.setDetectorDestinations(i + 1, destinations);

            manager.addDetector(i + 11, i + 21, 0.2f, 0.4f, 0, 1, "Detector entry" + (i + 11));
            manager.setDetectorCapabilities(i + 11, true, true, false, false, true, false, false);
            manager.setDetectorDestinations(i + 1, destinationsIn);
        }

        manager.addJunction(1, 2, "Junction");

        // Einfache Controller: Anzahl Phasen = Anzahl Arme (Phasen 1 bis
        // numberArms). Komplexere Controller: Anzahl Phasen = Anzahl
        // Turnings (Phasen 11 bis numberArms * 10 + numberArms)
        for (int i = 0; i < numberArms; i++) {
            manager.addPhase(i + 1, 0, 1);

            for (int j = 0; j < numberArms - 1; j++) {
                manager.addPhase(j + 1 + (i + 1) * 10, 0, 1);
                manager.addSignalGrp(j + 1 + i * (numberArms - 1), 1);
                manager.addTurning(1, j + 1 + i * (numberArms - 1), i + 1, j >= i ? j + 12 : j + 11);
                manager.addSignalGrpPhase(j + 1 + i * (numberArms - 1), i + 1, 1);
                manager.addSignalGrpPhase(j + 1 + i * (numberArms - 1), j + 1 + (i + 1) * 10, 1);
            }
        }

        manager.initSubDetectors();
        manager.initJunctions();
        manager.setReplicationID(1);
        manager.setTime(0);
        manager.finalizeInit();
    }

    @Override
    public void create() {
    }

    @Override
    protected void createCentroids() {
    }

    @Override
    public void generateFlowForTurnings(float time) {
    }

    @Override
    protected void initParameters() {
    }

    @Override
    protected void initInSectionsToDestinations() {
    }
}
