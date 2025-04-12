package de.dfg.oc.otc.layer2.ea;

/**
 * Abstract base class for selection operators.
 *
 * @author hpr
 */
abstract class AbstractSelection {
    final EA ea;

    AbstractSelection(final EA ea) {
        this.ea = ea;
    }

    /**
     * Abstract method, subclasses implement the selection operator in this
     * method.
     *
     * @return an array containing the selected individuals
     */
    protected abstract Individual[] selectParents();
}