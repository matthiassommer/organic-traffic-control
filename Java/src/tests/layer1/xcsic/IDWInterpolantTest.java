package tests.layer1.xcsic;

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationComponent;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationComponentException;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.IDWInterpolant;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.interpolants.values.AccumulatedIDWOTCValue;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by Dominik on 24.03.2015.
 * <p>
 * Tests the addition and deletion of sites from the interpolant.
 * Tests if interpolation returns a plausible interpolated result and the corresponding weights
 * Also tests if the right natural neighbor (Situation and TrafficLightControllerParameters) are determined
 *
 * @author rauhdomi
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({IDWInterpolant.class, TrafficLightControllerParameters.class})
@SuppressStaticInitializationFor("de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants")
public class IDWInterpolantTest {

    private static final double EPSILON = 0.01;

    @Mock
    private InterpolationComponent interpolationComponent;

    @Mock
    private Situation firstSituation;
    @Mock
    private Situation secondSituation;
    @Mock
    private Situation queryPoint;

    @Mock
    private SignalGroup firstSignalGroup;
    @Mock
    private SignalGroup secondSignalGroup;
    @Mock
    private SignalGroup thirdSignalGroup;

    @Mock
    private TrafficLightControllerParameters firstAction;
    @Mock
    private TrafficLightControllerParameters secondAction;

    private Map<SignalGroup, Double> firstFlowsOfSignalGroups;
    private Map<SignalGroup, Double> secondFlowsOfSignalGroups;
    private Map<SignalGroup, Double> queryPointFlowsOfSignalGroups;

    private IDWInterpolant interpolant;

    @Before
    public void setUp() {
        Whitebox.setInternalState(InterpolationConstants.class, "p_max", 5);
        Whitebox.setInternalState(InterpolationConstants.class, "IDW_EXPO", 5);
    }

    @Test
    public void shouldAddSite() throws InterpolationComponentException {
        this.interpolant = new IDWInterpolant();

        this.interpolant.addNewSite(firstSituation, new Object[]{firstAction});

        assertEquals(firstSituation, this.interpolant.getSamplingPoints().iterator().next());
    }

    @Test(expected = InterpolationComponentException.class)
    public void shouldNotAddSiteBecauseEqualOneExists() throws InterpolationComponentException {
        this.interpolant = new IDWInterpolant();

        this.interpolant.addNewSite(firstSituation, new Object[]{firstAction});
        this.interpolant.addNewSite(firstSituation, new Object[]{firstAction});
    }

    @Test
    public void shouldDeleteSite() throws InterpolationComponentException {
        this.interpolant = new IDWInterpolant();

        this.interpolant.addNewSite(firstSituation, new Object[]{firstAction});
        this.interpolant.removeSite(firstSituation);

        assertEquals(0, this.interpolant.getSamplingPoints().size());
    }

    @Test(expected = InterpolationComponentException.class)
    public void shouldThrowExceptionBecauseSiteDoesNotExist() throws InterpolationComponentException {
        this.interpolant = new IDWInterpolant();

        this.interpolant.addNewSite(firstSituation, new Object[]{firstAction});

        this.interpolant.removeSite(secondSituation);
    }

    @Test
    public void shouldGetInterpolatedAction() throws InterpolationComponentException {
        setUpShouldGetInterpolatedAction();

        AccumulatedIDWOTCValue interpolatedValue = (AccumulatedIDWOTCValue) this.interpolant.interpolate(queryPoint);
        Map<TrafficLightControllerParameters, Double> weights = interpolatedValue.getWeightsForActions();

        assertEquals(2, weights.size());
        assertEquals(weights.get(firstAction), 0.5, EPSILON);
        assertEquals(weights.get(secondAction), 0.5, EPSILON);

        assertArrayEquals(new float[]{20.0f, 30.0f, 40.0f},
                interpolatedValue.getInterpolatedAction().getGreenTimes(),
                (float) EPSILON);
    }

    @Test
    public void shouldGetNearestNeighbor() throws InterpolationComponentException {
        setUpShouldGetNearestNeighbor();

        AccumulatedIDWOTCValue interpolatedValue = (AccumulatedIDWOTCValue) this.interpolant.interpolate(queryPoint);
        Map<TrafficLightControllerParameters, Double> weights = interpolatedValue.getWeightsForActions();

        Situation neNeSituation = interpolatedValue.getSituation();
        TrafficLightControllerParameters neNeAction = interpolatedValue.getNearestNeighborAction();

        double weightOfFirstAction = interpolatedValue.getWeightsForActions().get(firstAction);
        double weightOfSecondAction = interpolatedValue.getWeightsForActions().get(secondAction);

        assertTrue(weightOfFirstAction < weightOfSecondAction);

        assertEquals(secondSituation, neNeSituation);
        assertEquals(secondAction, neNeAction);
    }

    private void setUpShouldGetInterpolatedAction() throws InterpolationComponentException {
        firstFlowsOfSignalGroups = new HashMap<>();
        secondFlowsOfSignalGroups = new HashMap<>();
        queryPointFlowsOfSignalGroups = new HashMap<>();

        firstFlowsOfSignalGroups.put(firstSignalGroup, 10.0);
        firstFlowsOfSignalGroups.put(secondSignalGroup, 20.0);
        firstFlowsOfSignalGroups.put(thirdSignalGroup, 30.0);

        secondFlowsOfSignalGroups.put(firstSignalGroup, 30.0);
        secondFlowsOfSignalGroups.put(secondSignalGroup, 40.0);
        secondFlowsOfSignalGroups.put(thirdSignalGroup, 50.0);

        queryPointFlowsOfSignalGroups.put(firstSignalGroup, 20.0);
        queryPointFlowsOfSignalGroups.put(secondSignalGroup, 30.0);
        queryPointFlowsOfSignalGroups.put(thirdSignalGroup, 40.0);

        when(firstSignalGroup.toString()).thenReturn("SignalGroup1");
        when(secondSignalGroup.toString()).thenReturn("SignalGroup2");
        when(thirdSignalGroup.toString()).thenReturn("SignalGroup3");

        when(firstSituation.getFlowsOfSignalGroup()).thenReturn(firstFlowsOfSignalGroups);
        when(secondSituation.getFlowsOfSignalGroup()).thenReturn(secondFlowsOfSignalGroups);
        when(queryPoint.getFlowsOfSignalGroup()).thenReturn(queryPointFlowsOfSignalGroups);

        when(firstAction.getGreenTimes()).thenReturn(new float[]{10.0f, 20.0f, 30.0f});
        when(secondAction.getGreenTimes()).thenReturn(new float[]{30.0f, 40.0f, 50.0f});

        this.interpolant = new IDWInterpolant();
        this.interpolant.addNewSite(firstSituation, new Object[]{firstAction});
        this.interpolant.addNewSite(secondSituation, new Object[]{secondAction});
    }

    private void setUpShouldGetNearestNeighbor() throws InterpolationComponentException {
        firstFlowsOfSignalGroups = new HashMap<>();
        secondFlowsOfSignalGroups = new HashMap<>();
        queryPointFlowsOfSignalGroups = new HashMap<>();

        firstFlowsOfSignalGroups.put(firstSignalGroup, 10.0);
        firstFlowsOfSignalGroups.put(secondSignalGroup, 20.0);
        firstFlowsOfSignalGroups.put(thirdSignalGroup, 30.0);

        secondFlowsOfSignalGroups.put(firstSignalGroup, 30.0);
        secondFlowsOfSignalGroups.put(secondSignalGroup, 40.0);
        secondFlowsOfSignalGroups.put(thirdSignalGroup, 50.0);

        queryPointFlowsOfSignalGroups.put(firstSignalGroup, 25.0);
        queryPointFlowsOfSignalGroups.put(secondSignalGroup, 35.0);
        queryPointFlowsOfSignalGroups.put(thirdSignalGroup, 45.0);

        when(firstSignalGroup.toString()).thenReturn("SignalGroup1");
        when(secondSignalGroup.toString()).thenReturn("SignalGroup2");
        when(thirdSignalGroup.toString()).thenReturn("SignalGroup3");

        when(firstSituation.getFlowsOfSignalGroup()).thenReturn(firstFlowsOfSignalGroups);
        when(secondSituation.getFlowsOfSignalGroup()).thenReturn(secondFlowsOfSignalGroups);
        when(queryPoint.getFlowsOfSignalGroup()).thenReturn(queryPointFlowsOfSignalGroups);

        when(firstAction.getGreenTimes()).thenReturn(new float[]{10.0f, 20.0f, 30.0f});
        when(secondAction.getGreenTimes()).thenReturn(new float[]{30.0f, 40.0f, 50.0f});

        this.interpolant = new IDWInterpolant();
        this.interpolant.addNewSite(firstSituation, new Object[]{firstAction});
        this.interpolant.addNewSite(secondSituation, new Object[]{secondAction});
    }
}
