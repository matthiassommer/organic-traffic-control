package de.dfg.oc.otc.layer2;

import de.dfg.oc.otc.layer1.controller.AbstractTLCSelector;
import de.dfg.oc.otc.layer2.ea.EAServerInterface;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;

import java.rmi.RemoteException;

/**
 * Implements the {@link de.dfg.oc.otc.layer2.OTCLayer2AnnounceInterface}.
 *
 * @author rochner
 */
public class OTCLayer2Announce implements OTCLayer2AnnounceInterface {
    /**
     * Returns the result of the optimisation process at Layer 2 to the controller at Layer 1.
     *
     * @param result new TLC parameter set
     */
    private void addResultToLCS(final OptimisationResult result) {
        final OTCManager manager = OTCManager.getInstance();

        final AimsunNetwork network = manager.getNetwork();
        if (network != null) {
            final OTCNode node = network.getNode(result.getNodeID());

            if (node != null) {
                final AbstractTLCSelector junctionsLCS = node.getTLCSelector();
                if (junctionsLCS != null) {
                    junctionsLCS.addOptimisationResult(result);
                } else {
                    manager.newWarning("LCS for junction " + result.getNodeID() + " could not be obtained.");
                }
            } else {
                manager.newWarning("OTCNode " + result.getNodeID() + " could not be obtained.");
            }
        } else {
            manager.newWarning("Network could not be obtained.");
        }
    }

    @Override
    public final int announce(final EAServerInterface serverInterface) throws RemoteException {
        final OTCManager manager = OTCManager.getInstance();
        manager.setLayer2Present(true);
        return manager.addEA(serverInterface);
    }

    @Override
    public final String getFilenamePrefix() throws RemoteException {
        return OTCManager.getInstance().getFilenamePrefix();
    }

    @Override
    public final float getSimTime() {
        return OTCManager.getInstance().getTime();
    }

    @Override
    public final void pushResult(final int eaId, final OptimisationResult result) throws RemoteException {
        addResultToLCS(result);

        final OTCManager manager = OTCManager.getInstance();
        manager.getEaList().get(eaId - 1).setEaReady(true);
        manager.checkEAStatus();
        manager.checkLayer2Busy();
    }
}
