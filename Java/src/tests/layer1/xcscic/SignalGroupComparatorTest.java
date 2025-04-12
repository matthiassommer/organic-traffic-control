package tests.layer1.xcscic;

import de.dfg.oc.otc.layer1.controller.xcscic.SignalGroupComparator;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.*;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Dominik on 01.04.2015.
 *
 * Tests if the {@link SignalGroup}s are ordered properly ascending by their id.
 *
 * @author Dominik Rauh
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SignalGroup.class})
public class SignalGroupComparatorTest
{
    @Mock private SignalGroup firstSignalGroup;
    @Mock private SignalGroup secondSignalGroup;
    @Mock private SignalGroup thirdSignalGroup;

    @Test
    public void signalGroupsShouldBeOrderedDescending()
    {
        when(firstSignalGroup.getId()).thenReturn(1);
        when(secondSignalGroup.getId()).thenReturn(2);
        when(thirdSignalGroup.getId()).thenReturn(3);

        Map<SignalGroup, Double> flowsPerSignalGroup = new TreeMap<>(new SignalGroupComparator());

        flowsPerSignalGroup.put(thirdSignalGroup, 3.0);
        flowsPerSignalGroup.put(secondSignalGroup, 2.0);
        flowsPerSignalGroup.put(firstSignalGroup, 1.0);

        SignalGroup[] sG = flowsPerSignalGroup.keySet().toArray(new SignalGroup[flowsPerSignalGroup.keySet().size()]);

        assertArrayEquals(new SignalGroup[]{firstSignalGroup, secondSignalGroup, thirdSignalGroup}, sG);
    }
}
