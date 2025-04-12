package de.dfg.oc.otc.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Dieses Programm f�hrt zwei Ergebnisdateien von Teilsimulationen zu einer
 * Datei zusammen.
 * 
 * @author MaHi
 * 
 */
abstract class MergingMain {
	/** Namen der Unterordner. */
	private static final String part1 = "MORNING";
	private static final String part2 = "AFTERNOON";
	/** Aufw�rmzeit, setzt den Zeitraum, der nicht ben�tigt wird. */
	private static final int warmupTime = 1800;

	/**
	 * Methode sucht alle Performance Dateien im gleichen Ordner, anhand der
	 * Kriterien: contains "_Performance_" und endsWith ".csv".
	 * 
	 * @param path
	 *            Pfad in der die MainMethode liegt
	 * @return Sortieres Array mit allen Files
	 */
	private static File[] getFiles(final String path) {
		final FilenameFilter filter = (dir, name) -> name.contains("_Performance_") && name.endsWith(".csv");

		final File dir = new File(path);
		final File[] allFiles = dir.listFiles(filter);

		Arrays.sort(allFiles);
		return allFiles;
	}

	public static void main(final String[] args) {
		// Schritt 1: Files Holen

		// Ordner bestimmen
		final String part1Path = System.getProperty("user.dir") + "/" + part1;
		final String part2Path = System.getProperty("user.dir") + "/" + part2;

		final File[] part1Files = getFiles(part1Path);
		final File[] part2Files = getFiles(part2Path);

		// Schritt 2: Files kombinieren
        for (File part1File : part1Files) {
            // Zusammenpassenden part1 und part 2 aufgrund des Namens finden.
            // Hierf�r Zerlegung des Namens und Suchen der relevanten Teile
            // n�tig
            final String[] part1FilesNameArray = part1File.getName().split("_");

            // Gibt die L�nge des relevanten Namens an
            int length = 0;
            for (int j = 0; j < part1FilesNameArray.length; j++) {
                if (part1FilesNameArray[j].startsWith("20") && part1FilesNameArray[j].length() == 8) {
                    length = j;
                }
            }

            // relevanten Namen zusammenbauen
            String part1RelevantName = "";
            for (int j = 0; j < length; j++) {
                part1RelevantName += part1FilesNameArray[j] + "_";
            }

            // Suchen des passenden part2Files durch vergleichen des part2
            // Namens mit dem part1RelevantName und zusammenf�gen der Dateien
            for (File part2File : part2Files) {
                if (part2File.getName().startsWith(part1RelevantName)) {
                    putTogether(part1File, part2File);
                }
            }
        }
	}

	/**
	 * Methode f�gt die Teilsimulationen zusammen.
	 * 
	 * @param morning
	 *            erste Teilsimulation
	 * @param afternoon
	 *            zweite Teilsimulation
	 */
	private static void putTogether(final File morning, final File afternoon) {
		final File result = new File(System.getProperty("user.dir") + "/RESULT/" + morning.getName().split("_")[3]
				+ "_Performance_" + morning.getName().split("_")[7]);

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
			final BufferedReader bufferedReader = new BufferedReader(new FileReader(morning.getAbsolutePath()));
			final BufferedReader bufferedReader1 = new BufferedReader(new FileReader(afternoon.getAbsolutePath()));

			final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(result.getAbsolutePath()));

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

			bufferedReader.close();

			currentLine = bufferedReader1.readLine();

			// Relevanten Teil der zweiten Teilsimulation �bertragen
			while (!currentLine.startsWith(warmupTime + ",00;")) {
				currentLine = bufferedReader1.readLine();
			}

			int origNumber = warmupTime - 300;
			int newNumber = Integer.decode(lastLine.split(";")[0].split(",")[0]);

			while (currentLine != null) {
				origNumber += 300;
				newNumber += 300;

				currentLine = currentLine.replaceFirst(String.valueOf(origNumber), String.valueOf(newNumber));

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
