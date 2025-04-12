package de.dfg.oc.otc.layer1.controller;

import de.dfg.oc.otc.config.DefaultParams;

/**
 * This class provides all learning parameters for the LCS as well as other
 * experimental settings.
 */
public class LCSConstants {
    private static LCSConstants instance;
    /**
     * Turning-based situations or signal group-based situation?
     */
    private final boolean turningBasedSituation;
    /**
     * ALPHA parameter for accuracy determination.
     */
    private final float alpha;
    /**
     * Learning rate parameter BETA: A higher value leads to less history
     * dependence, but also to higher variances when differing rewards are
     * received.
     */
    private final float beta;
    /**
     * Prediction error value that is used for initialization when a new
     * classifier is created.
     */
    private final float epsilonInit;
    /**
     * EPSILON_ZERO parameter for accuracy determination.
     */
    private final float epsilonZero;
    /**
     * Initial experience for newly created classifiers.
     */
    private final int experienceInit;
    /**
     * Fitness value that is used for initialization when a new classifier is
     * created.
     */
    private final float fitnessInit;
    /**
     * Width of the interval that is used when creating a new classifier for a
     * specific situation.
     */
    private final int intervalWidthForNewConditions;
    /**
     * NY parameter for accuracy determination.
     */
    private final float ny;
    /**
     * The experience of a classifier required to be able to subsume other
     * classifiers.
     */
    private final int thetaSub;

    private LCSConstants() {
        intervalWidthForNewConditions = DefaultParams.L1_INTERVAL_WIDTH_FOR_NEW_CONDITIONS;
        experienceInit = DefaultParams.EXPERIENCE_INIT;
        beta = DefaultParams.BETA;
        epsilonInit = DefaultParams.EPSILON_INIT;
        fitnessInit = DefaultParams.FITNESS_INIT;
        epsilonZero = DefaultParams.EPSILON_ZERO;
        alpha = DefaultParams.ALPHA;
        ny = DefaultParams.NY;
        thetaSub = DefaultParams.THETA_SUB;
        turningBasedSituation = DefaultParams.TURNING_BASED_SITUATION;
    }

    public static LCSConstants getInstance() {
        if (instance == null) {
            instance = new LCSConstants();
        }
        return instance;
    }

    public final float getAlpha() {
        return alpha;
    }

    public final float getBeta() {
        return beta;
    }

    /**
     * @return a string containing the current configuration
     */
    public final String getConfig() {
        final String linesep = System.getProperty("line.separator");
        return "LCS CONFIG" + linesep + "Learning rate beta " + beta + ", inital prediction error epsilonInit "
                + epsilonInit + ", initial fitness value fitnessInit " + fitnessInit + linesep
                + "accuracy determination variables: epsilonZero " + epsilonZero + ", alpha " + alpha + ", ny " + ny
                + linesep + "expericence value thetaSub " + thetaSub + ", intervalWidthForNewConditions "
                + intervalWidthForNewConditions + ", initial experience value experienceInit " + experienceInit
                + ", turningBasedSituation " + turningBasedSituation;
    }

    public final float getEpsilonInit() {
        return epsilonInit;
    }

    public final float getEpsilonZero() {
        return epsilonZero;
    }

    public final int getExperienceInit() {
        return experienceInit;
    }

    public final float getFitnessInit() {
        return fitnessInit;
    }

    public final int getIntervalWidthForNewConditions() {
        return intervalWidthForNewConditions;
    }

    public final float getNy() {
        return ny;
    }

    public final int getThetaSub() {
        return thetaSub;
    }
}
