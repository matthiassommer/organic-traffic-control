package de.dfg.oc.otc.aid.algorithms.knn;

import com.Ostermiller.util.CSVParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexandermartel on 20.03.15.
 */
class KNNDetectorDataValueImporter {
    static List<KNNDetectorDataValue> readTrainingData(FileInputStream fileStream) throws IOException {
        List<KNNDetectorDataValue> list = new ArrayList<>();

        CSVParser csvparser = new CSVParser(fileStream);

        String[] values;
        while ((values = csvparser.getLine()) != null) {
            float density = Float.parseFloat(values[2]);
            float speed = Float.parseFloat(values[0]);
            float occ = Float.parseFloat(values[1]);
            boolean isIncident = Boolean.parseBoolean(values[3]);

            // 1: Count, 2: Presence, 3: Speed, 4: Occupied Time Percentage, 5: Headway, 6: Density, 7: EquippedVehicle
            float[] featurevector = {0, 0, speed, occ, 0, density, 0};

            list.add(new KNNDetectorDataValue(csvparser.getLastLineNumber(), featurevector, isIncident));
        }

        csvparser.close();
        return list;
    }
}
