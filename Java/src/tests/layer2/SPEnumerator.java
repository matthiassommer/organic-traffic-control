package tests.layer2;

import de.dfg.oc.otc.layer2.TurningData;
import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Vollständige Enumeration.
 *
 * @author hpr
 */
public class SPEnumerator {
    @Test
    public void run() {
        SPEnumerator spe = new SPEnumerator();

        for (int i = 5; i < 60; i++) {
            for (int j = 5; j < 60; j++) {
                for (int k = 3; k < 60; k++) {
                    for (int l = 5; l < 60; l++) {
                        int[] pheno = new int[4];
                        pheno[0] = i;
                        pheno[1] = j;
                        pheno[2] = k;
                        pheno[3] = l;

                        if (spe.getCycleTime(pheno) < 121) {
                            spe.websterAvgDelay(pheno);
                        }
                    }
                }
            }
        }
    }

    private final int[] referencePhases = {2, 1, 4, 3, 33, 3, 1, 2, 1, 1, 5, 9, 4, 3, 7, 9, 2};

    private final boolean[] referencePhaseTypes = {true, true, true, true, false, true, true, true, true, true, true, false,
            true, false, true, false, true};
    private final float[] situation = {50, 540, 1230, 130, 990, 990 + 260, 1320, 1170, 120, 140, 1390, 260, 540 + 120};
    // int[] sectionIdsForSit = { 130, 127, 130, 529, 130, 246, 122, 123, 122,
    // 534, 534, 129, 126, 128, 125, 127, 125, 529, 120, 121, 120, 123, 120,
    // 534, 529, 121 };

    private float totalDemand = -1;

    /**
     * @param phenoType
     * @return
     */
    private double getCycleTime(final int[] phenoType) {
        // Interphases!
        int ct = 36;
        for (int aPhenoType : phenoType) {
            ct += aPhenoType;
        }
        return ct;
    }

    /**
     * @return
     */
    private float getTotalDemand() {
        if (totalDemand == -1) {
            float td = 0;
            for (float aSituation : situation) {
                td += aSituation;
            }
            totalDemand = td;
        }

        return totalDemand;
    }

    /**
     * @param i
     * @return
     */
    private int mapPhaseIdToGeneId(final int i) {
        // {4=0, 11=1, 13=2, 15=3}
        switch (i) {
            case 4:
                return 0;
            case 11:
                return 1;
            case 13:
                return 2;
            case 15:
                return 3;
            default:
                System.out.println("ERROR");
                return -1;
        }
    }

    /**
     * @param situation
     * @return
     */
    private Map<String, TurningData> turnDataK3(final float[] situation) {
        Map<String, TurningData> turnData = new HashMap<>();

        // 125;529=125 -> 529, SHARED false, #LANES 1.0, PHASES [4, 5, 6]
        TurningData td = new TurningData(125, 529, 1, situation[8]);
        td.addPhase(4);
        td.addPhase(5);
        td.addPhase(6);
        turnData.put("125;529", td);

        // 125;127=125 -> 127, SHARED false, #LANES 3.0, PHASES [4, 5, 6]
        td = new TurningData(125, 127, 3, situation[7]);
        td.addPhase(4);
        td.addPhase(5);
        td.addPhase(6);
        turnData.put("125;127", td);

        // 529;121=529 -> 121, SHARED false, #LANES 2.0, PHASES [1, 2, 9, 10,
        // 11, 12, 13, 14, 15, 16, 17]
        td = new TurningData(529, 121, 2, situation[12]);
        td.addPhase(1);
        td.addPhase(2);
        td.addPhase(9);
        td.addPhase(10);
        td.addPhase(11);
        td.addPhase(12);
        td.addPhase(13);
        td.addPhase(14);
        td.addPhase(15);
        td.addPhase(16);
        td.addPhase(17);
        turnData.put("529;121", td);

        // 120;534=120 -> 534, SHARED false, #LANES 1.0, PHASES [5]
        td = new TurningData(120, 534, 1, situation[11]);
        td.addPhase(5);
        turnData.put("120;534", td);

        // 130;246=130 -> 246, SHARED false, #LANES 3.0, PHASES [1, 14, 15, 16,
        // 17]
        td = new TurningData(130, 246, 3, situation[2]);
        td.addPhase(1);
        td.addPhase(14);
        td.addPhase(15);
        td.addPhase(16);
        td.addPhase(17);
        turnData.put("130;246", td);

        // 122;534=122 -> 534, SHARED false, #LANES 3.0, PHASES [10, 11, 12]
        td = new TurningData(122, 534, 3, situation[4]);
        td.addPhase(10);
        td.addPhase(11);
        td.addPhase(12);
        turnData.put("122;534", td);

        // 130;127=130 -> 127, SHARED true, #LANES 2.0, PHASES [12, 13, 14, 15,
        // 16]
        td = new TurningData(130, 127, 2, situation[0] + situation[1]);
        td.addPhase(12);
        td.addPhase(13);
        td.addPhase(14);
        td.addPhase(15);
        td.addPhase(16);
        td.setShared(true);
        turnData.put("130;127", td);

        // 534;129=534 -> 129, SHARED false, #LANES 3.0, PHASES [11, 12, 13,
        // 14]
        td = new TurningData(534, 129, 3, situation[5]);
        td.addPhase(11);
        td.addPhase(12);
        td.addPhase(13);
        td.addPhase(14);
        turnData.put("534;129", td);

        // 120;121=120 -> 121, SHARED true, #LANES 3.0, PHASES [5]
        td = new TurningData(120, 121, 3, situation[9] + situation[10]);
        td.addPhase(5);
        td.setShared(true);
        turnData.put("120;121", td);

        // 126;128=126 -> 128, SHARED false, #LANES 2.0, PHASES [1, 2, 3, 4, 5,
        // 6, 7, 16, 17]
        td = new TurningData(126, 128, 2, situation[6]);
        td.addPhase(1);
        td.addPhase(2);
        td.addPhase(3);
        td.addPhase(4);
        td.addPhase(5);
        td.addPhase(6);
        td.addPhase(7);
        td.addPhase(16);
        td.addPhase(17);
        turnData.put("126;128", td);

        // 122;123=122 -> 123, SHARED false, #LANES 1.0, PHASES [10, 11, 12]
        td = new TurningData(122, 123, 1, situation[3]);
        td.addPhase(10);
        td.addPhase(11);
        td.addPhase(12);
        turnData.put("122;123", td);

        return turnData;
    }

    /**
     * Calculates the average delay at an intersection based on Webster's
     * formula for the average delay in the approaches. See [Web58, SL97].
     *
     * @param phenoType
     */
    private void websterAvgDelay(final int[] phenoType) {
        double tw = 0;

        Map<String, TurningData> tdMap = turnDataK3(situation);

        Collection<TurningData> turnDataCollection = tdMap.values();

        // Determine total demand at intersection
        float totalDemand = getTotalDemand();

        for (TurningData td : turnDataCollection) {
            // Green time for this turning
            float tf = 0;

            for (Integer phaseId : td.getPhases()) {
                if (referencePhaseTypes[phaseId - 1]) {
                    // Interphase, get duration from reference!
                    tf += referencePhases[phaseId - 1];
                } else {
                    // Non-interphase, get duration from individual!
                    tf += phenoType[mapPhaseIdToGeneId(phaseId - 1)];
                }
            }

            tw += td.getFlow() / totalDemand
                    * websterAvgDelayPerTurn(td.getFlow(), tf, getCycleTime(phenoType), td.getNumberOfLanes());
        }

        try {
            // Create file
            FileWriter fstream = new FileWriter("out.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.append(Arrays.toString(phenoType)).append(";" + tw).append(";\n");
            out.close();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Calculates the average waiting time for a turning according to Webster's
     * formula.
     *
     * @param M     current flow (veh/h)
     * @param tf    effective green time for turning
     * @param tu    cycle time
     * @param lanes number of available lanes for the turning
     * @return average waiting time (in sec)
     */
    private double websterAvgDelayPerTurn(double M, final double tf, final double tu, final int lanes) {
        // Avoid division by zero
        if (M == 0) {
            M = 1;
        }

        // Saturation flow
        final double S = lanes * 1800;
        // Anteil der effektiven Gr�nzeit am Umlauf
        final double f = tf / tu;
        // S�ttigungsgrad
        double g = M / (f * S);

        // Prerequisite: g < 1
        if (g > 1) {
            g = .99;
        }

        // avg. delay
        final double tw = .9 * (tu * FastMath.pow(1 - f, 2) / (2 * (1 - M / S)) + 1800 * FastMath.pow(g, 2) / (M * (1 - g)));
        return tw;
    }
}
