package tests.layer1.xcsic;

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationComponent;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.adjustment.WebstersDecisionFunction;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.IDWInterpolant;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values.OTCValue;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Matchers.*;
import static org.junit.Assert.*;

import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.powermock.api.mockito.PowerMockito.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Dominik on 23.03.2015.
 *
 * Tests if the threshold value (average distance between sampling points) is calculated correctly.
 * Also tests if a site will be delete when a better action for that site exists
 *
 * @author rauhdomi
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(WebstersDecisionFunction.class)
@SuppressStaticInitializationFor("de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants")
public class WebstersDecisionFunctionTest
{
    private static final double EPSILON = 0.1;

    @Mock private InterpolationComponent interpolationComponent;
    @Mock private IDWInterpolant interpolant;

    @Mock private Situation firstSituation;
    @Mock private Situation secondSituation;
    @Mock private Situation thirdSituation;
    @Mock private Situation fourthSituation;

    @Mock private SignalGroup firstSignalGroup;
    @Mock private SignalGroup secondSignalGroup;
    @Mock private SignalGroup thirdSignalGroup;

    @Mock private TrafficLightControllerParameters executedAction;
    @Mock private TrafficLightControllerParameters neNeAction;

    @Mock private OTCValue interpolatedValue;

    private Set<Situation> samplingPoints;

    private Map<SignalGroup, Double> firstFlowsOfSignalGroups;
    private Map<SignalGroup, Double> secondFlowsOfSignalGroups;
    private Map<SignalGroup, Double> thirdFlowsOfSignalGroups;
    private Map<SignalGroup, Double> fourthFlowsOfSignalGroups;

    private Method calculateSupersedeThresholdMethod;

    private WebstersDecisionFunction decisionFunction;

    @Test
    public void supersedeThresholdShouldBeCalculatedCorrectly() throws Exception
    {
        setUpSupersedeThresholdShouldBeCalculatedCorrectly();

        double threshold = (Double)calculateSupersedeThresholdMethod.invoke(decisionFunction);

        assertEquals(86.6, threshold, EPSILON);
    }

    @Test
    public void siteShouldBeSuperseeded() throws Exception
    {
        setUpSiteShouldBeSuperseeded();

        decisionFunction.decide(firstSituation, executedAction, interpolatedValue, 0, 0);

        Mockito.verify(interpolationComponent, Mockito.times(1)).deleteSite(anyObject());
    }

    @Test
    public void siteShouldNotBeSuperseeded() throws Exception
    {
        setUpSiteShouldNotBeSuperseeded();

        decisionFunction.decide(firstSituation, executedAction, interpolatedValue, 0, 0);

        Mockito.verify(interpolationComponent, Mockito.never()).deleteSite(anyObject());
    }

    private void setUpSupersedeThresholdShouldBeCalculatedCorrectly() throws NoSuchMethodException
    {
        calculateSupersedeThresholdMethod =
                WebstersDecisionFunction.class.getDeclaredMethod("calculateSupersedeThreshold");
        calculateSupersedeThresholdMethod.setAccessible(true);

        firstFlowsOfSignalGroups = new HashMap<>();
        secondFlowsOfSignalGroups = new HashMap<>();
        thirdFlowsOfSignalGroups = new HashMap<>();
        fourthFlowsOfSignalGroups = new HashMap<>();

        firstFlowsOfSignalGroups.put(firstSignalGroup, 10.0);
        firstFlowsOfSignalGroups.put(secondSignalGroup, 20.0);
        firstFlowsOfSignalGroups.put(thirdSignalGroup, 30.0);

        secondFlowsOfSignalGroups.put(firstSignalGroup, 40.0);
        secondFlowsOfSignalGroups.put(secondSignalGroup, 50.0);
        secondFlowsOfSignalGroups.put(thirdSignalGroup, 60.0);

        thirdFlowsOfSignalGroups.put(firstSignalGroup, 70.0);
        thirdFlowsOfSignalGroups.put(secondSignalGroup, 80.0);
        thirdFlowsOfSignalGroups.put(thirdSignalGroup, 90.0);

        fourthFlowsOfSignalGroups.put(firstSignalGroup, 100.0);
        fourthFlowsOfSignalGroups.put(secondSignalGroup, 110.0);
        fourthFlowsOfSignalGroups.put(thirdSignalGroup, 120.0);

        samplingPoints = new HashSet<>();
        samplingPoints.add(firstSituation);
        samplingPoints.add(secondSituation);
        samplingPoints.add(thirdSituation);
        samplingPoints.add(fourthSituation);

        when(firstSignalGroup.toString()).thenReturn("SignalGroup1");
        when(secondSignalGroup.toString()).thenReturn("SignalGroup2");
        when(thirdSignalGroup.toString()).thenReturn("SignalGroup3");
        when(fourthSituation.toString()).thenReturn("SignalGroup4");

        when(firstSituation.getFlowsOfSignalGroup()).thenReturn(firstFlowsOfSignalGroups);
        when(secondSituation.getFlowsOfSignalGroup()).thenReturn(secondFlowsOfSignalGroups);
        when(thirdSituation.getFlowsOfSignalGroup()).thenReturn(thirdFlowsOfSignalGroups);
        when(fourthSituation.getFlowsOfSignalGroup()).thenReturn(fourthFlowsOfSignalGroups);

        when(interpolant.getSamplingPoints()).thenReturn(samplingPoints);
        when(interpolationComponent.getInterpolant()).thenReturn(interpolant);

        decisionFunction = new WebstersDecisionFunction(interpolationComponent);
    }

    private void setUpSiteShouldBeSuperseeded() throws Exception
    {
        Whitebox.setInternalState(InterpolationConstants.class, "p_max", 1);

        firstFlowsOfSignalGroups = new HashMap<>();

        firstFlowsOfSignalGroups.put(firstSignalGroup, 10.0);
        firstFlowsOfSignalGroups.put(secondSignalGroup, 20.0);
        firstFlowsOfSignalGroups.put(thirdSignalGroup, 30.0);

        samplingPoints = new HashSet<>();
        samplingPoints.add(firstSituation);

        when(firstSignalGroup.toString()).thenReturn("SignalGroup1");
        when(secondSignalGroup.toString()).thenReturn("SignalGroup2");
        when(thirdSignalGroup.toString()).thenReturn("SignalGroup3");
        when(fourthSituation.toString()).thenReturn("SignalGroup4");

        when(firstSituation.getFlowsOfSignalGroup()).thenReturn(firstFlowsOfSignalGroups);
        when(interpolatedValue.getSituation()).thenReturn(firstSituation);
        when(interpolatedValue.getNearestNeighborAction()).thenReturn(neNeAction);

        when(interpolant.getSamplingPoints()).thenReturn(samplingPoints);
        when(interpolationComponent.getInterpolant()).thenReturn(interpolant);

        decisionFunction = spy(new WebstersDecisionFunction(interpolationComponent));

        doReturn(1.0).when(decisionFunction, method(WebstersDecisionFunction.class, "calculateSupersedeThreshold"))
                .withNoArguments();

        doReturn(1.0).when(decisionFunction,
                    method(WebstersDecisionFunction.class,
                            "websterAverageDelay",
                            Situation.class,
                            TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(executedAction));

        doReturn(2.0).when(decisionFunction,
                method(WebstersDecisionFunction.class,
                        "websterAverageDelay",
                        Situation.class,
                        TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(neNeAction));

    }

    private void setUpSiteShouldNotBeSuperseeded() throws Exception
    {
        Whitebox.setInternalState(InterpolationConstants.class, "p_max", 1);

        firstFlowsOfSignalGroups = new HashMap<>();

        firstFlowsOfSignalGroups.put(firstSignalGroup, 10.0);
        firstFlowsOfSignalGroups.put(secondSignalGroup, 20.0);
        firstFlowsOfSignalGroups.put(thirdSignalGroup, 30.0);

        samplingPoints = new HashSet<>();
        samplingPoints.add(firstSituation);

        when(firstSignalGroup.toString()).thenReturn("SignalGroup1");
        when(secondSignalGroup.toString()).thenReturn("SignalGroup2");
        when(thirdSignalGroup.toString()).thenReturn("SignalGroup3");
        when(fourthSituation.toString()).thenReturn("SignalGroup4");

        when(firstSituation.getFlowsOfSignalGroup()).thenReturn(firstFlowsOfSignalGroups);
        when(interpolatedValue.getSituation()).thenReturn(firstSituation);
        when(interpolatedValue.getNearestNeighborAction()).thenReturn(neNeAction);

        when(interpolant.getSamplingPoints()).thenReturn(samplingPoints);
        when(interpolationComponent.getInterpolant()).thenReturn(interpolant);

        decisionFunction = spy(new WebstersDecisionFunction(interpolationComponent));

        doReturn(1.0).when(decisionFunction, method(WebstersDecisionFunction.class, "calculateSupersedeThreshold"))
                .withNoArguments();

        doReturn(2.0).when(decisionFunction,
                method(WebstersDecisionFunction.class,
                        "websterAverageDelay",
                        Situation.class,
                        TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(executedAction));

        doReturn(1.0).when(decisionFunction,
                method(WebstersDecisionFunction.class,
                        "websterAverageDelay",
                        Situation.class,
                        TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(neNeAction));

    }
}
