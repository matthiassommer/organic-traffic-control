package tests.layer2;

import java.rmi.Remote;
import java.rmi.RemoteException;

@FunctionalInterface
interface RemoteFileReceiverInterface extends Remote {
    /**
     * Receive a file from a remote source.
     *
     * @param packet A {@code RemoteFilePacket} to be received
     * @throws RemoteException if there are RMI-related problems
     */
    void receiveFile(RemoteFilePacket packet) throws RemoteException;
}
