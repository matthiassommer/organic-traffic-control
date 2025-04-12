package tests.layer1.xcsic;

import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.components.evaluation.WebstersEvaluationFirstVariant;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by Dominik on 18.03.2015.
 *
 * Tests if the calculation of the trust value works correctly.
 * Also tests if the action rankings (superior, inferior, even) which are needed to calculate the trust value
 * are determined correctly
 *
 * @author rauhdomi
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(WebstersEvaluationFirstVariant.class)
@SuppressStaticInitializationFor("de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants")
public class WebstersEvaluationFirstVariantTest
{
    private static final double EPSILON = 0.0000000001;

    private Method calculateTrustValueIfWindowFullMethod;
    private WebstersEvaluationFirstVariant websterEvalMetric;

    @Mock private Situation situation;

    @Mock private TrafficLightControllerParameters executedAction;
    @Mock private TrafficLightControllerParameters interpolatedAction;

    @Before
    public void setUp() throws IllegalAccessException, NoSuchFieldException, NoSuchMethodException {
        this.websterEvalMetric = spy(new WebstersEvaluationFirstVariant());

        this.calculateTrustValueIfWindowFullMethod =
                WebstersEvaluationFirstVariant.class.getDeclaredMethod("calculateTrustValueIfWindowFull");
        this.calculateTrustValueIfWindowFullMethod.setAccessible(true);

        Whitebox.setInternalState(InterpolationConstants.class, "p_max", 5);
        Whitebox.setInternalState(InterpolationConstants.class, "t_window", 5);
    }

    @Test
    public void calculateTrustValueAllSuperior() throws Exception
    {
        websterEvalMetric.getInterpolatedActionRanking().put(WebstersEvaluationFirstVariant.RANKING_SUPERIOR, 5);
        calculateTrustValueIfWindowFullMethod.invoke(websterEvalMetric);

        double trust = websterEvalMetric.getEvaluationValue();

        assertEquals(1.0, trust, EPSILON);
    }

    @Test
    public void calculateTrustValueAllInferior() throws Exception
    {
        websterEvalMetric.getInterpolatedActionRanking().put(WebstersEvaluationFirstVariant.RANKING_INFERIOR, 5);
        calculateTrustValueIfWindowFullMethod.invoke(websterEvalMetric);

        double trust = websterEvalMetric.getEvaluationValue();

        assertEquals(0.0, trust, EPSILON);
    }

    @Test
    public void calculateTrustValueAllEven() throws Exception
    {
        websterEvalMetric.getInterpolatedActionRanking().put(WebstersEvaluationFirstVariant.RANKING_EVEN, 5);
        calculateTrustValueIfWindowFullMethod.invoke(websterEvalMetric);

        double trust = websterEvalMetric.getEvaluationValue();

        assertEquals(0.0, trust, EPSILON);
    }

    @Test
    public void calculateTrustValueAllMax() throws Exception
    {
        websterEvalMetric.getInterpolatedActionRanking().put(WebstersEvaluationFirstVariant.RANKING_EVEN, 5);
        websterEvalMetric.getInterpolatedActionRanking().put(WebstersEvaluationFirstVariant.RANKING_SUPERIOR, 5);
        websterEvalMetric.getInterpolatedActionRanking().put(WebstersEvaluationFirstVariant.RANKING_INFERIOR, 5);
        calculateTrustValueIfWindowFullMethod.invoke(websterEvalMetric);

        double trust = websterEvalMetric.getEvaluationValue();

        assertEquals(0.5, trust, EPSILON);
    }

    @Test
    public void calculateTrustValueZeroSuperior() throws Exception
    {
        websterEvalMetric.getInterpolatedActionRanking().put(WebstersEvaluationFirstVariant.RANKING_EVEN, 2);
        websterEvalMetric.getInterpolatedActionRanking().put(WebstersEvaluationFirstVariant.RANKING_INFERIOR, 5);
        calculateTrustValueIfWindowFullMethod.invoke(websterEvalMetric);

        double trust = websterEvalMetric.getEvaluationValue();

        assertEquals(0.0, trust, EPSILON);
    }

    @Test
    public void rankingShouldBeSuperior() throws Exception
    {
        setUpRankingShouldBeSuperior();

        websterEvalMetric.evaluate(situation, executedAction, interpolatedAction, 0, 0);
        Map<String, Integer> actionRanking = websterEvalMetric.getInterpolatedActionRanking();

        assertEquals(1, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_SUPERIOR).intValue());
        assertEquals(0, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_INFERIOR).intValue());
        assertEquals(0, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_EVEN).intValue());
    }

    @Test
    public void rankingShouldBeInferior() throws Exception
    {
        setUpRankingShouldBeInferior();

        websterEvalMetric.evaluate(situation, executedAction, interpolatedAction, 0, 0);
        Map<String, Integer> actionRanking = websterEvalMetric.getInterpolatedActionRanking();

        assertEquals(0, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_SUPERIOR).intValue());
        assertEquals(1, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_INFERIOR).intValue());
        assertEquals(0, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_EVEN).intValue());
    }

    @Test
    public void rankingShouldBeEven() throws Exception
    {
        setUpRankingShouldBeEven();

        websterEvalMetric.evaluate(situation, executedAction, interpolatedAction, 0, 0);
        Map<String, Integer> actionRanking = websterEvalMetric.getInterpolatedActionRanking();

        assertEquals(0, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_SUPERIOR).intValue());
        assertEquals(0, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_INFERIOR).intValue());
        assertEquals(1, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_EVEN).intValue());
    }

    @Test
    public void rankingShouldBeEvenBecauseActionEquals() throws Exception
    {
        websterEvalMetric.evaluate(situation, executedAction, executedAction, 0, 0);
        Map<String, Integer> actionRanking = websterEvalMetric.getInterpolatedActionRanking();

        assertEquals(0, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_SUPERIOR).intValue());
        assertEquals(0, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_INFERIOR).intValue());
        assertEquals(1, actionRanking.get(WebstersEvaluationFirstVariant.RANKING_EVEN).intValue());
    }

    private void setUpRankingShouldBeSuperior() throws Exception
    {
        doReturn(1.0).when(websterEvalMetric,
                method(WebstersEvaluationFirstVariant.class,
                        "websterAverageDelay",
                        Situation.class,
                        TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(interpolatedAction));

        doReturn(2.0).when(websterEvalMetric,
                method(WebstersEvaluationFirstVariant.class,
                        "websterAverageDelay",
                        Situation.class,
                        TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(executedAction));
    }

    private void setUpRankingShouldBeInferior() throws Exception
    {
        doReturn(2.0).when(websterEvalMetric,
                method(WebstersEvaluationFirstVariant.class,
                        "websterAverageDelay",
                        Situation.class,
                        TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(interpolatedAction));

        doReturn(1.0).when(websterEvalMetric,
                method(WebstersEvaluationFirstVariant.class,
                        "websterAverageDelay",
                        Situation.class,
                        TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(executedAction));
    }

    private void setUpRankingShouldBeEven() throws Exception
    {
        doReturn(1.0).when(websterEvalMetric,
                method(WebstersEvaluationFirstVariant.class,
                        "websterAverageDelay",
                        Situation.class,
                        TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(interpolatedAction));

        doReturn(1.0).when(websterEvalMetric,
                method(WebstersEvaluationFirstVariant.class,
                        "websterAverageDelay",
                        Situation.class,
                        TrafficLightControllerParameters.class)).withArguments(anyObject(), eq(executedAction));
    }
}
