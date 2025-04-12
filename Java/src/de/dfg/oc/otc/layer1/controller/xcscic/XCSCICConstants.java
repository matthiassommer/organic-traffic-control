/*
 * Copyright (c) 2015 by
 * Anthony Stein, M.Sc.
 * University of Augsburg
 * Department of Computer Science
 * Chair of Organic Computing
 * All rights reserved. Distribution without approval by the copyright holder is explicitly prohibited.
 * Sources are only for non-commercial and academic use
 * in the scope of student theses and courses of the University of Augsburg.
 *
 * THE SOFTWAREPARTS ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package de.dfg.oc.otc.layer1.controller.xcscic;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Anthony Stein
 *         <p>
 *         Diese Klasse stellt eine Schnittstelle für die Konfiguration
 *         sämtlicher für das XCSCIC notwendige Parameter zur Verfügung.
 * @author Anthony Stein
 * @author Dominik Rauh
 */

public final class XCSCICConstants {

    public static final String BASE_PATH;
    public static final String EVAL_FOLDER = "eval";
    public static final String EVAL_EXPERIMENT_FOLDER_BASE = "run";
    public static String EVAL_EXPERIMENT_FOLDER;

    /* Der Pfad für die Erstellung einer Textdatei, welche die experimentellen Resultate enthält. */
    public static String OUT_FILE_PATH;
    public static final String OUT_FILE_EXTENSION = ".csv";
    public static final String OUT_FILE_DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    public static final Charset OUT_FILE_CHARSET = StandardCharsets.UTF_8;

    static {
        File baseFile = new File(XCSCICConstants.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        BASE_PATH = baseFile.getParent();

        createEvalFolder();
        generateNewFolderNames();
    }

    private XCSCICConstants() {
    }

    public static void generateNewFolderNames() {
        String timeStamp = new SimpleDateFormat(OUT_FILE_DATE_FORMAT).format(new Date());
        EVAL_EXPERIMENT_FOLDER = createExperimentFolder(timeStamp);
        OUT_FILE_PATH = createEvalFilenames(timeStamp);
    }

    private static void createEvalFolder() {
        File evalFolder = new File(BASE_PATH + File.separator + EVAL_FOLDER);

        if (!evalFolder.exists()) {
            evalFolder.mkdir();
        }
    }

    private static String createExperimentFolder(String timeStamp) {
        String folderName =
                BASE_PATH + File.separator
                        + EVAL_FOLDER + File.separator
                        + EVAL_EXPERIMENT_FOLDER_BASE
                        + "_" + timeStamp;

        File experimentFolder = new File(folderName);

        if (!experimentFolder.exists()) {
            experimentFolder.mkdir();
        } else {
            folderName = folderName + "(1)";
            new File(folderName).mkdir();
        }

        return folderName;
    }

    private static String createEvalFilenames(String timeStamp) {
        return EVAL_EXPERIMENT_FOLDER
                + File.separator
                + "xcscic";
    }
}
