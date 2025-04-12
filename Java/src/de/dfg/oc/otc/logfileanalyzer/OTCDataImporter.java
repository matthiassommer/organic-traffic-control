package de.dfg.oc.otc.logfileanalyzer;

import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * GUI for graphical analyzing LOG-Files.
 */
public class OTCDataImporter implements DataImporterInterface {
    private String lastline;

    public final DataElement getNextDataElement(final BufferedReader bR) {
        if (bR == null) {
           throw new IllegalArgumentException("BufferedReader is null");
        }

        DataElement emptyElement = new DataElement();

        try {
            if (lastline == null) {
                lastline = bR.readLine();
            }
            if (lastline == null) {
                return null;
            }

            boolean loop = true;
            DefaultTableModel population = emptyElement.getPopulation();
            DefaultTableModel matchSet = emptyElement.getMatchSet();
            DefaultTableModel actionSet = emptyElement.getActionSet();
            int status = 0;

            while (loop) {
                if (status == 0) {
                    if (lastline.contains("* SimTime ")) {

                        int helperIndex = lastline.indexOf(":");
                        if (helperIndex != -1) {
                            lastline = lastline.substring(helperIndex + 1);
                        }

                        String[] andEverything = lastline.split(",");
                        String iterationText = andEverything[0];
                        iterationText = iterationText.substring(10);
                        double iteration = Double.parseDouble(iterationText);
                        emptyElement.setIteration(iteration);

                        andEverything = lastline.split(", situation");
                        andEverything = andEverything[1].split(", cycle");
                        emptyElement.setInput(andEverything[0]);

                        status = 1;
                        lastline = bR.readLine();
                    }
                }

                if (status == 1) {
                    if (!lastline.contains("* ")) {
                        population.addRow(stringToRow(lastline));
                    }
                    if (lastline.contains("* MatchSet")) {
                        status = 2;
                    }
                    if (lastline.contains("* ActionSet")) {
                        status = 3;
                    }
                }
                if (status == 2) {
                    if (!lastline.contains("* ")) {
                        matchSet.addRow(stringToRow(lastline));
                    }
                    if (lastline.contains("* ActionSet")) {
                        status = 3;
                    }
                }
                if (status == 3) {
                    if (!lastline.contains("* ")) {
                        actionSet.addRow(stringToRow(lastline));
                    }
                }

                if (lastline.contains("* SimTime ")) {
                    loop = false;
                } else {
                    lastline = bR.readLine();
                }
                if (lastline == null) {
                    loop = false;
                }
            }

            if (status == 0) {
                emptyElement = null;
            } else {
                emptyElement.setPopulation(population);
                emptyElement.setMatchSet(matchSet);
                emptyElement.setActionSet(actionSet);
            }
        } catch (IOException e) {
            System.err.println(Arrays.toString(e.getStackTrace()));
        }
        return emptyElement;
    }

    /**
     * Spaltet den String einer eingelesenen Zeile auf und gibt die
     * Informationen f�r die Tabelle als String-Feld aus.
     *
     * @param text String der eingelesenen Zeile
     * @return String-Feld, die die Werte aufgespaltet zur�ckgibt
     */
    private String[] stringToRow(final String text) {
        if (text.equals("NO CLASSIFIERS AVAILABLE")) {
            String[] values = new String[11];
            values[0] = text;
            return values;
        }

        String[] infos = text.split(" --- ");
        String[] idCreated = infos[0].split(" ");
        String[] rest = infos[3].split(",");

        String id = idCreated[0];
        String createdBy = idCreated[1];
        String condition = infos[1];
        String action = infos[2];
        String currentPrediction = rest[0];
        String initialPrediction = rest[1];
        String predictionError = rest[2];
        String fitness = rest[3];
        String actionSetSize = rest[4];
        String numerosity = rest[5];
        String experience = rest[6];

        String[] werte = new String[11];

        werte[0] = id.trim();
        werte[1] = createdBy.trim();
        werte[2] = condition.trim();
        werte[3] = action.trim();
        werte[4] = currentPrediction.trim();
        werte[5] = initialPrediction.trim();
        werte[6] = predictionError.trim();
        werte[7] = fitness.trim();
        werte[8] = actionSetSize.trim();
        werte[9] = numerosity.trim();
        werte[10] = experience.trim();

        return werte;
    }
}
