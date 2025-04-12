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

package de.dfg.oc.otc.layer1.controller.xcscic.interpolation;

import de.dfg.oc.otc.layer1.controller.xcscic.SignalGroupComparator;
import de.dfg.oc.otc.layer2.TurningData;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Dominik on 12.03.2015.
 *
 * Encapsulates a Situation i.e. the current flows of a signal group and also stores the corresponding turnings
 * This Situation may be passed to the {@link InterpolationComponent} in order to interpolate a signal plan based on this
 * situation.
 *
 * The flows of signal groups have to bo ordered by the signal group id.
 * Thus a {@link TreeMap} with the {@link SignalGroupComparator} has to be passed during instantiation.
 *
 * @author Dominik Rauh
 *
 */
public class Situation
{
    /** EPSILON for the testing of equality of double values */
    private static final double EPSILON = 0.000000000001;

    private Map<SignalGroup, Double> flowsOfSignalGroup;
    private Map<String, TurningData> turningDataMap;

    public Situation(TreeMap<SignalGroup, Double> flowsOfSignalGroup, Map<String, TurningData> turningDataMap)
    {
        if(!(flowsOfSignalGroup.comparator() instanceof SignalGroupComparator))
        {
            throw new IllegalArgumentException("Flows of signal groups are not ordered through the SignalGroupComparator!");
        }
        this.flowsOfSignalGroup = flowsOfSignalGroup;
        this.turningDataMap = turningDataMap;
    }

    public Map<SignalGroup, Double> getFlowsOfSignalGroup() {
        return this.flowsOfSignalGroup;
    }

    public Map<String, TurningData> getTurningDataMap()
    {
        return this.turningDataMap;
    }

    /**
     * A Situation is equal when the flows of the contained signal groups are equal
     * @param o The Situation to check equality for
     * @return true if the situation is equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Situation situation = (Situation) o;

        if(this.flowsOfSignalGroup.size() != situation.flowsOfSignalGroup.size()) return false;

        for(Map.Entry<SignalGroup, Double> entryThis : this.flowsOfSignalGroup.entrySet())
        {
            Map.Entry<SignalGroup, Double> matchingCompareEntry = null;

            for(Map.Entry<SignalGroup, Double> entryCompare : situation.flowsOfSignalGroup.entrySet())
            {
                if(entryCompare.getKey().toString().equals(entryThis.getKey().toString())){
                    matchingCompareEntry = entryCompare;
                    break;
                }
            }

            if(matchingCompareEntry == null) { return false; }
            if(!flowsAreEqual(entryThis.getValue(), matchingCompareEntry.getValue())) { return false; }
        }

        return true;
    }

    @Override
    public int hashCode() {

        int result = 0;

        for(Double v: flowsOfSignalGroup.values())
        {
            result += (int)v.doubleValue();
        }

        return result;
    }

    private boolean flowsAreEqual(double flowThis, double flowCompare)
    {
        boolean isSmaller = ((flowThis - EPSILON) < flowCompare);
        boolean isBigger = ((flowThis + EPSILON) > flowCompare);

        return isSmaller && isBigger;
    }

    private boolean bitwiseEqualsWithCanonicalNaN(double x, double y) {
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(y);
    }
}