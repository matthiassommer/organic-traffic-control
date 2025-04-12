package tests.layer1.xcscic;

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.Interval;
import de.dfg.oc.otc.layer1.controller.LCSConstants;
import de.dfg.oc.otc.layer1.controller.xcscic.Classifier;
import de.dfg.oc.otc.layer1.controller.xcscic.ClassifierCondition;
import de.dfg.oc.otc.layer1.controller.xcscic.ClassifierSet;
import de.dfg.oc.otc.layer1.controller.xcscic.SignalGroupComparator;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.IDWInterpolant;
import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.values.AccumulatedIDWOTCValue;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

/**
 * Created by Dominik on 28.03.2015.
 * <p>
 * Tests if interpolation returns a plausible interpolated result and the corresponding weights.
 * Also tests whether the right natural neighbor is determined.
 *
 * @author Dominik Rauh
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(
        {IDWInterpolant.class,
                TrafficLightControllerParameters.class,
                ClassifierSet.class,
                Classifier.class,
                LCSConstants.class,
                ClassifierCondition.class,
                SignalGroup.class,
                Interval.class})
@SuppressStaticInitializationFor("de.dfg.oc.otc.layer1.controller.xcscic.interpolation.InterpolationConstants")
public class IDWInterpolantTest {
    private static final double EPSILON = 0.000001;

    @Mock
    private Situation queryPoint;
    @Mock
    private TrafficLightControllerParameters firstAction;
    @Mock
    private TrafficLightControllerParameters secondAction;
    @Mock
    private TrafficLightControllerParameters interpolatedAction;

    @Mock
    private SignalGroup firstSignalGroup;
    @Mock
    private SignalGroup secondSignalGroup;
    @Mock
    private SignalGroup thirdSignalGroup;

    private Map<SignalGroup, Double> queryPointFlowsOfSignalGroups;

    private ClassifierSet population;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Classifier firstClassifier;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Classifier secondClassifier;

    @Mock
    private Interval firstClassifierFirstInterval;
    @Mock
    private Interval firstClassifierSecondInterval;
    @Mock
    private Interval firstClassifierThirdInterval;

    @Mock
    private Interval secondClassifierFirstInterval;
    @Mock
    private Interval secondClassifierSecondInterval;
    @Mock
    private Interval secondClassifierThirdInterval;

    private IDWInterpolant interpolant;

    @Before
    public void setUp() {
        Whitebox.setInternalState(InterpolationConstants.class, "p_max", 5);
        Whitebox.setInternalState(InterpolationConstants.class, "IDW_EXPO", 5);
    }

    @Test
    public void shouldGetInterpolatedClassifier() throws Exception {
        setUpShouldGetInterpolatedClassifier();

        this.interpolant.interpolate(queryPoint, population);

        AccumulatedIDWOTCValue result = (AccumulatedIDWOTCValue) this.interpolant.getInterpolatedValue();
        Classifier interpolatedClassifier = result.getInterpolatedClassifier();

        //Assert the idw weights
        assertArrayEquals(new double[]{0.5, 0.5},
                ArrayUtils.toPrimitive(result.getWeightsForClassifiers().values().toArray(new Double[2])), (float) EPSILON);

        //Assert the creation of a interpolated classifier
        assertNotNull(interpolatedClassifier);

        //Assert the classifier's condition
        assertEquals(3, interpolatedClassifier.getCondition().getLength());
        assertEquals(60, interpolatedClassifier.getCondition().getInterval(0).getCenter(), EPSILON);
        assertEquals(120, interpolatedClassifier.getCondition().getInterval(1).getCenter(), EPSILON);
        assertEquals(180, interpolatedClassifier.getCondition().getInterval(2).getCenter(), EPSILON);

        //Assert the classifier's action
        assertArrayEquals(new float[]{20.0f, 30.0f, 40.0f},
                interpolatedClassifier.getAction().getGreenTimes(),
                (float) EPSILON);

        //Assert the classifier's performance related attributes
        assertEquals(0.4, interpolatedClassifier.getPrediction(), EPSILON);
        assertEquals(0.8, interpolatedClassifier.getPredictionError(), EPSILON);
        assertEquals(0.15, interpolatedClassifier.getFitness(), EPSILON);
        assertEquals(LCSConstants.getInstance().getExperienceInit(), interpolatedClassifier.getExperience(), EPSILON);
        assertEquals(1, interpolatedClassifier.getNumerosity());

        //Assert that the interpolation component created the interpolated classifier
        assertEquals(Classifier.Creator.IC, interpolatedClassifier.getCreatedBy());
    }

    @Test
    public void shouldGetNearestNeighbor() throws Exception {
        setUpShouldGetNearestNeighbor();

        this.interpolant.interpolate(queryPoint, population);

        AccumulatedIDWOTCValue result = (AccumulatedIDWOTCValue) this.interpolant.getInterpolatedValue();
        Classifier nearestNeighbor = result.getNearestNeighborClassifier();

        double weightOfFirstClassifier = result.getWeightsForClassifiers().get(firstClassifier);
        double weightOfSecondClassifier = result.getWeightsForClassifiers().get(secondClassifier);

        double weightOfFirstAction = result.getAccumulatedIDWeightForActionHash(firstAction.hashCode());
        double weightOfSecondAction = result.getAccumulatedIDWeightForActionHash(secondAction.hashCode());

        //The first classifier is the nearest neighbor of the query point so its weight should be bigger
        assertTrue(weightOfFirstClassifier > weightOfSecondClassifier);

        //So the weight of the classifier's corresponding action should also be bigger
        assertTrue(weightOfFirstAction > weightOfSecondAction);

        //Assert that a nearest neighbor was found
        assertNotNull(nearestNeighbor);

        //Assert that the right classifier was chosen nearest neighbor
        assertEquals(firstClassifier, nearestNeighbor);
    }

    private void setUpShouldGetInterpolatedClassifier() throws Exception {
        this.interpolant = new IDWInterpolant();

        queryPointFlowsOfSignalGroups = new TreeMap<>(new SignalGroupComparator());

        queryPointFlowsOfSignalGroups.put(firstSignalGroup, 60.0);
        queryPointFlowsOfSignalGroups.put(secondSignalGroup, 120.0);
        queryPointFlowsOfSignalGroups.put(thirdSignalGroup, 180.0);

        when(firstSignalGroup.toString()).thenReturn("SignalGroup1");
        when(firstSignalGroup.getId()).thenReturn(1);
        when(secondSignalGroup.toString()).thenReturn("SignalGroup2");
        when(secondSignalGroup.getId()).thenReturn(2);
        when(thirdSignalGroup.toString()).thenReturn("SignalGroup3");
        when(thirdSignalGroup.getId()).thenReturn(3);

        when(firstAction.getMaxSignalGroupFlowForDegreeOfSaturation(1)).thenReturn(new float[]{100.0f, 200.0f, 300.0f});
        when(secondAction.getMaxSignalGroupFlowForDegreeOfSaturation(1)).thenReturn(new float[]{200.0f, 300.0f, 400.0f});
        when(interpolatedAction.getMaxSignalGroupFlowForDegreeOfSaturation(1)).thenReturn(new float[]{200.0f, 300.0f, 400.0f});

        //1/60(intervalwidth) * situation
        when(firstAction.getNbOfLanesForSignalGroups()).thenReturn(new int[]{1, 2, 3});
        when(secondAction.getNbOfLanesForSignalGroups()).thenReturn(new int[]{2, 3, 4});
        when(interpolatedAction.getNbOfLanesForSignalGroups()).thenReturn(new int[]{1, 2, 3});

        when(firstAction.getGreenTimes()).thenReturn(new float[]{10.0f, 20.0f, 30.0f});
        when(secondAction.getGreenTimes()).thenReturn(new float[]{30.0f, 40.0f, 50.0f});

        whenNew(TrafficLightControllerParameters.class).withArguments(anyInt(), anyObject(), anyObject(), anyObject(), anyObject()).thenAnswer(
                new Answer<TrafficLightControllerParameters>() {
                    @Override
                    public TrafficLightControllerParameters answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        Object mock = invocation.getMock();

                        when(interpolatedAction.getGreenTimes()).thenReturn((float[]) args[2]);

                        return interpolatedAction;
                    }
                });

        doNothing().when(firstAction,
                method(TrafficLightControllerParameters.class,
                        "fillNumberOfLanesForSignalGroups")).withNoArguments();
        doNothing().when(secondAction,
                method(TrafficLightControllerParameters.class,
                        "fillNumberOfLanesForSignalGroups")).withNoArguments();

        doNothing().when(interpolatedAction,
                method(TrafficLightControllerParameters.class,
                        "fillNumberOfLanesForSignalGroups")).withNoArguments();

        when(queryPoint.getFlowsOfSignalGroup()).thenReturn(queryPointFlowsOfSignalGroups);

        when(firstClassifier.getAction()).thenReturn(firstAction);
        when(secondClassifier.getAction()).thenReturn(secondAction);

        when(firstClassifier.getPrediction()).thenReturn(0.3f);
        when(secondClassifier.getPrediction()).thenReturn(0.5f);

        when(firstClassifierFirstInterval.getCenter()).thenReturn(30.0f);
        when(firstClassifierSecondInterval.getCenter()).thenReturn(90.0f);
        when(firstClassifierThirdInterval.getCenter()).thenReturn(150.0f);

        when(secondClassifierFirstInterval.getCenter()).thenReturn(90.0f);
        when(secondClassifierSecondInterval.getCenter()).thenReturn(150.0f);
        when(secondClassifierThirdInterval.getCenter()).thenReturn(210.0f);

        List<Interval> firstIntervals = new ArrayList<>();
        firstIntervals.add(firstClassifierFirstInterval);
        firstIntervals.add(firstClassifierSecondInterval);
        firstIntervals.add(firstClassifierThirdInterval);

        List<Interval> secondIntervals = new ArrayList<>();
        secondIntervals.add(secondClassifierFirstInterval);
        secondIntervals.add(secondClassifierSecondInterval);
        secondIntervals.add(secondClassifierThirdInterval);

        when(firstClassifier.getCondition()).thenReturn(new ClassifierCondition(firstIntervals));
        when(secondClassifier.getCondition()).thenReturn(new ClassifierCondition(secondIntervals));

        doCallRealMethod().when(firstClassifier).setPredictionError(anyFloat());
        doCallRealMethod().when(firstClassifier).getPredictionError();
        firstClassifier.setPredictionError(0.7f);

        doCallRealMethod().when(firstClassifier).setFitness(anyFloat());
        doCallRealMethod().when(firstClassifier).getFitness();
        firstClassifier.setFitness(0.1f);

        doCallRealMethod().when(secondClassifier).setPredictionError(anyFloat());
        doCallRealMethod().when(secondClassifier).getPredictionError();
        secondClassifier.setPredictionError(0.9f);

        doCallRealMethod().when(secondClassifier).setFitness(anyFloat());
        doCallRealMethod().when(secondClassifier).getFitness();
        secondClassifier.setFitness(0.2f);

        this.population = new ClassifierSet();
        this.population.add(firstClassifier);
        this.population.add(secondClassifier);
    }

    private void setUpShouldGetNearestNeighbor() throws Exception {
        this.interpolant = new IDWInterpolant();

        queryPointFlowsOfSignalGroups = new TreeMap<>(new SignalGroupComparator());

        queryPointFlowsOfSignalGroups.put(firstSignalGroup, 60.0);
        queryPointFlowsOfSignalGroups.put(secondSignalGroup, 120.0);
        queryPointFlowsOfSignalGroups.put(thirdSignalGroup, 180.0);

        when(firstSignalGroup.toString()).thenReturn("SignalGroup1");
        when(firstSignalGroup.getId()).thenReturn(1);
        when(secondSignalGroup.toString()).thenReturn("SignalGroup2");
        when(secondSignalGroup.getId()).thenReturn(2);
        when(thirdSignalGroup.toString()).thenReturn("SignalGroup3");
        when(thirdSignalGroup.getId()).thenReturn(3);

        when(firstAction.getMaxSignalGroupFlowForDegreeOfSaturation(1)).thenReturn(new float[]{100.0f, 200.0f, 300.0f});
        when(secondAction.getMaxSignalGroupFlowForDegreeOfSaturation(1)).thenReturn(new float[]{200.0f, 300.0f, 400.0f});
        when(interpolatedAction.getMaxSignalGroupFlowForDegreeOfSaturation(1)).thenReturn(new float[]{200.0f, 300.0f, 400.0f});

        //1/60(intervalwidth) * situation
        when(firstAction.getNbOfLanesForSignalGroups()).thenReturn(new int[]{1, 2, 3});
        when(secondAction.getNbOfLanesForSignalGroups()).thenReturn(new int[]{2, 3, 4});
        when(interpolatedAction.getNbOfLanesForSignalGroups()).thenReturn(new int[]{1, 2, 3});

        when(firstAction.getGreenTimes()).thenReturn(new float[]{10.0f, 20.0f, 30.0f});
        when(secondAction.getGreenTimes()).thenReturn(new float[]{30.0f, 40.0f, 50.0f});

        whenNew(TrafficLightControllerParameters.class).withArguments(anyInt(), anyObject(), anyObject(), anyObject(), anyObject()).thenAnswer(
                new Answer<TrafficLightControllerParameters>() {
                    @Override
                    public TrafficLightControllerParameters answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        Object mock = invocation.getMock();

                        when(interpolatedAction.getGreenTimes()).thenReturn((float[]) args[2]);

                        return interpolatedAction;
                    }
                });

        doNothing().when(firstAction,
                method(TrafficLightControllerParameters.class,
                        "fillNumberOfLanesForSignalGroups")).withNoArguments();
        doNothing().when(secondAction,
                method(TrafficLightControllerParameters.class,
                        "fillNumberOfLanesForSignalGroups")).withNoArguments();

        doNothing().when(interpolatedAction,
                method(TrafficLightControllerParameters.class,
                        "fillNumberOfLanesForSignalGroups")).withNoArguments();

        when(queryPoint.getFlowsOfSignalGroup()).thenReturn(queryPointFlowsOfSignalGroups);

        when(firstClassifier.getAction()).thenReturn(firstAction);
        when(secondClassifier.getAction()).thenReturn(secondAction);

        when(firstClassifier.getPrediction()).thenReturn(0.3f);
        when(secondClassifier.getPrediction()).thenReturn(0.5f);

        when(firstClassifierFirstInterval.getCenter()).thenReturn(30.0f);
        when(firstClassifierSecondInterval.getCenter()).thenReturn(90.0f);
        when(firstClassifierThirdInterval.getCenter()).thenReturn(150.0f);

        when(secondClassifierFirstInterval.getCenter()).thenReturn(120.0f);
        when(secondClassifierSecondInterval.getCenter()).thenReturn(180.0f);
        when(secondClassifierThirdInterval.getCenter()).thenReturn(250.0f);

        List<Interval> firstIntervals = new ArrayList<>();
        firstIntervals.add(firstClassifierFirstInterval);
        firstIntervals.add(firstClassifierSecondInterval);
        firstIntervals.add(firstClassifierThirdInterval);

        List<Interval> secondIntervals = new ArrayList<>();
        secondIntervals.add(secondClassifierFirstInterval);
        secondIntervals.add(secondClassifierSecondInterval);
        secondIntervals.add(secondClassifierThirdInterval);

        when(firstClassifier.getCondition()).thenReturn(new ClassifierCondition(firstIntervals));
        when(secondClassifier.getCondition()).thenReturn(new ClassifierCondition(secondIntervals));

        doCallRealMethod().when(firstClassifier).setPredictionError(anyFloat());
        doCallRealMethod().when(firstClassifier).getPredictionError();
        firstClassifier.setPredictionError(0.7f);

        doCallRealMethod().when(firstClassifier).setFitness(anyFloat());
        doCallRealMethod().when(firstClassifier).getFitness();
        firstClassifier.setFitness(0.1f);

        doCallRealMethod().when(secondClassifier).setPredictionError(anyFloat());
        doCallRealMethod().when(secondClassifier).getPredictionError();
        secondClassifier.setPredictionError(0.9f);

        doCallRealMethod().when(secondClassifier).setFitness(anyFloat());
        doCallRealMethod().when(secondClassifier).getFitness();
        secondClassifier.setFitness(0.2f);

        this.population = new ClassifierSet();
        this.population.add(firstClassifier);
        this.population.add(secondClassifier);
    }
}
