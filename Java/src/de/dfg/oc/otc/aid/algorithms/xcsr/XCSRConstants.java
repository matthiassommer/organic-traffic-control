package de.dfg.oc.otc.aid.algorithms.xcsr;

import de.dfg.oc.otc.aid.algorithms.xcsr.continuous.RealValueAllele;

import java.io.Serializable;

/**
 * This class provides all relevant learning parameters for the XCS as well as
 * other experimental settings and flags. Most parameter-names are chosen similar to
 * the 'An Algorithmic Description of xcs.XCS' ( Butz&Wilson, IlliGAL report 2000017).
 *
 * @author Martin V. Butz
 * @version XCSJava 1.0
 * @since JDK1.1
 */
public class XCSRConstants implements Serializable {
    /**
     * Specifies the maximal number of micro-classifiers in the population.
     * In the multiplexer problem this value is set to 400, 800, 2000 in the 6, 11, 20 multiplexer resp..
     */
    final static int maxPopSize = 6000;

    /**
     * Number of distinct actions.
     */
    final static int theta_mna = 2;

    // Die untere und obere Schranke des Problemraums
    public static final double minPhenotypeValue = 0.0;
    public static final double maxPhenotypeValue = 1.0;
    /**
     * The reduction factor in the fitness evaluation.
     */
    final public static double alpha = 0.1;
    /**
     * The learning rate for updating fitness, prediction, prediction error,
     * and action set size estimate in XCS's classifiers.
     */
    final public static double beta = 0.2;
    /**
     * The fraction of the mean fitness of the population below which the fitness of a classifier may be considered
     * in its vote for deletion.
     */
    final public static double delta = 0.1;
    /**
     * Specifies the exponent in the power function for the fitness evaluation.
     */
    final static double nu = 5.;
    /**
     * The threshold for the GA application in an action set.
     */
    final public static double theta_GA = 5;
    /**
     * The error threshold under which the accuracy of a classifier is set to one.
     */
    final static double epsilon_0 = 10.0;
    /**
     * Specified the threshold over which the fitness of a classifier may be considered in its deletion probability.
     */
    final static int theta_del = 20;
    /**
     * The probability of applying crossover in an offspring classifier.
     */
    final public static double pX = 0.3;
    /**
     * The probability of mutating one allele and the action in an offspring classifier.
     */
    final public static double pM = 0.05;
    /**
     * The probability of using a don't care symbol in an allele when covering.
     */
    final static double P_dontcare = 0.0;
    /**
     * The reduction of the prediction error when generating an offspring classifier.
     */
    final public static double predictionErrorReduction = 0.25;
    /**
     * The reduction of the fitness when generating an offspring classifier.
     */
    final public static double fitnessReduction = 0.1;
    /**
     * The experience of a classifier required to be a subsumer.
     */
    final static int theta_sub = 20;
    /**
     * The initial prediction value when generating a new classifier (e.g in covering).
     */
    final public static double predictionIni = 10.0;
    /**
     * The initial prediction error value when generating a new classifier (e.g in covering).
     */
    final public static double predictionErrorIni = 0.0;
    /**
     * The initial fitness value when generating a new classifier (e.g in covering).
     */
    final public static double fitnessIni = 0.01;

    // Die verwendete Repräsentation der Condition-Allele (CSR, OBR, UBR)
    public static final RealValueAllele.AlleleRepresentations usedGeneRepresentation =
            RealValueAllele.AlleleRepresentations.UNORDERED_BOUND;
    // s_0 Parameter (vgl. Wilson(2000)) für den standardmäßigen "spread" während der Covering-Routine
    private static final double defaultSpread = 0.2;
    // Für den Mutation-Operator unter Verwendung der CENTER-SPREAD Representation
    private static final double m_cs = 0.1;
    // Für den Mutation-Operator unter Verwendung der (UN-)ORDERED-BOUND Representation
    private static final double m_ob = 0.2;
    // Für den Covering-Operator unter Verwendung der (UN-)ORDERED-BOUND Representation
    private static final double r_ob = 0.2;

    /**
     * Constant for the random number generator (modulus of PMMLCG = 2^31 -1).
     */
    final private static long _M = 2147483647;
    /**
     * Constant for the random number generator (default = 16807).
     */
    final private static long _A = 16807;
    /**
     * Constant for the random number generator (=_M/_A).
     */
    final private static long _Q = _M / _A;
    /**
     * Constant for the random number generator (=_M mod _A).
     */
    final private static long _R = _M % _A;
    /**
     * The initialization of the pseudo random generator. Must be at lest one and smaller than _M.
     */
    private static long seed = 1;

    /**
     * The default constructor.
     */
    public XCSRConstants() {
    }

    /**
     * Sets a random seed in order to randomize the pseudo random generator.
     */
    public static void setSeed(long s) {
        seed = s;
    }

    /**
     * Returns a random number in between zero and one.
     */
    public static double drand() {
        long hi = seed / _Q;
        long lo = seed % _Q;
        long test = _A * lo - _R * hi;

        if (test > 0) {
            seed = test;
        }
        else {
            seed = test + _M;
        }

        return (double) (seed) / _M;
    }

    /**
     * Erstellt einen reellwertiges "Dont-Care" Allel (Wildcard Operator)
     * Entspricht einem Maximal generellen Intervall-Prädikat (minPhenotypeValue, maxPhenotypeValue)
     *
     * @return Reellwertiges "Dont-Care" Allel
     */
    static RealValueAllele realDontCare() {
        switch (usedGeneRepresentation) {
            case ORDERED_BOUND:
                return new RealValueAllele(minPhenotypeValue, maxPhenotypeValue, RealValueAllele.AlleleRepresentations.ORDERED_BOUND);
            case UNORDERED_BOUND:
                double endpoint_left, endpoint_right;
                // Um Verzerrungseffekt (Bias) bei Verwendung der UBR zu vermeiden
                if (drand() < 0.5) {
                    endpoint_left = minPhenotypeValue;
                    endpoint_right = maxPhenotypeValue;
                } else {
                    endpoint_left = maxPhenotypeValue;
                    endpoint_right = minPhenotypeValue;
                }
                return new RealValueAllele(endpoint_left, endpoint_right, RealValueAllele.AlleleRepresentations.ORDERED_BOUND);
            case CENTER_SPREAD:
                return new RealValueAllele(minPhenotypeValue, maxPhenotypeValue, RealValueAllele.AlleleRepresentations.CENTER_SPREAD);
            default:
                return new RealValueAllele(minPhenotypeValue, maxPhenotypeValue, RealValueAllele.AlleleRepresentations.ORDERED_BOUND);
        }
    }

    /**
     * Generiert einen zufällig generierten "Spread" / Spanne / Abweichung zum Ausgangswert
     * in Abhängikeit von der verwendeten Condition-Repräsentation
     *
     * @return Einen zufällig generierten "Spread" / Spanne / Abweichung zum Ausgangswert
     */
    static double getRandomSpread() {
        switch (usedGeneRepresentation) {
            case ORDERED_BOUND:
            case UNORDERED_BOUND:
                return drand() * r_ob;
            case CENTER_SPREAD:
                return drand() * defaultSpread;
            default:
                return drand() * r_ob;
        }
    }

    /**
     * Erstellt eine zufällig generierten Mutationswert in Abhängikeit von der verwendeten Condition-Repräsentation
     *
     * @return Zufällig generierter Mutationswert
     */
    public static double getRandomMutationValue() {
        boolean positive = drand() < 0.5;
        double value = 0.5;

        switch (usedGeneRepresentation) {
            case ORDERED_BOUND:
            case UNORDERED_BOUND:
                value = drand() * m_ob;
                break;
            case CENTER_SPREAD:
                value = drand() * m_cs;
                break;
        }
        // Vorzeichen wird ebenfalls zufällig gewählt!
        if (!positive) {
            return value *= -1;
        }
        return value;
    }
}
