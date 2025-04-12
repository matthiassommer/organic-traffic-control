package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.layer2.OptimisationResult;
import de.dfg.oc.otc.layer2.OptimisationTask;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * An interface which all evolutionary algorithms on Layer 2 have to implement.
 */
public interface EAServerInterface extends Remote {
    void addTask(OptimisationTask task) throws RemoteException;

    int registerEAAtLayer1(EA ea) throws RemoteException;

    /**
     * Calls a RMI method to communicate the results to Layer 1.
     *
     * @param result the {@code OptimisationResult}
     * @throws RemoteException
     */
    void returnOptimisationResult(OptimisationResult result) throws RemoteException;
}