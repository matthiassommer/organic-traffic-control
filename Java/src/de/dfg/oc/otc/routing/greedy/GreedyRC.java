package de.dfg.oc.otc.routing.greedy;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.routing.ProtocolType;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.RoutingTable;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Class implements a greedy routing protocol for decentralised routing in urban
 * road networks. Orientated on the GPSR routing protocol. Each node in the
 * network determines the routes in the routing tables locally decided with the
 * arc between next node and destination which turning to take.
 * <p/>
 * There is no communication at all.
 * <p/>
 * Each node has to know its own, of all neighbours, as well as the geographical
 * coordinates of all targets. Calculation is based on local delays.
 * <p/>
 * Fast algorithm but whether optimal nor complete.
 *
 * @author lyda, tomforde
 */
public class GreedyRC extends RoutingComponent {
    /**
     * Map containing the identifier of the direction and the centroids laying
     * in the range.
     */
    private Map<Direction, List<Centroid>> centroidsInDirection;
    private boolean isInitialized;
    /**
     * Map containing the identifier of the direction and the OutSections laying
     * in the range.
     */
    private Map<Direction, List<Section>> outSectionsInDirection;

    public GreedyRC(final ProtocolType type, final OTCNode node) {
        super(type, node);
    }

    /**
     * Determines the angle between two points.
     *
     * @param pointA
     * @param pointB
     * @return angle between a and b
     */
    private double determineArc(final Point2D.Double pointA, final Point2D.Double pointB) {
        final double x1 = pointA.getX();
        final double y1 = pointA.getY();
        final double x2 = pointB.getX();
        final double y2 = pointB.getY();
        final double angleX = Math.tanh((y2 - y1) / (x2 - x1));

        if (x1 > x2) {
            // case pointB is more left
            if (y1 > y2) {
                // case pointB is more down
                return angleX + 3 / 2 * Math.PI;
            } else if (y1 < y2) {
                // case point B is more up
                return angleX + Math.PI;
            }
            return Math.PI;
        } else if (x1 < x2) {
            // case pointB is more right
            if (y1 > y2) {
                // case pointB is more down
                return angleX + 2 * Math.PI;
            } else if (y1 < y2) {
                // case point B is more up
                return angleX;
            }
            return 0;
        } else {
            // case x1==x2
            if (y2 - y1 > 0) {
                return Math.PI / 2;
            }
            if (y2 - y1 < 0) {
                return 3 * Math.PI / 2;
            }
            return 0;
        }
    }

    /**
     * Map containing the identifier of the direction and the border values of
     * the range.
     *
     * @return list of directions
     */
    private List<Direction> determineBorderOfRanges() {
        // Number of directions is specified by number of out sections
        final int numberOfDirections = getJunction().getOutSections().size();
        final double rangeSize = 2 * Math.PI / numberOfDirections;
        double rangeLowerBorder = 0;
        double rangeUpperBorder = rangeSize;

        List<Direction> directions = new ArrayList<>();
        for (int i = 1; i <= numberOfDirections; i++) {
            final Direction direction = new Direction(rangeLowerBorder, rangeUpperBorder);

            // Prefare for next iteration
            rangeLowerBorder = rangeUpperBorder;
            rangeUpperBorder += rangeSize;

            directions.add(direction);
        }
        return directions;
    }

    /**
     * Determine the costs for using the turnings from thisInSection.
     *
     * @param inSection
     * @return
     */
    private Map<Section, Float> determineTurningCosts(final Section inSection) {
        final List<Section> outSections = this.getJunction().getOutSections();

        Map<Section, Float> outSectionsWithTurningCosts = new HashMap<>();
        for (Section outSection : outSections) {
            final float turningCost = determineTurningCost(inSection.getId(), outSection.getId());
            outSectionsWithTurningCosts.put(outSection, turningCost);
        }

        return outSectionsWithTurningCosts;
    }

    /**
     * Check if centroid is a local destination, then don't sort this centroid.
     *
     * @param directions
     * @param centroid
     */
    private void directionOfCentroids(Iterable<Direction> directions, Centroid centroid) {
        // Get their coordinates and determine their arc
        final Point2D.Double centroidCoord = centroid.getCoordinates();
        final double arcValue = determineArc(getJunction().getCoordinates(), centroidCoord);

        // run through the direction range map to get the right direction
        for (Direction direction : directions) {
            // centroids can exist in two directions if they are on the border
            final boolean isInRange = direction.getStartRange() <= arcValue && arcValue <= direction.getEndRange();
            if (isInRange || direction.getStartRange() == 0 && arcValue == 2 * Math.PI
                    || direction.getEndRange() == 2 * Math.PI && arcValue == 0) {
                // Get or create the list of centroids belonging to this direction
                List<Centroid> centroidInRangeList = centroidsInDirection.get(direction);
                if (centroidInRangeList == null) {
                    centroidInRangeList = new ArrayList<>();
                    centroidsInDirection.put(direction, centroidInRangeList);
                }
                centroidInRangeList.add(centroid);
            }
        }
    }

    /**
     * Determine outSections arc and sort intodirection->outSection Map.
     *
     * @param directions
     */
    private void directionOfOutSections(final Iterable<Direction> directions) {
        outSectionsInDirection = new HashMap<>();

        // Exclude OutSection which are leading to centroids
        Collection<Section> possibleOutSections = new ArrayList<>();

        for (Section possibleOutSection : getJunction().getOutSections()) {
            if (!outSectionDestinations.containsKey(possibleOutSection.getId())) {
                possibleOutSections.add(possibleOutSection);
            }
        }

        for (Section outSection : possibleOutSections) {
            // Check if there is a centroid next to this node
            if (outSection.getNextJunction() != null) {
                double arcValue = determineArc(getJunction().getCoordinates(), outSection.getNextJunction()
                        .getCoordinates());
                // run through the direction range map to get the right direction
                for (Direction direction : directions) {
                    // outSection can exist in two directions if they are on the border
                    // Exception: the first outSection comes also into the
                    // last direction and the last outSection into the first direction
                    final boolean isInRange = direction.getStartRange() <= arcValue
                            && arcValue <= direction.getEndRange();
                    if (isInRange || direction.getStartRange() == 0 && arcValue == 2 * Math.PI
                            || direction.getEndRange() == 2 * Math.PI && arcValue == 0) {
                        // Get or create the list of outSecions belonging to this direction
                        List<Section> outSectionsInRangeList = outSectionsInDirection.get(direction);
                        if (outSectionsInRangeList == null) {
                            outSectionsInRangeList = new ArrayList<>();
                            outSectionsInDirection.put(direction, outSectionsInRangeList);
                        }
                        outSectionsInRangeList.add(outSection);
                    }
                }
            }
        }
    }

    @Override
    public final void performProtocol() {
        if (!isInitialized) {
            initialise();
        }

        final List<Section> inSections = this.getJunction().getInSections();
        for (Section inSection : inSections) {
            // Routing Table for each section gets cleared
            RoutingTable routingTable = this.inSectionToRoutingTable.get(inSection.getId());
            routingTable.reset();

            final Map<Section, Float> outSectionsWithTurningCosts = determineTurningCosts(inSection);
            determineBestSection(routingTable, outSectionsWithTurningCosts);

            // insert local destinations
            for (Map.Entry<Integer, Integer> outSection : outSectionDestinations.entrySet()) {
                routingTable.insertRoutingData(outSection.getValue(), outSection.getKey(), 1);
            }
        }
    }

    private void determineBestSection(RoutingTable routingTable, Map<Section, Float> outSectionsWithTurningCosts) {
        for (Map.Entry<Direction, List<Section>> directionListEntry : outSectionsInDirection.entrySet()) {
            Section bestSection = null;
            float bestTurningCost;

            // Get the containing outSections
            List<Section> outSections = directionListEntry.getValue();
            if (outSections.size() == 2) {
                // case binary selection:
                float cost0 = outSectionsWithTurningCosts.get(outSections.get(0));
                float cost1 = outSectionsWithTurningCosts.get(outSections.get(1));

                if (cost0 < cost1) {
                    bestSection = outSections.get(0);
                    bestTurningCost = cost0;
                } else if (cost0 > cost1) {
                    bestSection = outSections.get(1);
                    bestTurningCost = cost1;
                } else {
                    // same, then random selection
                    final double randomValue = Math.random();
                    int index = 0;
                    bestTurningCost = cost0;
                    if (randomValue > .5) {
                        index = 1;
                    }
                    bestSection = outSections.get(index);
                }
            } else {
                // multi/single selection
                // sort out the one with the best turning value
                bestTurningCost = Float.MAX_VALUE;
                for (Section outSection : outSections) {
                    float turningCost = outSectionsWithTurningCosts.get(outSection);
                    if (turningCost > 0 && bestTurningCost > turningCost) {
                        bestSection = outSection;
                        bestTurningCost = turningCost;
                    }
                }
            }

            if (bestSection != null && bestTurningCost != -1) {
                updateRoutingData(routingTable, directionListEntry, bestSection.getId(), bestTurningCost);
            }
        }
    }

    private void initialise() {
        initialiseSectionMappings();
        determineNeighbourRCs();
        calculateOffsetToNeighbor();
        initialiseLocalDestinations();

        centroidsInDirection = new HashMap<>();

        initialiseDirections();
        isInitialized = true;
    }

    private void updateRoutingData(RoutingTable routingTable, Map.Entry<Direction, List<Section>> directionListEntry,
                                   int sectionID, float costs) {
        // Run over centroids of this direction
        for (Centroid centroidInDirection : centroidsInDirection.get(directionListEntry.getKey())) {
            float oldCosts = routingTable.getDelayForTarget(centroidInDirection.getId());

            if (Float.isNaN(oldCosts) || costs < oldCosts) {
                routingTable.updateRoutingData(centroidInDirection.getId(), sectionID, costs);
            }
        }
    }

    private void initialiseDirections() {
        List<Direction> directions = determineBorderOfRanges();
        for (Centroid centroid : OTCManager.getInstance().getNetwork().getCentroidMap().values()) {
            if (!outSectionDestinations.containsValue(centroid.getId())) {
                directionOfCentroids(directions, centroid);
            }

            directionOfOutSections(directions);
        }
    }
}
