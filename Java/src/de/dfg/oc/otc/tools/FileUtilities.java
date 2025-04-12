package de.dfg.oc.otc.tools;

import java.io.*;

public abstract class FileUtilities {
    /**
     * Kopiert eine Datei.
     *
     * @param src   Quelldatei
     * @param dest  Zieldatei
     * @param force Falls true, wird eine eventuell bestehende Datei
     *              �berschrieben.
     * @throws IOException
     */
    static void copyFile(final File src, final File dest, final boolean force) throws IOException {
        final int bufSize = 500;
        if (dest.exists()) {
            if (force) {
                dest.delete();
            } else {
                throw new IOException("Cannot overwrite existing file: " + dest);
            }
        }

        final byte[] buffer = new byte[bufSize];
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            while (true) {
                final int read = in.read(buffer);
                if (read == -1) {
                    // -1 bedeutet EOF
                    break;
                }
                out.write(buffer, 0, read);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
        }
    }

    public static void createNewFile(String filename) {
        try {
            File file = new File(filename);

            // create folder
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Couldn't create folder: " + parent.getAbsolutePath());
            }

            if (file.exists()) {
                file.delete();
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Schreibt den �bergebenen Text {@code t} in die Datei {@code f}.
     *
     * @param f      ein Datei
     * @param t      ein Text
     * @param append �berschreiben oder Anf�gen?
     */
    public static void writeToFile(final File f, final String t, final boolean append) {
        try {
            final FileWriter fstream = new FileWriter(f, append);
            final BufferedWriter out = new BufferedWriter(fstream);
            out.write(t + System.getProperty("line.separator"));
            out.close();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
