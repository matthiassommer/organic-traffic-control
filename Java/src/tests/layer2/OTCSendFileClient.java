package tests.layer2;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * This client can be used to testWithUpper file transfers via RMI.
 *
 * @author hpr
 */
abstract class OTCSendFileClient {
    public static void main(final String[] args) throws Exception {
        Registry registry = LocateRegistry.getRegistry();
        RemoteFileReceiverInterface fileRecv = (RemoteFileReceiverInterface) registry.lookup("RemoteFileReceiver");

        if (args.length != 1) {
            System.out.println("usage: OTCSendFileClient <file_to_send>");
        } else {
            RemoteFilePacket rfp = new RemoteFilePacket(args[0]);
            fileRecv.receiveFile(rfp);
        }
    }
}
