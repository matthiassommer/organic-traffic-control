package tests.layer1.xcsic;

import de.dfg.oc.otc.layer1.controller.xcsic.SignalGroupComparator;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import de.dfg.oc.otc.manager.aimsun.Turning;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Dominik on 18.03.2015.
 * <p>
 * Tests if a situation will be evaluated as equal, if the situations' flows are equal
 *
 * @author rauhdomi
 */
public class SituationTest {
    private Situation firstSituation;
    private Situation secondSituation;

    private SignalGroup firstSignalGroupStub;
    private SignalGroup secondSignalGroupStub;
    private SignalGroup thirdSignalGroupStub;
    private SignalGroup fourthSignalGroupStub;

    @Before
    public void setUp() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Section inSection = new Section(1, 49, 1, new int[]{2}, 2, 50, 200);
        Section outSection = new Section(3, 49, 1, new int[]{4}, 2, 50, 200);

        Constructor<Turning> turningConstructor = Turning.class.getDeclaredConstructor(Integer.TYPE, Section.class, Section.class);
        turningConstructor.setAccessible(true);
        Turning turning = turningConstructor.newInstance(1, inSection, outSection);

        Constructor<SignalGroup> signalGroupConstructor = SignalGroup.class.getDeclaredConstructor(Integer.TYPE);
        signalGroupConstructor.setAccessible(true);
        this.firstSignalGroupStub = signalGroupConstructor.newInstance(1);
        this.secondSignalGroupStub = signalGroupConstructor.newInstance(2);
        this.thirdSignalGroupStub = signalGroupConstructor.newInstance(3);
        this.fourthSignalGroupStub = signalGroupConstructor.newInstance(2);

        Method addTurningMethod = SignalGroup.class.getDeclaredMethod("addTurning", Turning.class);
        addTurningMethod.setAccessible(true);
        addTurningMethod.invoke(this.firstSignalGroupStub, turning);
        addTurningMethod.invoke(this.secondSignalGroupStub, turning);
        addTurningMethod.invoke(this.thirdSignalGroupStub, turning);
        addTurningMethod.invoke(this.fourthSignalGroupStub, turning);
    }

    @Test
    public void shouldNotThrowExceptionBecauseSignalGroupComparatorUsed() {
        TreeMap<SignalGroup, Double> flowsOfFirstSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfFirstSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfFirstSignalGroups.put(this.secondSignalGroupStub, 2.3);

        firstSituation = new Situation(flowsOfFirstSignalGroups, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionBecauseSignalGroupComparatorNotUsed() {
        TreeMap<SignalGroup, Double> flowsOfFirstSignalGroups = new TreeMap<>(new Comparator<SignalGroup>() {
            @Override
            public int compare(SignalGroup a, SignalGroup b) {
                if (a.getId() < b.getId()) {
                    return 0;
                } else {
                    return 0;
                }
            }
        });
        flowsOfFirstSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfFirstSignalGroups.put(this.secondSignalGroupStub, 2.3);

        firstSituation = new Situation(flowsOfFirstSignalGroups, null);
    }

    @Test
    public void situationsShouldBeEqual() {
        setUpSituationsShouldBeEqual();

        assertTrue(firstSituation.equals(secondSituation));
    }

    @Test
    public void situationsShouldNotBeEqualBecauseFlowsNotOfEqualValue() {
        setUpSituationsShouldNotBeEqualBecauseFlowsNotOfEqualValue();

        assertFalse(firstSituation.equals(secondSituation));
    }

    @Test
    public void situationsShouldNotBeEqualBecauseFlowsNotOfEqualLength() {
        setUpSituationsShouldNotBeEqualBecauseFlowsNotOfEqualLength();

        assertFalse(firstSituation.equals(secondSituation));
    }

    @Test
    public void situationsShouldNotBeEqualBecauseSignalGroupsDiffer() {
        setUpSituationsShouldNotBeEqualBecauseSignalGroupsDiffer();

        assertFalse(firstSituation.equals(secondSituation));
    }

    @Test
    public void situationShouldNotBeAddedToSetBecauseOfEquality() {
        setUpSituationShouldNotBeAddedToSetBecauseOfEquality();

        Set<Situation> situations = new HashSet<>();
        situations.add(this.firstSituation);

        assertFalse(situations.add(secondSituation));
    }

    private void setUpSituationsShouldBeEqual() {
        TreeMap<SignalGroup, Double> flowsOfFirstSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfFirstSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfFirstSignalGroups.put(this.secondSignalGroupStub, 2.3);

        TreeMap<SignalGroup, Double> flowsOfSecondSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfSecondSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfSecondSignalGroups.put(this.secondSignalGroupStub, 2.3);

        firstSituation = new Situation(flowsOfFirstSignalGroups, null);
        secondSituation = new Situation(flowsOfSecondSignalGroups, null);
    }

    private void setUpSituationsShouldNotBeEqualBecauseFlowsNotOfEqualValue() {
        TreeMap<SignalGroup, Double> flowsOfFirstSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfFirstSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfFirstSignalGroups.put(this.secondSignalGroupStub, 2.3);

        TreeMap<SignalGroup, Double> flowsOfSecondSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfSecondSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfSecondSignalGroups.put(this.secondSignalGroupStub, 2.3000008);

        firstSituation = new Situation(flowsOfFirstSignalGroups, null);
        secondSituation = new Situation(flowsOfSecondSignalGroups, null);
    }

    private void setUpSituationsShouldNotBeEqualBecauseFlowsNotOfEqualLength() {
        TreeMap<SignalGroup, Double> flowsOfFirstSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfFirstSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfFirstSignalGroups.put(this.secondSignalGroupStub, 2.3);

        TreeMap<SignalGroup, Double> flowsOfSecondSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfSecondSignalGroups.put(this.firstSignalGroupStub, 10.1);

        firstSituation = new Situation(flowsOfFirstSignalGroups, null);
        secondSituation = new Situation(flowsOfSecondSignalGroups, null);
    }

    private void setUpSituationsShouldNotBeEqualBecauseSignalGroupsDiffer() {
        TreeMap<SignalGroup, Double> flowsOfFirstSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfFirstSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfFirstSignalGroups.put(this.secondSignalGroupStub, 2.3);

        TreeMap<SignalGroup, Double> flowsOfSecondSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfSecondSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfSecondSignalGroups.put(this.thirdSignalGroupStub, 2.3);

        firstSituation = new Situation(flowsOfFirstSignalGroups, null);
        secondSituation = new Situation(flowsOfSecondSignalGroups, null);
    }

    private void setUpSituationShouldNotBeAddedToSetBecauseOfEquality() {
        TreeMap<SignalGroup, Double> flowsOfFirstSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfFirstSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfFirstSignalGroups.put(this.secondSignalGroupStub, 2.3);

        TreeMap<SignalGroup, Double> flowsOfSecondSignalGroups = new TreeMap<>(new SignalGroupComparator());
        flowsOfSecondSignalGroups.put(this.firstSignalGroupStub, 10.1);
        flowsOfSecondSignalGroups.put(this.fourthSignalGroupStub, 2.3);

        firstSituation = new Situation(flowsOfFirstSignalGroups, null);
        secondSituation = new Situation(flowsOfSecondSignalGroups, null);
    }
}
