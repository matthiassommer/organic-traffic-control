package tests.layer2;

import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer2.OTCLayer2Announce;
import de.dfg.oc.otc.layer2.OptimisationTask;
import de.dfg.oc.otc.layer2.TurningData;
import de.dfg.oc.otc.layer2.ea.EAConfig;
import de.dfg.oc.otc.manager.OTCManager;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

/**
 * EA-Parameterstudie.
 *
 * @author hpr
 */
class Sensa {
    // public final static File FILE = new File("K7_LSBG_160.ang");
    // public final static int NODE_ID = 160;
    // public final static int RID = 676;
    private static final double[] CROSS_PROB = {1.0, .7, .4, .1};
    private static final File FILE = new File("./angNodeFiles/K3_LSBG_131.ang");
    private static final double[] MUTATION_PROB = {1.0, .7, .4, .1};
    private static final int NODE_ID = 131;
    // Experimental setup
    private static final int NUM_EXP = 10;
    private static final int RID = 240;
    private Registry rmiRegistry;

    @Test
    public void run() {
        Sensa s = new Sensa();
        s.startReg();

        // K7
        // Low traffic (5h00-6h00 hourly)
        // float[] situation = {144, 326, 135, 30, 209, 186};
        // Medium traffic (10h00-1h00 hourly)
        // float[] situation = { 777, 1167, 281, 274, 563, 550 };
        // Spitzenstunde vorm.
        // float[] situation = { 830, 1800, 530, 190, 740, 610 };
        // int[] sectionIdsForSit = { 155, 115, 682, 159, 682, 149, 206, 207,
        // 272, 159, 211, 212 };

        // HashMap<String, TurningData> turnData = s.turnDataK7(situation);

        // K3
        float[] situation = {50, 540, 1230, 130, 990, 990 + 260, 1320, 1170, 120, 140, 1390, 260, 540 + 120};
        int[] sectionIdsForSit = {130, 127, 130, 529, 130, 246, 122, 123, 122, 534, 534, 129, 126, 128, 125, 127, 125,
                529, 120, 121, 120, 123, 120, 534, 529, 121};

        HashMap<String, TurningData> turnData = s.turnDataK3(situation);

        // Mutation probability
        for (double aMUTATION_PROB : MUTATION_PROB) {
            // Crossover probability
            for (double aCROSS_PROB : CROSS_PROB) {
                // Create standard configuration
                EAConfig eaConf = new EAConfig();
                eaConf.setMutationProb(aMUTATION_PROB);
                eaConf.setCrossOverProb(aCROSS_PROB);

                // Perform repeated optimisations
                for (int k = 0; k < NUM_EXP; k++) {
                    try {
                        OptimisationTask optTask = new OptimisationTask(FILE, NODE_ID, 0, situation, sectionIdsForSit, RID, eaConf,
                                Attribute.LOS, 0);
                        optTask.setTurningData(turnData);
                        OTCManager.getInstance().addTask(optTask);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void connectRegistry() {
        try {
            rmiRegistry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        } catch (RemoteException e) {
            try {
                rmiRegistry = LocateRegistry.getRegistry();
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void registerRMIMethods() {
        Remote layer2Announce = new OTCLayer2Announce();
        try {
            Remote layer2AnnounceStub = UnicastRemoteObject
                    .exportObject(layer2Announce, 0);
            rmiRegistry.rebind("Layer2Announce", layer2AnnounceStub);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startReg() {
        connectRegistry();
        registerRMIMethods();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, TurningData> turnDataK3(final float[] situation) {
        HashMap<String, TurningData> turnData = new HashMap<>();

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

    private HashMap<String, TurningData> turnDataK7(final float[] situation) {
        HashMap<String, TurningData> turnData = new HashMap<>();

        TurningData td = new TurningData(272, 159, 3, situation[4]);
        td.addPhase(7);
        td.addPhase(8);
        td.addPhase(9);
        turnData.put("272;159", td);

        td = new TurningData(206, 207, 2, situation[3]);
        td.addPhase(5);
        td.addPhase(6);
        td.addPhase(7);
        td.addPhase(8);
        turnData.put("206;207", td);

        td = new TurningData(682, 149, 2, situation[2]);
        td.addPhase(4);
        td.addPhase(5);
        turnData.put("682;149", td);

        td = new TurningData(682, 159, 3, situation[1]);
        td.addPhase(2);
        td.addPhase(3);
        td.addPhase(4);
        td.addPhase(5);
        turnData.put("682;159", td);

        td = new TurningData(211, 212, 2, situation[5]);
        td.addPhase(1);
        td.addPhase(2);
        td.addPhase(8);
        td.addPhase(9);
        td.addPhase(10);
        turnData.put("211;212", td);

        td = new TurningData(155, 115, 3, situation[0]);
        td.addPhase(2);
        turnData.put("155;115", td);

        return turnData;
    }
}
