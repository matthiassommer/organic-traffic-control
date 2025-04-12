package tests.aid.laSVM;

import de.dfg.oc.otc.aid.algorithms.svm.DetectorDataStorage;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by Christoph Weiss on 08.04.2015.
 */
public class DetectorDataStorageTest {
    private static DetectorDataStorage archive;
    private static List<DetectorDataValue> dataQueue;

    private static final int timeStepSize = 10;
    private static final float[] placeHolderValues = {0.0f, 0.0f, 0.0f, 0.0f};

    @BeforeClass
    public static void startSVMDataArchive() {
        archive = new DetectorDataStorage(20, timeStepSize, placeHolderValues, 1);
        dataQueue = archive.getValues();
    }

    @Before
    public void resetDataQueue() {
        archive.clear();
    }

    @Test
    public void addOneValue() {
        // @pre action
        dataQueue.clear();
        int archiveSizePre = archive.getValues().size();

        // take first action: add some new data to archive
        float time = 1210;
        if (!archive.getValues().isEmpty()) {
            if (archive.getValues().get(0).getTime() > time) {
                time = archive.getValues().get(0).getTime() + timeStepSize * 2;
            }
        }
        float[] data = {10, 5, 20, 5};
        DetectorDataValue value = new DetectorDataValue(time, data);

        archive.addValue(value, new float[]{0});

        // Test first action
        assertTrue(archive.getValues().size() == archiveSizePre + 1);
        assertTrue(archive.getValues().get(0).getTime() == (int) value.getTime() - value.getTime() % timeStepSize);
        assertTrue(archive.getValues().get(0).getValues().length == data.length);
        assertArrayEquals(data, archive.getValues().get(0).getValues(), 0.0f);
    }

    @Test
    public void addTwoValuesMerged() {
        // @pre action
        dataQueue.clear();
        int archiveSizePre = archive.getValues().size();

        // take first action: add some new data to archive
        float time = 1210;
        if (!archive.getValues().isEmpty()) {
            if (archive.getValues().get(0).getTime() > time) {
                time = archive.getValues().get(0).getTime() + timeStepSize * 2;
            }
        }

        float[] data = {10, 5, 20, 5};
        DetectorDataValue value = new DetectorDataValue(time, data);
        archive.addValue(value, new float[]{0});

        // take second action add a second data sample into archive. It lies in the same time slot as the first and
        // and should be merged
        float time2 = time + timeStepSize / 2f;
        float[] data2 = {15, 23, 10, 100};
        archive.addValue(new DetectorDataValue(time2, data2), new float[]{0});

        // Test second action
        assertTrue(archive.getValues().size() == archiveSizePre + 1);
        assertTrue(archive.getValues().get(0).getTime() == (int) value.getTime() - value.getTime() % timeStepSize);
        assertTrue(archive.getValues().get(0).getValues().length == data2.length);
        for (int i = 0; i < data2.length; i++) {
            assertTrue(archive.getValues().get(0).getValues()[i] == (data[i] + data2[i]) / 2);
        }

        // add value in different time slot
        float time3 = time + 20;
        float[] data3 = {15, 23, 10, 100};
        archive.addValue(new DetectorDataValue(time3, data3), new float[]{0});
    }

    @Test
    public void testGetValuesMethod() {
        assertSame(dataQueue, archive.getValues());
    }

    @Test
    public void getEntryVectorTest() {
        // take action: fill 10 random DetectorDataValue samples into the archive
        float startTime = 100;
        Random generator = new Random();

        DetectorDataValue[] dataSamples = new DetectorDataValue[10];
        float[] predictionSamples = new float[10];

        for (int i = 0; i < 10; i++) {
            float[] temp = new float[4];
            for (int j = 0; j < 4; j++) {
                temp[j] = generator.nextFloat();
            }

            dataSamples[i] = new DetectorDataValue(startTime + i * timeStepSize, temp);
            predictionSamples[i] = generator.nextFloat();

            archive.addValue(dataSamples[i], new float[]{predictionSamples[i]});
        }

        int j = 1;
        float[] expectedResult = new float[10];
        expectedResult[0] = predictionSamples[9];

        for (int i = 9; i >= 1; i--) {
            expectedResult[j++] = dataSamples[i].getValues()[DetectorCapabilities.SPEED];
        }

        float[] result = archive.getFeatureVector(startTime + 9 * timeStepSize, 9, new int[]{DetectorCapabilities.SPEED});

        // test action:
        assertArrayEquals(expectedResult, result, 0.0f);
    }

    @Test
    public void predictionValue() {
        // take action: fill 10 random DetectorDataValue samples into the archive
        float startTime = 100;
        Random generator = new Random();

        DetectorDataValue[] dataSamples = new DetectorDataValue[10];
        float[] predictionSamples = new float[10];

        for (int i = 0; i < 10; i++) {
            float[] temp = new float[4];
            for (int j = 0; j < 4; j++) {
                temp[j] = generator.nextFloat();
            }

            dataSamples[i] = new DetectorDataValue(startTime + i * timeStepSize, temp);
            predictionSamples[i] = generator.nextFloat();

            archive.addValue(dataSamples[i], new float[]{predictionSamples[i]});
        }
    }
}
