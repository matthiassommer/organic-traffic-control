package tests.layer1;

import de.dfg.oc.otc.layer1.observer.Attribute;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Layer1ObserverTest {
    @Test
    public final void testAttributeMapEvaluationForLCS() throws Exception {
        Attribute attribute = Attribute.LOS;
        float inputValue = 130;
        float maxValue = attribute.getMaximalValue();
        assertEquals("Default MaxValue for Attribute LOS", 200, maxValue, 0.1f);

        float mappedValue = attribute.mapEvaluationForLCS(inputValue);
        assertEquals("MappedValue for 130", 70, mappedValue, 0.1f);

        mappedValue = attribute.mapPredictionToEvaluation(mappedValue);
        assertEquals("Inverted Mapping", inputValue, mappedValue, 0.1f);

        attribute.setMaximalValue(100);
        maxValue = attribute.getMaximalValue();
        assertEquals("New MaxValue for Attribute LOS", 100, maxValue, 0.1f);

        mappedValue = attribute.mapEvaluationForLCS(inputValue);
        assertEquals("MappedValue for 130 with new MaxValue", 0, mappedValue, 0.1f);

        mappedValue = attribute.mapPredictionToEvaluation(mappedValue);
        assertEquals("Inverted Mapping", maxValue, mappedValue, 0.1f);

        attribute = Attribute.QUEUELENGTH;
        inputValue = 13;
        maxValue = attribute.getMaximalValue();
        assertEquals("Default MaxValue for Attribute QUEUELENGTH", 20, maxValue, 0.1f);

        mappedValue = attribute.mapEvaluationForLCS(inputValue);
        assertEquals("MappedValue for 13", 7, mappedValue, 0.1f);

        mappedValue = attribute.mapPredictionToEvaluation(mappedValue);
        assertEquals("Inverted Mapping", inputValue, mappedValue, 0.1f);

        attribute.setMaximalValue(10);
        maxValue = attribute.getMaximalValue();
        assertEquals("New MaxValue for Attribute QUEUELENGTH", 10, maxValue, 0.1f);

        mappedValue = attribute.mapEvaluationForLCS(inputValue);
        assertEquals("MappedValue for 13 with new MaxValue", 0, mappedValue, 0.1f);

        mappedValue = attribute.mapPredictionToEvaluation(mappedValue);
        assertEquals("Inverted Mapping", maxValue, mappedValue, 0.1f);
    }
}
