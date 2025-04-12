package de.dfg.oc.otc.aid;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Dietmar on 24.04.2017.
 * This class holds information about the state of policies in Aimsun (active/inactive)
 */
public class AimsunPolicyStatus {

    /**
     * Maps a list of policy IDs to a detector pair ID.
     * Allows differentiating incidents for different road segments.
     */
    private static ConcurrentHashMap<String, LinkedHashSet<Integer>> pairIDPolicyIDs = new ConcurrentHashMap<>();

    /**
     * Represents the current status of a policy. Is refreshed at the beginning of every update cycle
     */
    private static ConcurrentHashMap<Integer, Boolean> policyActive = new ConcurrentHashMap<>();

    /**
     * Holds a simple listing of all policies whose status is queried by the API
     */
    private static LinkedHashSet<Integer> policyList = new LinkedHashSet<>();

    /**
     * Iterator that internally saves the next policy to query
     * Is reset every update cycle
     */
    private static Iterator policyListIterator = policyList.iterator();


    /**
     * Modify this static code according to your needs.
     * In this case, some policies relevant for both of these two detector pairs are added.
     * More sophisticated assignments are possible.
     */
    static {
        String[] detectorPairs = {"977978", "979980"};
        Integer[] relevantPolicies =   {10000733, 10000737, 10000740, 10000743, 10000746, 10000749, 10000752, 10000755, 10000758, 10000761, 10000764, 10000767,
                                        10000770, 10000773, 10000776, 10000779, 10000782, 10000785, // Parameterstudie

                                        10000789, 10000793, 10000796, 10000799, 10000802, 10000805, 10000808, 10000812, 10002869, 10002872, 10002875, 10002878,     // Auswertung

                                        10002656, 10002659, 10002662, 10002665, 10002668, 10002671, 10002674, 10002678, 10002682, 10002685, 10002688};    // Billsted classic

        for (String pairID : detectorPairs) {
            AimsunPolicyStatus.addDetectorPolicy(pairID, relevantPolicies);
        }
    }

    /**
     * Use this method to add policy IDs to be queried.
     * Saves which detector pair their status is relevant for.
     * @param detectorPairID E.g. "979980"
     * @param policyIDs E.g. {10000733, 10000737, 10000740}
     */
    public static void addDetectorPolicy(String detectorPairID, Integer[] policyIDs)
    {
        pairIDPolicyIDs.putIfAbsent(detectorPairID, new LinkedHashSet<>());
        pairIDPolicyIDs.get(detectorPairID).addAll(Arrays.asList(policyIDs));
        policyList.addAll(Arrays.asList(policyIDs));
        policyListIterator = policyList.iterator();
    }

    /**
     * Use this method in your own code to query the current status of policies relevant for your detector pair.
     * Before using this, add policies using addDetectorPolicy()
     * @param detectorPairID detector pair ID
     * @return true if any relevant policy for detectorPairID is active
     */
    public static boolean isAnyPolicyActive(String detectorPairID)
    {
        try {
            // for each policy relevant to this detector pair ID check if it is active
            LinkedHashSet<Integer> pairIDs = pairIDPolicyIDs.get(detectorPairID);
            if (pairIDs == null)
                return false;
            Iterator<Integer> it = policyList.iterator();
            while (it.hasNext()) {
                if (policyActive.get(it.next())) {
                    return true;
                }
            }
            return false;
        }
        catch (Exception e)
        {
            System.out.println(e);
            return false;
        }
    }

    /**
     * This method is to be exclusively used by OTCManager#getPolicyToQuery()
     * @return Each policy to be queried one by one, terminating with -1 (CRUCIAL!)
     */
    public static Integer getNextPolicy()
    {
        if (policyListIterator.hasNext())
        {
            return (Integer) policyListIterator.next();
        }
        else
        {
            policyListIterator = policyList.iterator();
            return -1;
        }
    }

    /**
     * This method is to be exclusively used by OTCManager#setPolicyStatus()
     * @param policyID policy ID
     * @param status current status (true=active)
     */
    public static void setStatus(Integer policyID, Boolean status)
    {
        policyActive.put(policyID, status);
    }

}
