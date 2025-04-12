package tests.layer2;

import java.io.*;

/**
 * A {@code RemoteFilePacket} represents a file that can be transfered via
 * RMI to be recreated on another system.
 */
/*
 * http://www.javaworld.com/javaworld/jw-05-1997/javadev/RMISendFile.html
 */
class RemoteFilePacket implements Serializable {
    private final File fileToBeTransfered;
    private byte[] fileData;

    /**
     * Create a {@code RemoteFilePacket}. The file specified by
     * {@code _filename} is read and stored.
     *
     * @param fileName name of the file represented by this
     *                 {@code RemoteFilePacket}
     */
    RemoteFilePacket(final String fileName) {
        this.fileToBeTransfered = new File(fileName);
        this.readIn();
    }

    /**
     * Get the name of the file respresented by this
     * {@code RemoteFilePacket}.
     *
     * @return fully qualified file name
     */
    final String getName() {
        return this.fileToBeTransfered.getName();
    }

    /**
     * Reads to data from the from the file specified by {@code filePath}
     * and {@code fileName} and stores it in {@code fileData}.
     */
    private void readIn() {
        try {
            this.fileData = new byte[(int) this.fileToBeTransfered.length()];
            new FileInputStream(fileToBeTransfered).read(this.fileData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the {@code fileData} to the given {@code OutputStream}.
     *
     * @param out The outputStream to write to
     */
    final void writeTo(final OutputStream out) {
        try {
            out.write(this.fileData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
