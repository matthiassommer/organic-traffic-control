package tests.testNetworks;

import de.dfg.oc.otc.manager.OTCManager;
import org.apache.commons.math3.random.RandomDataGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Matthias Sommer.
 */
public abstract class AbstractTestNetwork {
    protected final RandomDataGenerator rand = new RandomDataGenerator();
    protected final OTCManager manager;
    protected final int roadType = 49;
    protected final float speedLimit = 50;
    protected final float capacity = 2200;
    protected final float length = 100;
    protected int numberOfSections;
    protected Map<Integer, Integer[]> sectionToDestinations = new LinkedHashMap<>(this.numberOfSections);
    protected int numberOfDetectors;
    protected int numberOfPhases;
    protected int numberOfCentroids;
    protected int junctionId;
    protected int signalGroupId;
    protected int numberOfJunctions;
    protected int maxTurningFlow = 10;
    protected int maxSectionFlow = 10;
    protected int diffDetectorId = 500;
    protected int extendSectionDistance = 250;

    public AbstractTestNetwork() {
        manager = OTCManager.getInstance();
    }

    public void incrementTime() {
        manager.setTimeForTests(manager.getTime() + OTCManager.getSimulationStepSize());
    }

    public abstract void create();

    protected abstract void createCentroids();

    protected abstract void initParameters();

    protected abstract void initInSectionsToDestinations();

    protected void createJunctions() {
        for (int j = junctionId; j < this.numberOfJunctions + junctionId; j++) {
            manager.addJunction(j, 2, "Junction " + j);
        }
    }

    /**
     * @param numberOfTurnings    number of turnings for incoming sections
     * @param regionalSectionsIds sections, which have to be extended in order to build regions
     */
    protected void createSections(int numberOfTurnings, int[] regionalSectionsIds) {
        for (Map.Entry<Integer, Integer[]> entry : sectionToDestinations.entrySet()) {
            if (entry.getValue().length == 0) {
                manager.addSection(entry.getKey(), roadType, 0, length, new int[]{}, speedLimit, capacity);
            } else {
                Integer[] temp = entry.getValue();
                int[] dest = new int[temp.length];
                for (int i = 0; i < dest.length; i++) {
                    dest[i] = temp[i];
                }

                boolean isRegionalSection = false;
                if (regionalSectionsIds.length > 0) {
                    for (int i = 0; i < regionalSectionsIds.length; i++) {
                        if (entry.getKey() == regionalSectionsIds[i]) {
                            // the value +extendSectionDistance is for regional RC relevant, e.g. see RegionalLinkStateRC (determineRegionalRCType)
                            manager.addSection(entry.getKey(), roadType, numberOfTurnings, length + extendSectionDistance, dest, speedLimit, capacity);
                            isRegionalSection = true;
                        }
                    }
                }

                if (!isRegionalSection) {
                    manager.addSection(entry.getKey(), roadType, numberOfTurnings, length, dest, speedLimit, capacity);
                }
            }
        }

        for (Map.Entry<Integer, Integer[]> entry : sectionToDestinations.entrySet()) {
            manager.setSectionNbLanes(entry.getKey(), 2);

            if (numberOfDetectors > 0) {
                manager.addDetector(entry.getKey(), entry.getKey(), 0, 2, 0, 1, "Detector " + entry.getKey());
                manager.setDetectorCapabilities(entry.getKey(), true, true, true, true, true, true, true);

                manager.addDetector(entry.getKey() + diffDetectorId, entry.getKey(), 8, 10, 0, 1, "Detector " + entry.getKey());
                manager.setDetectorCapabilities(entry.getKey() + diffDetectorId, true, true, true, true, true, true, true);
            }
        }
    }

    protected void createDetectors() {
        for (Map.Entry<Integer, Integer[]> entry : sectionToDestinations.entrySet()) {
            if (entry.getValue().length == 0) {
                manager.setDetectorDestinations(entry.getKey() + diffDetectorId, new int[]{});
                manager.setDetectorDestinations(entry.getKey(), new int[]{});
            } else {
                Integer[] temp = entry.getValue();
                int[] dest = new int[temp.length];
                for (int i = 0; i < dest.length; i++) {
                    dest[i] = temp[i];
                }
                manager.setDetectorDestinations(entry.getKey() + diffDetectorId, dest);
                manager.setDetectorDestinations(entry.getKey(), dest);
            }
        }
    }

    protected void createSignalGroups() {
        int tempSignalGroupID = signalGroupId + 1;
        for (int j = junctionId; j < this.numberOfJunctions + junctionId; j++) {
            manager.addSignalGrp(signalGroupId, j);

            for (int i = 1; i < numberOfPhases + 1; i++) {
                manager.addPhase(i, 0, 10, 20, 5, j);
                manager.addSignalGrpPhase(tempSignalGroupID, i, j);
            }
        }
    }

    /**
     * @param numberOfInSections inSections to the current considered junction
     */
    protected void createTurnings(int numberOfInSections) {
        signalGroupId++;
        int counterOfInSection = 0;
        for (Map.Entry<Integer, Integer[]> entry : sectionToDestinations.entrySet()) {
            Integer[] temp = entry.getValue();
            if (temp.length > 0) {
                for (int i = 0; i < temp.length; i++) {
                    manager.addTurning(junctionId, signalGroupId, entry.getKey(), temp[i]);
                }
                counterOfInSection++;
                if (counterOfInSection == numberOfInSections) {
                    junctionId++;
                    counterOfInSection = 0;
                }
            }
        }
    }

    public void generateFlowForSections() {
        for (Map.Entry<Integer, Integer[]> entry : sectionToDestinations.entrySet()) {
            manager.addSectionRawFlow(entry.getKey(), rand.nextInt(0, maxSectionFlow));
        }
    }

    public void generateFlowForTurnings(float time) {
        for (Map.Entry<Integer, Integer[]> entry : sectionToDestinations.entrySet()) {
            if (entry.getValue().length != 0) {
                Integer[] temp = entry.getValue();
                for (int i = 0; i < temp.length; i++) {
                    manager.addTurningRawStatisticalData(entry.getKey(), temp[i], time, rand.nextInt(0, maxTurningFlow), 1, 1, 1, 1, 1, 1, 1, 10);
                }
            }
        }
    }

    public Map<Integer, Integer[]> getSectionToDestinations() {
        return sectionToDestinations;
    }

    protected final void initSimulation() {
        manager.initSubDetectors();
        manager.initJunctions();
        manager.setReplicationID(1);
        manager.setTimeForTests(0);
        manager.finalizeInit();
    }
}
