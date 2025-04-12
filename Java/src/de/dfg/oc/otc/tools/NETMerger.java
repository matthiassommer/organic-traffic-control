package de.dfg.oc.otc.tools;

import java.io.*;
import java.util.Arrays;

/**
 * Dieses Programm f�hrt zwei Ergebnisdateien von Teilsimulationen zu einer
 * Datei zusammen.
 *
 * @author MaHi / hpr
 */
class NETMerger {
    private class FileListFilter implements FilenameFilter {
        private final String extension;
        private final String name;

        FileListFilter(final String name, final String extension) {
            this.name = name;
            this.extension = extension;
        }

        public boolean accept(final File directory, final String filename) {
            boolean fileOK = true;

            fileOK &= filename.contains(name);
            fileOK &= filename.endsWith('.' + extension);

            return fileOK;
        }
    }

    private static final String[] fileTypes = {"_Performance_NET",
            "_Performance_NET_ABS", "_Fuel_NET", "_Fuel-km_NET",
            "_Pollution_NET", "_Stream"};
    /**
     * Namen der Unterordner.
     */
    private static final String part1 = "MORNING";
    private static final String part2 = "AFTERNOON";

    public static void main(final String[] args) {
        final NETMerger merger = new NETMerger();

        for (String fileType : fileTypes) {
            // Schritt 1: Files Holen

            // Ordner bestimmen
            final String part1Path = System.getProperty("user.dir") + "/" + part1;
            final String part2Path = System.getProperty("user.dir") + "/" + part2;

            final File[] part1Files = merger.getFiles(part1Path, fileType);
            final File[] part2Files = merger.getFiles(part2Path, fileType);

            // Schritt 2: Files kombinieren
            for (File part1File : part1Files) {
                // Suchen des passenden part2Files durch vergleichen des part2
                // Namens mit dem part1RelevantName und zusammenf�gen der
                // Dateien
                for (File part2File : part2Files) {
                    if (part2File.getName().startsWith(
                            part1File.getName())) {
                        merger.putTogether(part1File, part2File);
                    }
                }
            }
        }
    }

    /**
     * Methode sucht alle Performance Dateien im gleichen Ordner, anhand der
     * Kriterien: contains "_Performance_" und endsWith ".csv".
     *
     * @param path Pfad in der die MainMethode liegt
     * @param str  Namensteil zur Dateiindentifikation
     * @return Sortieres Array mit allen Files
     */
    private File[] getFiles(final String path, final String str) {
        final FilenameFilter flf = new FileListFilter(str, "csv");

        final File dir = new File(path);
        final File[] allFiles = dir.listFiles(flf);

        Arrays.sort(allFiles);
        return allFiles;
    }

    /**
     * Methode f�gt die Teilsimulationen zusammen.
     *
     * @param morning   erste Teilsimulation
     * @param afternoon zweite Teilsimulation
     */
    private void putTogether(final File morning, final File afternoon) {
        final File result = new File(System.getProperty("user.dir") + "/RESULT/"
                + morning.getName());

        // Verzeichnis und Datei erstellen
        if (!result.exists()) {
            final File folder = new File(System.getProperty("user.dir") + "/RESULT");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            try {
                result.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            // Lesen
            final BufferedReader bufferedReader = new BufferedReader(new FileReader(
                    morning.getAbsolutePath()));
            final BufferedReader bufferedReader1 = new BufferedReader(new FileReader(
                    afternoon.getAbsolutePath()));

            final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                    result.getAbsolutePath()));

            String currentLine = bufferedReader.readLine();

            // String wird ben�tigt um die letzte Simulationssekunde zu erhalten
            String lastLine = currentLine;

            // erste Teilsimulation �bertragen
            while (currentLine != null) {
                bufferedWriter.write(currentLine);
                bufferedWriter.newLine();
                lastLine = currentLine;

                currentLine = bufferedReader.readLine();
            }

            // Relevanten Teil der zweiten Teilsimulation �bertragen
            final String endOfFirst = lastLine.split("; ")[0];
            bufferedReader.close();
            currentLine = bufferedReader1.readLine();

            while (currentLine != null && !currentLine.startsWith(endOfFirst)) {
                currentLine = bufferedReader1.readLine();
            }

            // Letzte doppelte Zeile �berspingen
            currentLine = bufferedReader1.readLine();

            while (currentLine != null) {
                bufferedWriter.write(currentLine);
                bufferedWriter.newLine();
                currentLine = bufferedReader1.readLine();
            }

            bufferedWriter.close();
            bufferedReader1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
