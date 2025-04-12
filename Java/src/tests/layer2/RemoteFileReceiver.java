package tests.layer2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

/**
 * A {@code RemoteFileReceiver} implements the
 * {@code RemoteFileReceiverInterface} to receive files via RMI.
 *
 * @author hpr
 */
class RemoteFileReceiver implements RemoteFileReceiverInterface {
    /**
     * Directory where the received file are saved If {@code null}, the
     * current directory (where JVM was started) is used.
     */
    private File dirToSaveTo;

    RemoteFileReceiver() {
        super();
    }

    /**
     * Receives a file via RMI and writes it to a file. The filename is
     * specified by the {@code RemoteFilePacket} object (with an additional
     * .snd), the directory is specified by {@code dirToSaveTo}.
     *
     * @param packet The packet to receive
     * @throws RemoteException
     */
    public final void receiveFile(final RemoteFilePacket packet) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(this.dirToSaveTo, packet.getName() + ".snt"));
            packet.writeTo(fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the directory where received files are saved. If set to
     * {@code null}, the current directory (where JVM was started) is used.
     *
     * @param directory the directory to place files in
     */
    final void setDirectory(final String directory) {
        this.dirToSaveTo = new File(directory);
    }
}
