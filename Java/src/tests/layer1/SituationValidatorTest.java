package tests.layer1;

import org.junit.Test;

import java.io.*;
import java.util.*;

public class SituationValidatorTest {
    private final Map<Double, double[]> measuredDemands = new HashMap<>();
    private final Map<Double, double[]> realDemands = new HashMap<>();

    @Test
    public final void test() {
        readMeasuredDemand();
        readRealDemand();

        Set<Double> keysForMeasurements = measuredDemands.keySet();
        Iterable<Double> sortedKeysForMeasurements = new TreeSet<>(keysForMeasurements);

        double timeForRealDemand = 21600;

        try {
            FileWriter diffStream = new FileWriter("diff.csv");
            BufferedWriter diffOut = new BufferedWriter(diffStream);

            FileWriter measStream = new FileWriter("measure.csv");
            BufferedWriter measOut = new BufferedWriter(measStream);

            for (double timeForMeasurement : sortedKeysForMeasurements) {
                if (timeForMeasurement + 19800 >= timeForRealDemand + 900) {
                    timeForRealDemand += 900;
                }

                double[] measuredDemand = measuredDemands.get(timeForMeasurement);
                double[] realDemand = realDemands.get(timeForRealDemand);

                double[] differenceDemand = new double[measuredDemand.length];
                for (int i = 0; i < differenceDemand.length; i++) {
                    differenceDemand[i] = realDemand[i] - measuredDemand[i];
                }

                String line;
                line = 19800 + timeForMeasurement + " " + Arrays.toString(measuredDemand);
                line = line.replaceAll(" \\[", "; ");
                line = line.replaceAll("\\]", "");
                line = line.replaceAll(",", ";");
                line = line.replaceAll("\\.", ",");
                measOut.write(line + "\n");

                line = 19800 + timeForMeasurement + " " + Arrays.toString(differenceDemand);
                line = line.replaceAll(" \\[", "; ");
                line = line.replaceAll("\\]", "");
                line = line.replaceAll(",", ";");
                line = line.replaceAll("\\.", ",");
                diffOut.write(line + "\n");

            }
            diffOut.close();
            measOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMeasuredDemand() {
        try {
            FileReader fr = new FileReader("NEWAVG7_REG.csv");
            BufferedReader br = new BufferedReader(fr);

            String line;
            String[] lineParts;
            while ((line = br.readLine()) != null) {
                lineParts = line.split(";");

                double time = new Double(lineParts[0].replace(",", "."));
                double[] situationInts = new double[lineParts.length - 1];
                for (int i = 1; i < lineParts.length; i++) {
                    situationInts[i - 1] = new Double(lineParts[i].replace(",", "."));
                }

                measuredDemands.put(time, situationInts);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readRealDemand() {
        try {
            FileReader fr = new FileReader("K7_realDemands_0600-1200.txt");
            BufferedReader br = new BufferedReader(fr);

            String line;
            String[] lineParts;
            String situationString;
            while ((line = br.readLine()) != null) {
                lineParts = line.split(";");

                double time = new Double(lineParts[0].replace(",", "."));

                // Get demand
                situationString = lineParts[1];
                situationString = situationString.replace("[", "");
                situationString = situationString.replace("]", "");
                String[] situationParts = situationString.split(", ");
                double[] situationInts = new double[situationParts.length];
                for (int i = 0; i < situationParts.length; i++) {
                    situationInts[i] = new Double(situationParts[i]) * 4;
                }

                realDemands.put(time, situationInts);

            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}