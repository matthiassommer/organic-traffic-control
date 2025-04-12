package de.dfg.oc.otc.layer1.controller;

import java.util.Formatter;

/**
 * Represents an interval with a lower and upper bound.
 *
 * @author hpr
 */
public class Interval {
    private float center;
    private float lowerBound;
    private float upperBound;

    /**
     * Creates an interval from the given arguments. The smaller argument forms
     * the lower bound, the larger argument forms the upper bound.
     *
     * @param lower the lower bound for the interval
     * @param upper the upper bound for the interval
     */
    public Interval(final float lower, final float upper) {
        if (lower <= upper) {
            this.lowerBound = lower;
            this.upperBound = upper;
        } else {
            this.lowerBound = upper;
            this.upperBound = lower;
        }
        calculateCenter();
    }

    /**
     * Method used to calculate the value of the center. Executed every time one
     * of the limits has been changed
     */
    private void calculateCenter() {
        final float val = (this.upperBound - this.lowerBound) / 2;
        this.center = this.upperBound - val;
    }

    /**
     * Returns {@code 0} if the value given as parameter is contained in
     * the interval. If the value is smaller than the the interval,
     * {@code -1} is returned. If the value is larger than the interval,
     * {@code 1} is returned.
     *
     * @param d the value that is compared to the interval
     * @return the result of the comparison (i.e. {@code -1, 0}, or
     * {@code 1})
     */
    public final int contains(final float d) {
        if (d < this.lowerBound) {
            return -1;
        } else if (d > this.upperBound) {
            return 1;
        }
        return 0;
    }

    public final float getCenter() {
        return this.center;
    }

    public final float getLower() {
        return this.lowerBound;
    }

    public final float getUpper() {
        return this.upperBound;
    }

    /**
     * Updates the lower bound of this interval if the parameter given is
     * smaller than the current lower bound.
     *
     * @param lower a candidate for a new lower bound
     */
    public final void setLower(final float lower) {
        if (lower < this.lowerBound) {
            this.lowerBound = lower;
        }
        calculateCenter();
    }

    /**
     * Updates the upper bound of this interval if the parameter given is larger
     * than the current upper bound.
     *
     * @param upper a candidate for a new upper bound
     */
    public final void setUpper(final float upper) {
        if (upper > this.upperBound) {
            this.upperBound = upper;
        }
        calculateCenter();
    }

    /**
     * Returns a string representation of this object. Since intervals are used
     * with integers in OTC, borders are formatted like {@code int} values
     * even if they are {@code floats}.
     *
     * @return a string representation of this object
     */
    @Override
    public final String toString() {
        final Formatter formatter = new Formatter();
        formatter.format("[%4.0f, ", this.lowerBound);
        formatter.format("%4.0f]", this.upperBound);
        final String s = formatter.toString();
        formatter.close();
        return s;
    }
}
