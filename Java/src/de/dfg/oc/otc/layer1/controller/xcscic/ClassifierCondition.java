/*
 * Copyright (c) 2015 by
 * Anthony Stein, M.Sc.
 * University of Augsburg
 * Department of Computer Science
 * Chair of Organic Computing
 * All rights reserved. Distribution without approval by the copyright holder is explicitly prohibited.
 * Sources are only for non-commercial and academic use
 * in the scope of student theses and courses of the University of Augsburg.
 *
 * THE SOFTWAREPARTS ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package de.dfg.oc.otc.layer1.controller.xcscic;

import de.dfg.oc.otc.layer1.controller.ClassifierException;
import de.dfg.oc.otc.layer1.controller.Interval;
import de.dfg.oc.otc.layer1.controller.LCSConstants;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the condition part of a classifier. The condition defines the
 * inputs to which a classifier is applicable.
 *
 * @author hpr
 */
public class ClassifierCondition implements Cloneable {
    private static final Logger log = Logger.getLogger(ClassifierCondition.class);
    /**
     * List of intervals that form the classifier condition.
     */
    private List<Interval> intervals;

    /**
     * Creates a classifier condition with the given intervals.
     */
    public ClassifierCondition(final List<Interval> intervals) {
        this.intervals = intervals;
    }

    /**
     * Creates a classifier condition that matches the given situation.
     */
    ClassifierCondition(final float[] situation, final int[] nbLanes, final float[] maxFlow) {
        this.intervals = new ArrayList<>();

        final int size = situation.length;
        for (int i = 0; i < size; i++) {
            float low = Math
                    .max((int) situation[i] - nbLanes[i] * LCSConstants.getInstance().getIntervalWidthForNewConditions() / 2, 0);
            low = Math.min(low, maxFlow[i]);

            final float up = Math.min((int) situation[i] + nbLanes[i] * LCSConstants.getInstance().getIntervalWidthForNewConditions() / 2,
                    maxFlow[i]);

            this.intervals.add(new Interval(low, up));
        }
    }

    /**
     * Creates a classifier condition from a string containing the intervals.
     *
     * @param condString a string containing the intervals for the new condition
     */
    ClassifierCondition(String condString) {
        // Remove first "[" and last "]"
        condString = condString.substring(condString.indexOf("[") + 1, condString.lastIndexOf("]"));
        final String[] intervalStrings = condString.split("\\] \\[");

        // Create classifier condition
        this.intervals = new ArrayList<>();

        for (String intervalString : intervalStrings) {
            final String[] lowerUpper = intervalString.split(", ");
            final float lower = new Float(lowerUpper[0]);
            final float upper = new Float(lowerUpper[1]);
            this.intervals.add(new Interval(lower, upper));
        }
    }

    /**
     * Returns a new condition that is identical to this one.
     *
     * @return a new condition that is identical to this one
     */
    @Override
    public final ClassifierCondition clone() {
        final List<Interval> clonedIntervals = new ArrayList<>(this.intervals.size());

        for (Interval interval : this.intervals) {
            float lower = interval.getLower();
            float upper = interval.getUpper();
            Interval clonedInterval = new Interval(lower, upper);
            clonedIntervals.add(clonedInterval);
        }

        return new ClassifierCondition(clonedIntervals);
    }

    /**
     * Returns {@code true} if this ClassifierCondition contains the
     * ClassifierCondition given as parameter.
     *
     * @param condition condition that is checked for containment
     * @return {@code true} if this ClassifierCondition contains the
     * ClassifierCondition given as parameter
     */
    final boolean contains(final ClassifierCondition condition) {
        // Check length
        if (this.intervals.size() != condition.intervals.size()) {
            log.warn("Error in contains(): ClassifierCondition has wrong length.");
            return false;
        }

        final Iterator<Interval> argIterator = condition.intervals.iterator();
        for (Interval interval : this.intervals) {
            Interval argIntv = argIterator.next();

            if (interval.getLower() > argIntv.getLower() || interval.getUpper() < argIntv.getUpper()) {
                return false;
            }
        }

        // If this line is reached, _cond is covered by this condition.
        return true;
    }

    /**
     * Returns the {@code i}-th interval of this condition.
     *
     * @param i the index of the interval that should be obtained
     * @return the {@code i}-th interval
     */
    public final Interval getInterval(final int i) {
        return this.intervals.get(i);
    }

    /**
     * Returns the length of the classifier condition.
     *
     * @return the length of this condition
     */
    public final int getLength() {
        return this.intervals.size();
    }

    /**
     * Returns {@code true} if the classifier condition matches the
     * situation given as parameter.
     *
     * @param situation the situation that will be checked
     * @return {@code true} if the classifier condition matches the
     * situation
     * @throws de.dfg.oc.otc.layer1.controller.ClassifierException
     */
    final boolean matches(final float[] situation) throws ClassifierException {
        final int size = situation.length;
        if (size != this.getLength()) {
            throw new ClassifierException(
                    "ClassifierCondition.matches(double[] _situation): Situation has wrong length.");
        }

        for (int i = 0; i < size; i++) {
            if (this.intervals.get(i).contains(situation[i]) != 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public final String toString() {
        String condition = "";
        for (Interval interval : this.intervals) {
            condition += interval + " ";
        }
        return condition;
    }

    /**
     * Widens the condition as far as necessary to include the given situation.
     *
     * @param situation a situation that should be included in the condition
     */
    final void widen(final float[] situation) {
        for (int i = 0; i < situation.length; i++) {
            final Interval interval = intervals.get(i);

            // Checks are performed by intervals class!
            interval.setLower(new Double(Math.floor(situation[i])).intValue());
            interval.setUpper(new Double(Math.ceil(situation[i])).intValue());
        }
    }
}
