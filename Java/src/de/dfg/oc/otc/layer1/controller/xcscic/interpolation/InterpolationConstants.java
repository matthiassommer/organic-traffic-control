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

package de.dfg.oc.otc.layer1.controller.xcscic.interpolation;

import de.dfg.oc.otc.layer1.controller.xcscic.interpolation.components.interpolants.InterpolantType;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by Anthony Stein on 28.03.14.
 * <p>
 * Die Konfigurationsdatei für die Interpolations-Komponente {@link InterpolationComponent}.
 * Konstanten können in der "xcscic_interpolation_component.properties" angepasst werden.
 * </p>
 *
 * @author Anthony Stein
 * @author Dominik Rauh
 *
 */
public final class InterpolationConstants {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final String BASE_PATH;
    private static final String CONSTANTS_PROPERTIES_FILENAME = "xcscic_interpolation_component.properties";
    private static final String CONSTANTS_PROPERTIES_FOLDER = "config";

    private static final String INTERPOLATION_METHOD_KEY = "interpolation_method";
    private static final String INTERPOLATION_BASED_COVERING_KEY = "interpolation_based_covering";
    private static final String ALWAYS_EXECUTE_INTERPOLATED_ACTION_KEY = "execute_ic_action";
    private static final String IGNORE_TRUST_LEVEL_KEY = "ignore_trust_level";
    private static final String ADD_COVERED_TO_POP_KEY = "add_covered_to_pop";
    private static final String ASI_KEY = "asi";
    private static final String IDW_EXPO_KEY = "idw_expo";
    private static final String TAU_TAX_KEY = "tau_tax";
    private static final String IOTA_INCENTIVE_KEY = "iota_incentive";
    private static final String P_MAX_KEY = "p_max";
    private static final String T_WINDOW_KEY = "t_window";


    //Defaults
    /** Von {@link InterpolationConstants} genutzte Interpolante */
    private static final InterpolantType defaultInterpolationMethod = InterpolantType.INVERSE_DISTANCE_WEIGHTING;
    // Soll interpolations-basiertes Covering verwendet werden?
    private static final boolean DEFAULT_INTERPOLATION_BASED_COVERING = true;
    /** Soll das XCS unabhängig vom action set immer die interpolierte action ausführen? */
    private static final boolean DEFAULT_ALWAYS_EXECUTE_INTERPOLATED_ACTION = false;
    /** Soll der trust-level bei der ASI und beim covering ignoriert werden? */
    private static final boolean DEFAULT_IGNORE_TRUST_LEVEL = false;
    /** Soll ein durch covering erzeugter classifier zur population hinzugefügt werden? */
    private static final boolean DEFAULT_ADD_COVERED_TO_POP = true;
    /** Soll Action-Selection-Integration verwendet werden? */
    private static final boolean DEFAULT_ASI = true;
    /** Wie hoch soll der Exponent der IDW-Interpolante sein */
    private static final int DEFAULT_IDW_EXPO = 5;
    // Der Parameter für die Bestrafung / Besteuerung bei Verwendung der Nearest Neighbor Interpolation
    private static final double default_tau_tax = 0.3;
    // Der Parameter für die Belohnung / den Bonus bei Verwendung der Nearest Neighbor Interpolation
    private static final double default_iota_incentive = 0.1;
    // Der Parameter für die maximal zulässige Anzahl der Stützstellen in der Interpolante
    private static final int default_p_max = 200;
    // Der Parameter für das Evaluationsfenster zur Berechnung der Evaluations-Metriken bzw. des Trust-Levels T_IC
    private static final int default_t_window = 10;

    /** Von {@link InterpolationComponent} genutzte Interpolante */
    public static final InterpolantType interpolationMethod;
    /** Soll interpolations-basiertes Covering verwendet werden? */
    public static final boolean INTERPOLATION_BASED_COVERING;
    /** Soll das XCS unabhängig vom action set immer die interpolierte action ausführen? */
    public static final boolean ALWAYS_EXECUTE_INTERPOLATED_ACTION;
    /** Soll der trust-level bei der ASI und beim covering ignoriert werden? */
    public static final boolean IGNORE_TRUST_LEVEL;
    /** Soll ein durch covering erzeugter classifier zur population hinzugefügt werden? */
    public static final boolean ADD_COVERED_TO_POP;
    /** Soll Action-Selection-Integration verwendet werden? */
    public static final boolean ASI;
    /** Wie hoch soll der Exponent der IDW-Interpolante sein */
    public static final int IDW_EXPO;
    // Der Parameter für die Bestrafung / Besteuerung bei Verwendung der Nearest Neighbor Interpolation
    public static double tau_tax;
    // Der Parameter für die Belohnung / den Bonus bei Verwendung der Nearest Neighbor Interpolation
    public static double iota_incentive;
    // Der Parameter für die maximal zulässige Anzahl der Stützstellen in der Interpolante
    public static final int p_max;
    // Der Parameter für das Evaluationsfenster zur Berechnung der Evaluations-Metriken bzw. des Trust-Levels T_IC
    public static final int t_window;

    private static Properties constantsProperties;

    static {
        File baseFile = new File(InterpolationConstants.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        BASE_PATH = baseFile.getParent();

        constantsProperties = new Properties();
        createConfigFolder();
        boolean propertiesCouldNotBeLoaded = loadConstantsProperties();

        interpolationMethod = getInterpolantType();
        INTERPOLATION_BASED_COVERING = isInterpolationBasedCovering();
        ALWAYS_EXECUTE_INTERPOLATED_ACTION = isAlwaysExecuteInterpolatedAction();
        IGNORE_TRUST_LEVEL = isIgnoreTrustLevel();
        ADD_COVERED_TO_POP = isAddCoveredToPop();
        ASI = isAsi();
        IDW_EXPO = getIdwExpo();
        tau_tax = getTauTax();
        iota_incentive = getIotaIncentive();
        p_max = getPMax();
        t_window = getTWindow();

        if(propertiesCouldNotBeLoaded) { storeConstantsProperties(); }
    }

    private InterpolationConstants() { }

    private static void createConfigFolder()
    {
        File configFolder = new File(BASE_PATH + File.separator + CONSTANTS_PROPERTIES_FOLDER);
        if(!configFolder.exists()) { configFolder.mkdir(); }
    }

    private static boolean loadConstantsProperties() {
        boolean exceptionWasThrown = false;
        String exceptionMessage = null;

        try (InputStream in = new FileInputStream(BASE_PATH
                + File.separator
                + CONSTANTS_PROPERTIES_FOLDER
                + File.separator
                + CONSTANTS_PROPERTIES_FILENAME)) {
            constantsProperties.load(in);
        } catch (FileNotFoundException e) {
            exceptionMessage = e.getMessage();
            exceptionWasThrown = true;
        } catch (IOException e) {
            exceptionMessage = e.getMessage();
            exceptionWasThrown = true;
        }

        if (exceptionWasThrown) {
            LOGGER.info(
                    "Could not load " + CONSTANTS_PROPERTIES_FILENAME + ": " + exceptionMessage + ". Defaulting...");
            loadConstantsPropertiesDefaults();
        }

        return exceptionWasThrown;
    }

    private static void loadConstantsPropertiesDefaults() {
        constantsProperties.setProperty(INTERPOLATION_METHOD_KEY, defaultInterpolationMethod.name());
        constantsProperties.setProperty(INTERPOLATION_BASED_COVERING_KEY, Boolean.toString(DEFAULT_INTERPOLATION_BASED_COVERING));
        constantsProperties.setProperty(ALWAYS_EXECUTE_INTERPOLATED_ACTION_KEY, Boolean.toString(DEFAULT_ALWAYS_EXECUTE_INTERPOLATED_ACTION));
        constantsProperties.setProperty(IGNORE_TRUST_LEVEL_KEY, Boolean.toString(DEFAULT_IGNORE_TRUST_LEVEL));
        constantsProperties.setProperty(ADD_COVERED_TO_POP_KEY, Boolean.toString(DEFAULT_ADD_COVERED_TO_POP));
        constantsProperties.setProperty(ASI_KEY, Boolean.toString(DEFAULT_ASI));
        constantsProperties.setProperty(IDW_EXPO_KEY, Integer.toString(DEFAULT_IDW_EXPO));
        constantsProperties.setProperty(TAU_TAX_KEY, Double.toString(default_tau_tax));
        constantsProperties.setProperty(IOTA_INCENTIVE_KEY, Double.toString(default_iota_incentive));
        constantsProperties.setProperty(P_MAX_KEY, Integer.toString(default_p_max));
        constantsProperties.setProperty(T_WINDOW_KEY, Integer.toString(default_t_window));
    }

    private static void storeConstantsProperties() {

        boolean exceptionWasThrown = false;
        String exceptionMessage = null;

        constantsProperties.setProperty(INTERPOLATION_METHOD_KEY, interpolationMethod.name());
        constantsProperties.setProperty(INTERPOLATION_BASED_COVERING_KEY, Boolean.toString(INTERPOLATION_BASED_COVERING));
        constantsProperties.setProperty(ALWAYS_EXECUTE_INTERPOLATED_ACTION_KEY, Boolean.toString(ALWAYS_EXECUTE_INTERPOLATED_ACTION));
        constantsProperties.setProperty(IGNORE_TRUST_LEVEL_KEY, Boolean.toString(IGNORE_TRUST_LEVEL));
        constantsProperties.setProperty(ADD_COVERED_TO_POP_KEY, Boolean.toString(ADD_COVERED_TO_POP));
        constantsProperties.setProperty(ASI_KEY, Boolean.toString(ASI));
        constantsProperties.setProperty(IDW_EXPO_KEY, Integer.toString(IDW_EXPO));
        constantsProperties.setProperty(TAU_TAX_KEY, Double.toString(tau_tax));
        constantsProperties.setProperty(IOTA_INCENTIVE_KEY, Double.toString(iota_incentive));
        constantsProperties.setProperty(P_MAX_KEY, Integer.toString(p_max));
        constantsProperties.setProperty(T_WINDOW_KEY, Integer.toString(t_window));

        try (OutputStream out = new FileOutputStream(BASE_PATH + File.separator
                + CONSTANTS_PROPERTIES_FOLDER
                + File.separator + CONSTANTS_PROPERTIES_FILENAME)) {
            constantsProperties.store(out, null);
        } catch (FileNotFoundException e) {
            exceptionMessage = e.getMessage();
            exceptionWasThrown = true;
        } catch (IOException e) {
            exceptionMessage = e.getMessage();
            exceptionWasThrown = true;
        }

        if(exceptionWasThrown) {
            LOGGER.warning("Could not store " + CONSTANTS_PROPERTIES_FILENAME + ": " + exceptionMessage);
        }
    }

    private static InterpolantType getInterpolantType() {
        return InterpolantType.valueOf(constantsProperties.getProperty(INTERPOLATION_METHOD_KEY));
    }

    private static boolean isInterpolationBasedCovering() {
        return Boolean.parseBoolean(constantsProperties.getProperty(INTERPOLATION_BASED_COVERING_KEY));
    }

    private static boolean isAlwaysExecuteInterpolatedAction() {
        return Boolean.parseBoolean(constantsProperties.getProperty(ALWAYS_EXECUTE_INTERPOLATED_ACTION_KEY));
    }

    private static boolean isIgnoreTrustLevel() {
        return Boolean.parseBoolean(constantsProperties.getProperty(IGNORE_TRUST_LEVEL_KEY));
    }

    private static boolean isAddCoveredToPop() {
        return Boolean.parseBoolean(constantsProperties.getProperty(ADD_COVERED_TO_POP_KEY));
    }

    private static boolean isAsi() {
        return Boolean.parseBoolean(constantsProperties.getProperty(ASI_KEY));
    }

    private static int getIdwExpo() {
        return Integer.parseInt(constantsProperties.getProperty(IDW_EXPO_KEY));
    }

    private static double getTauTax() {
        return Double.parseDouble(constantsProperties.getProperty(TAU_TAX_KEY));
    }

    private static double getIotaIncentive() {
        return Double.parseDouble(constantsProperties.getProperty(IOTA_INCENTIVE_KEY));
    }

    private static int getPMax() {
        return Integer.parseInt(constantsProperties.getProperty(P_MAX_KEY));
    }

    private static int getTWindow() {
        return Integer.parseInt(constantsProperties.getProperty(T_WINDOW_KEY));
    }
}
