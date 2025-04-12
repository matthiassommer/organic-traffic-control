package de.dfg.oc.otc.routing.greedy;

/**
 * Keeps the range between two arcs. Stands for the direction in which the
 * outgoing section lies.
 *
 * @author Johannes
 */
class Direction {
    private final double endRange;
    private final double startRange;

    Direction(final double startRange, final double endRange) {
        this.startRange = startRange;
        this.endRange = endRange;
    }

    final double getEndRange() {
        return endRange;
    }

    final double getStartRange() {
        return startRange;
    }
}
