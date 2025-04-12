package de.dfg.oc.otc.tools;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dominik on 24.03.2015.
 * <p>
 * Takes the average delay time of a replication from an exported Aimsun time series text file
 * and converts it to .csv for further processing.
 *
 * @author rauhdomi
 */
public class AimsunTimeSeriesToCSVConverter {
    private static final String IN_PATH = "C:\\Users\\Dominik\\Desktop\\reference.txt";
    private static final String OUT_PATH = "C:\\Users\\Dominik\\Desktop\\reference.csv";

    private static final String CSV_HEADER = "time;avgDelay;misteryvalue";

    public void convert(String inPath, String outPath) {
        List<String> aimsunText = parseAimSunTimeSeries(inPath);
        List<String> csvText = transformToCSV(aimsunText);
        writeCSV(outPath, csvText);
    }

    private List<String> parseAimSunTimeSeries(String inPath) {
        List<String> aimsunText = null;

        try (InputStream iStream = new FileInputStream(new File(inPath))) {
            aimsunText = IOUtils.readLines(iStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aimsunText;
    }

    private List<String> transformToCSV(List<String> aimsunText) {
        List<String> csvText = new ArrayList<>();
        csvText.add(CSV_HEADER);

        //Skip header and footer
        aimsunText.remove(0);
        aimsunText.remove(aimsunText.size() - 1);

        for (String aimsunLine : aimsunText) {
            String csvLine = extractValues(aimsunLine);
            csvText.add(csvLine);
        }

        return csvText;
    }

    private String extractValues(String line) {
        String csvLine = line.replaceAll("[\\[\\](){}]", "");
        csvLine = csvLine.replaceAll("\\s+", ";");

        return csvLine;
    }

    private void writeCSV(String outPath, List<String> csvText) {
        try (OutputStream oS = new FileOutputStream(new File(outPath))) {
            IOUtils.writeLines(csvText, System.lineSeparator(), oS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        AimsunTimeSeriesToCSVConverter converter = new AimsunTimeSeriesToCSVConverter();

        converter.convert(IN_PATH, OUT_PATH);
    }
}
