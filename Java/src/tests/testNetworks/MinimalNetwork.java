package tests.testNetworks;

/**
 * Network style:
 * C1 --> 100 --> Junction --> 101 --> C2
 *
 * @author Matthias Sommer.
 */
public class MinimalNetwork extends AbstractTestNetwork {
    protected void initInSectionsToDestinations() {
        sectionToDestinations.put(100, new Integer[]{101});
        sectionToDestinations.put(101, new Integer[]{});
    }

    public void create() {
        manager.createNetwork("");

        initParameters();
        initInSectionsToDestinations();

        createJunctions();
        createSections(1, new int[]{});
        createCentroids();
        createSignalGroups();
        createTurnings(numberOfSections);

        initSimulation();
    }

    protected void initParameters() {
        this.numberOfJunctions = 1;
        this.junctionId = 200;
        this.numberOfSections = 2;
        this.numberOfDetectors = 0;
        this.numberOfPhases = 2;
        this.numberOfCentroids = 2;
    }

    protected void createCentroids() {
        manager.addCentroid(300, new int[]{101}, new int[]{});
        manager.addCentroid(302, new int[]{}, new int[]{100});
    }
}
