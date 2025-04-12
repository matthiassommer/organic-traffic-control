package de.dfg.oc.otc.tools;

import tests.evaluation.aid.AIDTrafficDataReader;

/**
 * Utility functions to find the min. and max. values in an array.
 * <p>
 * Created by oc6admin on 07.01.2016.
 */
public abstract class HelperFunctions {
    private static double findMax(double[] numbers) {
        double highest = numbers[0];
        for (int index = 1; index < numbers.length; index++) {
            if (numbers[index] > highest) {
                highest = numbers[index];
            }
        }
        return highest;
    }

    private static double findMin(double[] numbers) {
        double lowest = numbers[0];
        for (int index = 1; index < numbers.length; index++) {
            if (numbers[index] < lowest) {
                lowest = numbers[index];
            }
        }
        return lowest;
    }

    public static double getMaxValue(double[] values, int type) {
        double[] array = new double[values.length / AIDTrafficDataReader.SITUATION_LENGTH];
        int j = 0;
        for (int i = type; i < values.length; i += AIDTrafficDataReader.SITUATION_LENGTH) {
            array[j] = values[i];
            j++;
        }
        return findMax(array);
    }

    public static double getMinValue(double[] values, int type) {
        double[] array = new double[values.length / AIDTrafficDataReader.SITUATION_LENGTH];
        int j = 0;
        for (int i = type; i < values.length; i += AIDTrafficDataReader.SITUATION_LENGTH) {
            array[j] = values[i];
            j++;
        }
        return findMin(array);
    }

    public static double normalize(double allData, double minData, double maxData) {
        return (allData - minData) / (maxData - minData);
    }
}
