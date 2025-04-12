package tests.testNetworks;

/**
 * /    \<==100==|~~~~~~~|<======104=======|~~~~~~~|<==106==/    \
 * | 300 |       |  200  |                 |  201  |       | 301  |
 * \____/==101==>|~~~~~~~|==102==>|==103==>|~~~~~~~|==105==>\____/
 *
 * @author Kamuran
 */
public class Create2Junctions extends AbstractTestNetwork {
    protected void initInSectionsToDestinations() {
        // Node 200
        sectionToDestinations.put(100, new Integer[]{});
        sectionToDestinations.put(101, new Integer[]{102});
        sectionToDestinations.put(102, new Integer[]{103});
        sectionToDestinations.put(104, new Integer[]{100});

        // Node  201
        sectionToDestinations.put(105, new Integer[]{});
        sectionToDestinations.put(103, new Integer[]{105});
        sectionToDestinations.put(106, new Integer[]{104});
    }

    public void create() {
        manager.createNetwork("Create2Junctions");

        initParameters();
        initInSectionsToDestinations();

        createJunctions();
        createSections(1, new int[]{});
        createCentroids();
        createDetectors();
        createSignalGroups();
        createTurnings(3);

        initSimulation();
    }

    protected void initParameters() {
        this.numberOfJunctions = 2;
        this.junctionId = 200;
        this.numberOfSections = 7;
        this.numberOfDetectors = 7;
        this.numberOfPhases = 2;
        this.numberOfCentroids = 2;
    }

    protected void createCentroids() {
        manager.addCentroid(300, new int[]{100}, new int[]{101});
        manager.addCentroid(301, new int[]{105}, new int[]{106});
    }

    @Override
    protected void createTurnings(int numberOfInSectionsToJunction) {
        signalGroupId++;
        manager.addTurning(junctionId, signalGroupId, 101, 102);
        manager.addTurning(junctionId, signalGroupId, 102, 103);
        manager.addTurning(junctionId, signalGroupId, 104, 100);

        junctionId++;
        manager.addTurning(junctionId, signalGroupId, 103, 105);
        manager.addTurning(junctionId, signalGroupId, 106, 104);
    }
}
