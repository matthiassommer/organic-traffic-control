package de.dfg.oc.otc.config;

import java.net.URL;

/**
 * This class defines the default values used for important parameters in the system.
 */
public abstract class DefaultParams {
    /**
     * Method used to specify the path information of the current class.
     */
    public static String getPath() {
        final Class<DefaultParams> c = DefaultParams.class;
        final ClassLoader loader = c.getClassLoader();
        final URL url = loader.getResource(c.getName().replace('.', '/') + ".class");

        String pfad = null;
        if (url != null) {
            pfad = url.toString();
        }
        assert pfad != null;
        pfad = pfad.substring(0, pfad.lastIndexOf(c.getName().replace('.', '/')));
        pfad = pfad.substring(5);
        pfad = pfad.replace("%20", " ");
        return pfad;
    }


    // MANAGER
    public static final boolean USE_GUI = true;
    // 3=XCST, 4=XCSIC, 5=XCSCIC
    public static final int TLC_SELECTOR = 3;
    public static final String PATH = getPath();


    // LAYER 0
    public static final float L0_DEFAULT_CAPACITY = 2400;
    public static final float L0_DEFAULTEXTSTEP = 1;
    public static final int L0_CONTROLLER = 1;
    public static final float L0_ABSOLUTE_MIN_DURATION = 5;
    // +***********************************************-Anzahl Zyklen, die zwischen zwei aufeinanderfolgenden Bewertungen liegen sollten
    public static final int L0_MIN_CYCLES_BETWEEN_EVALUATIONS = 2;
    /**
     * Anzahl Zyklen, die ein neuer TLC mindestens aktiv bleibt, bevor er
     * ausgetauscht werden kann
     */
    public static final int L0_MIN_CYCLES_DELAY = 2;
    public static final float L0_SMOOTH_INTERVAL = 15;
    public static final int L0_MIN_ENTRIES_AVERAGE = 3;
    /**
     * Anzahl an Umlaufzeiten, die das evaluationInterval ergeben sollen. (Ein
     * gr��erer Wert verringert die Schwankungen in der Situationserkennung.
     */
    public static final int L0_NUM_CYCLES_EVALUATION_INTERVAL = 10;


    // LAYER 1
    public static final float L1_ADJUST_TO_QUEUE_FACTOR = 1.15f;
    public static final int L1_WARMUP_TIME = 1200;
    public static final boolean L1_ACTIVE = true;
    public static final int L1_INTERVAL_WIDTH_FOR_NEW_CONDITIONS = 120;


    // LAYER 2
    public static final boolean L2_FIXED_SEED_FOR_EVALUATION = true;
    public static final boolean L2_SLOW_IF_LAYER2_BUSY = true;
    public static final boolean L2_LOG_LCS_MAPPING = true;
    public static final int L2_WARMUP_TIME = 0;
    public static final boolean L2_USE_WEBSTER = true;
    public static final long L2_MASTERSEED = 1;
    public static final boolean L2_DRAWCHART = false;
    public static final boolean L2_USE_AVG_FITNESS = false;

    // EA CONFIG
    public static final int EA_SIM_DURATION = 7200;
    public static final int EA_LAST_RUN_SIM_DURATION = 10800;
    public static final int EA_RE_EVALUATION_DURATION = 5400;
    public static final double EA_MUTATION_PROPABILITY = 1;
    public static final double EA_MUTATION_STEP = 0;
    public static final boolean EA_CREATE_LOGFILES = false;
    public static final double EA_CROSSOVERPROB = 1;
    // false=plus strategy, true = comma strategy
    public static final boolean EA_COMMASTRATEGY = false;
    public static final int EA_POP_SIZE = 16;
    public static final int EA_NUMBER_OF_CHILDREN = 24;
    public static final int L2_MAX_GENERATIONS = 64;


    // DPSS
    public static final boolean PSS_DECENTRAL_ACTIVE = false;
    public static final boolean PSS_LOG = false;
    public static final boolean PSS_USE_NEIGHBOR_STREAMS = false;
    public static final float RECALCULATE_PSS_INTERVAL = 15000;
    public static final boolean PSS_REGION_ACTIVE = false;
    public static final float PSS_ACTDIFF = 5;
    public static final float PSS_MIN_PREDICTION_DIFFERENCE = 5;
    public static final int PSS_INTERVAL_LENGTH_FOR_STREAM = 1000;
    public static final float PSS_CHECK_INTERVAL = 300;


    // ROUTING
    public static String ROUTING_PROTOCOL = "none";
    public static final float ROUTING_INTERVAL = 150;
    // 1=Webster, 2=Statistik
    public static final int ROUTING_DELAY_CALCULATION = 1;


    // LCS / XCS
    /**
     * Flag if LCS data should be logged.
     */
    public static final boolean LOG_LCS_DATA = true;
    public static final int THETA_SUB = 10;
    public static final int THETA_DEL = 20;
    public static final double DELTA = 0.1;
    public static final float NY = 5;
    public static final boolean L1_USE_INTERVAL = false;
    public static final boolean TURNING_BASED_SITUATION = false;
    public static final int EXPERIENCE_INIT = 1;
    public static final float EPSILON_INIT = 25;
    public static final float EPSILON_ZERO = 2;
    public static final float FITNESS_INIT = 0.01f;
    public static final float ALPHA = 0.1f;
    public static final float BETA = 0.2f;
    public static final int MAX_POPULATION_SIZE = 200;


    // DATABASE for evaluation
    public static final String DB_USER = "otc";
    public static final String DB_PASSWORD = "layer2";
    public static final String DB_TABLE_NAME = "TurnSta";


    // AID
    public static boolean AID_ACTIVE = true;

    public static String AID_DEFAULT_ALGORITHM = "XCSRUrban";
    public static final String AID_DETECTOR_CAPABILITIES = "Occupancy Speed";

    public static final int AID_EVALUATION_TIME_SENSITIVITY = 300;
    public static final int AID_SCANNING_INTERVAL = 40;
    public static final int AID_WARUM_UP_TIME = 0;

    public static final String AID_COMBINATION_STRATEGY = "MAJORITY_VOTE";
    public static final String AID_COMBINED_ALGORITHMS = "CA8,fuzzy knn";


    // DISTURBANCE
    public static boolean DIST_ACTIVE = true;

    public static final int DIST_TLC_CHANGE_ACTIVE = 1;
    public static final float DIST_TLC_LAMBDA = 1f;
    public static final boolean DIST_USE_TENTATIVE_INCIDENTS = false;

    public static final float DIST_CALCULATION_INTERVAL = 60;
    public static final String DIST_DEGREE_CALCULATION_METHOD = "SPEEDOCCUPANCY";
    public static final float DIST_STATIC_DEGREE = 0.5f;


    // PUBLIC TRANSPORT
    public static final int PT_FEATURE_SETTING = 0;
    public static final int PT_PHASE_CHANGE_METHOD = 1;
    public static final float PT_OFFSET = 3.0f;
}