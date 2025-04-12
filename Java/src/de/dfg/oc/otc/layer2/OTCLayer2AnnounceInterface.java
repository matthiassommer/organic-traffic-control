package de.dfg.oc.otc.layer2;

import de.dfg.oc.otc.layer2.ea.EAServerInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This interface defines methods provided by Layer 0/1 that can be called by
 * Layer 2 (via RMI).
 *
 * @author rochner
 */
public interface OTCLayer2AnnounceInterface extends Remote {
    /**
     * Informs Layer 0/1 that Layer 2 is present.
     *
     * @param serverInterface an interface needed for sending optimization tasks to Layer 2
     * @return return code
     * @throws RemoteException in case of remote errors
     */
    int announce(EAServerInterface serverInterface) throws RemoteException;

    /**
     * Returns the file name prefix for log files.
     *
     * @return the file name prefix for log files
     * @throws RemoteException in case of remote errors
     */
    String getFilenamePrefix() throws RemoteException;

    /**
     * Returns the current simulation time of Layer 0/1.
     *
     * @return the current simulation time of Layer 0/1
     * @throws RemoteException in case of remote errors
     */
    float getSimTime() throws RemoteException;

    /**
     * Collects an optimization result from Layer 2 and add a new rule to the
     * LCS.
     *
     * @param eaId   id of the evolutionary algorithm
     * @param result an optimization result
     * @throws RemoteException in case of remote errors
     */
    void pushResult(int eaId, OptimisationResult result) throws RemoteException;
}
