package de.dfg.oc.otc.aid.algorithms.apid;

import de.dfg.oc.otc.manager.OTCManager;

import java.util.*;

/**
 * Created by Dietmar on 03.05.2017.
 */
public class APIDParameterSelector {

    private static HashMap<Integer, APIDParameters> paramList = new HashMap<>();
    private static ArrayList<APIDParameters> paramListList = new ArrayList<>();
    private static ListIterator<APIDParameters> it;

    static {
        int startReplication = 10000819;

        try {
            boolean[] MEDIUM_TRAFFIC_DETECTION_ENABLED = {true};
            boolean[] COMPRESSION_WAVE_TEST_ENABLED = {false};
            boolean[] PERSISTENCE_TEST_ENABLED = {true};
            float[] TH_MEDIUM_TRAFFIC = {40, 80};
            float[] TH_INC_CLR = {-0.4f, -0.2f};
            float[] TH_PT = {0.4f, 0.1f};
            float[] TH_CW1 = {-1.3f};
            float[] TH_CW2 = {-1.5f};
            float[] PERSISTENCE_TEST_PERIOD = {50};
            float[] COMPRESSION_WAVE_TEST_PERIOD = {50};
            float[] TH_ID1 = {10.2f, 80f};
            float[] TH_ID2 = {0f, 0.8f};
            float[] TH_ID3 = {20.8f, 5f};
            float[] TH_MED_ID1 = {0.4f, 0.8f};
            float[] TH_MED_ID2 = {0.1f, 0f};

            for (int a = 0; a < TH_MEDIUM_TRAFFIC.length; a++)
                for (int b = 0; b < TH_INC_CLR.length; b++)
                    for (int c = 0; c < TH_PT.length; c++)
                        for (int d = 0; d < TH_CW1.length; d++)
                            for (int e = 0; e < TH_CW2.length; e++)
                                for (int f = 0; f < PERSISTENCE_TEST_PERIOD.length; f++)
                                    for (int g = 0; g < COMPRESSION_WAVE_TEST_PERIOD.length; g++)
                                        for (int h = 0; h < TH_ID1.length; h++)
                                            for (int i = 0; i < TH_ID2.length; i++)
                                                for (int j = 0; j < TH_ID3.length; j++)
                                                    for (int k = 0; k < TH_MED_ID1.length; k++)
                                                        for (int l = 0; l < TH_MED_ID2.length; l++)
                                                            for (int m = 0; m < MEDIUM_TRAFFIC_DETECTION_ENABLED.length; m++)
                                                                for (int n = 0; n < COMPRESSION_WAVE_TEST_ENABLED.length; n++)
                                                                    for (int o = 0; o < PERSISTENCE_TEST_ENABLED.length; o++)
                                                                    {
                                                                        APIDParameters p = new APIDParameters(MEDIUM_TRAFFIC_DETECTION_ENABLED[m], COMPRESSION_WAVE_TEST_ENABLED[n], PERSISTENCE_TEST_ENABLED[o], TH_MEDIUM_TRAFFIC[a],
                                                                                TH_INC_CLR[b], TH_PT[c], TH_CW1[d], TH_CW2[e], PERSISTENCE_TEST_PERIOD[f], COMPRESSION_WAVE_TEST_PERIOD[g],
                                                                                TH_ID1[h], TH_ID2[i], TH_ID3[j], TH_MED_ID1[k], TH_MED_ID2[l]);

                                                                        paramList.put(startReplication++, p);
                                                                        paramListList.add(p);
                                                                    }
            Collections.shuffle(paramListList, new Random(1));
            it = paramListList.listIterator();
            System.out.println("There are " + paramList.size() + " parameter configurations");
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }

    public static void setParams(APIDAlgorithm apid, int idx)
    {
        APIDParameters p = paramListList.get(idx);
        p.setParameters(apid);
        System.out.println("APID parameters are: " + p.toString());
    }

    public static void setNextParams(APIDAlgorithm apid)
    {
        if (it.hasNext())
        {
            APIDParameters p = it.next();
            p.setParameters(apid);
            System.out.println("APID parameters are: " + p.toString());
        }
        else
        {
            System.out.println("ERROR: HAD TO RESET ITERATOR");
            it = paramListList.listIterator();
            APIDParameters p = it.next();
            p.setParameters(apid);
            System.out.println("APID parameters are: " + p.toString());
        }
    }

    public static void setParams(APIDAlgorithm apid)
    {
        APIDParameters p = paramList.get(OTCManager.getInstance().getReplicationID());
        if (p != null) {
            p.setParameters(apid);
            System.out.println("APID parameters are: " + p.toString());
        }
        else
        {
            System.out.println("ERROR: COULD NOT SET PARAMETERS");
        }
    }

}
