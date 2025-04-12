package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.layer2.OptimisationTask;

import java.rmi.RemoteException;

/**
 * Serves as a connection interface between the server and a client.
 */
public class EAConnection extends Thread {
    private final EAServerInterface serverInterface;
    private boolean isReady = true;
    private OptimisationTask optimisationTask;

    public EAConnection(final EAServerInterface serverInterface) {
        this.serverInterface = serverInterface;
    }

    public final boolean isEaReady() {
        return isReady;
    }

    public final void setEaReady(final boolean ready) {
        this.isReady = ready;
    }

    @Override
    public final void run() {
        try {
            serverInterface.addTask(optimisationTask);
        } catch (RemoteException e) {
            System.err.println(e.getMessage());
        }
    }

    public final void setOptimisationTask(final OptimisationTask optimisationTask) {
        this.optimisationTask = optimisationTask;
    }
}
