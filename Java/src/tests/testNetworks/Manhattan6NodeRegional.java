package tests.testNetworks;

import java.util.Map;

/**
 * Creates a test network of six nodes and ten centroids (similar to manhattan6NodeRegional.ang).
 *
 * @author Kamuran
 */
public class Manhattan6NodeRegional extends AbstractTestNetwork {
    private final int[] manhattan6NRJunctions = new int[]{143, 158, 179, 357, 384, 403};

    protected void initInSectionsToDestinations() {
        // Node: 143 upper left
        // incomming centroidSections
        sectionToDestinations.put(130, new Integer[]{});
        sectionToDestinations.put(132, new Integer[]{});
        // incomming junctionSections
        sectionToDestinations.put(131, new Integer[]{130, 133, 135});
        sectionToDestinations.put(129, new Integer[]{132, 135, 133});
        sectionToDestinations.put(134, new Integer[]{135, 132, 130});
        sectionToDestinations.put(136, new Integer[]{132, 130, 133});

        // Node: 158 upper middle
        sectionToDestinations.put(139, new Integer[]{});
        sectionToDestinations.put(140, new Integer[]{136, 137, 141});
        sectionToDestinations.put(135, new Integer[]{137, 141, 139});
        sectionToDestinations.put(138, new Integer[]{141, 139, 136});
        sectionToDestinations.put(142, new Integer[]{139, 136, 137});

        // Node: 179 upper right
        sectionToDestinations.put(175, new Integer[]{});
        sectionToDestinations.put(173, new Integer[]{});
        sectionToDestinations.put(176, new Integer[]{142, 177, 173});
        sectionToDestinations.put(141, new Integer[]{177, 173, 175});
        sectionToDestinations.put(178, new Integer[]{173, 175, 142});
        sectionToDestinations.put(174, new Integer[]{175, 142, 177});

        // Node: 357 lower left
        sectionToDestinations.put(342, new Integer[]{});
        sectionToDestinations.put(355, new Integer[]{});
        sectionToDestinations.put(133, new Integer[]{342, 355, 352});
        sectionToDestinations.put(343, new Integer[]{355, 352, 134});
        sectionToDestinations.put(356, new Integer[]{352, 134, 342});
        sectionToDestinations.put(353, new Integer[]{134, 342, 355});

        // Node: 384 lower middle
        sectionToDestinations.put(379, new Integer[]{});
        sectionToDestinations.put(137, new Integer[]{353, 379, 382});
        sectionToDestinations.put(352, new Integer[]{379, 382, 138});
        sectionToDestinations.put(380, new Integer[]{382, 138, 353});
        sectionToDestinations.put(383, new Integer[]{138, 353, 379});

        // Node: 403 lower right
        sectionToDestinations.put(400, new Integer[]{});
        sectionToDestinations.put(398, new Integer[]{});
        sectionToDestinations.put(177, new Integer[]{383, 400, 398});
        sectionToDestinations.put(382, new Integer[]{400, 398, 178});
        sectionToDestinations.put(401, new Integer[]{398, 178, 383});
        sectionToDestinations.put(399, new Integer[]{178, 383, 400});
    }

    public void create() {
        manager.createNetwork("manhattan6NodeRegional");

        initParameters();
        initInSectionsToDestinations();

        createJunctions();
        createSections(3, new int[]{141, 142, 382, 383});
        createCentroids();
        createDetectors();
        createSignalGroups();
        createTurnings(4);

        initSimulation();
    }

    protected void initParameters() {
        this.numberOfJunctions = 6;
        this.numberOfSections = 34;
        this.numberOfDetectors = 34;
        this.numberOfPhases = 2;
        this.numberOfCentroids = 10;
    }

    protected void createCentroids() {
        // Node: 143 upper left
        manager.addCentroid(236, new int[]{130}, new int[]{129});
        manager.addCentroid(233, new int[]{132}, new int[]{131});

        // Node: 158 upper middle
        manager.addCentroid(230, new int[]{139}, new int[]{140});

        // Node: 179 upper right
        manager.addCentroid(227, new int[]{175}, new int[]{176});
        manager.addCentroid(239, new int[]{173}, new int[]{174});

        // Node: 357 lower left
        manager.addCentroid(425, new int[]{342}, new int[]{343});
        manager.addCentroid(426, new int[]{355}, new int[]{356});

        // Node: 384 lower middle
        manager.addCentroid(427, new int[]{379}, new int[]{380});

        // Node: 403 lower right
        manager.addCentroid(428, new int[]{400}, new int[]{401});
        manager.addCentroid(424, new int[]{398}, new int[]{399});
    }

    @Override
    protected void createJunctions() {
        for (int j = 0; j < manhattan6NRJunctions.length; j++) {
            manager.addJunction(manhattan6NRJunctions[j], 2, "Junction " + manhattan6NRJunctions[j]);
        }
    }

    @Override
    protected void createSignalGroups() {
        int tempSignalGroupID = signalGroupId + 1;
        for (int j = 0; j < manhattan6NRJunctions.length; j++) {
            manager.addSignalGrp(signalGroupId, manhattan6NRJunctions[j]);

            for (int i = 1; i < numberOfPhases + 1; i++) {
                manager.addPhase(i, 0, 10, 20, 5, manhattan6NRJunctions[j]);
                manager.addSignalGrpPhase(tempSignalGroupID, i, manhattan6NRJunctions[j]);
            }
        }
    }

    /**
     * @param numberOfInSections inSections to the current considered junction
     */
    @Override
    protected void createTurnings(int numberOfInSections) {
        signalGroupId++;
        int indexOfJunctionIds = 0;
        int counterOfInSection = 0;
        for (Map.Entry<Integer, Integer[]> entry : sectionToDestinations.entrySet()) {
            Integer[] temp = entry.getValue();

            if (temp.length > 0) {
                for (int i = 0; i < temp.length; i++) {
                    manager.addTurning(manhattan6NRJunctions[indexOfJunctionIds], signalGroupId, entry.getKey(), temp[i]);
                }
                counterOfInSection++;
                if (counterOfInSection == numberOfInSections) {
                    indexOfJunctionIds++;
                    counterOfInSection = 0;
                }
            }
        }
    }
}