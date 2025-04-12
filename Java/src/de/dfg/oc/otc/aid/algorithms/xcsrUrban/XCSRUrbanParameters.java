package de.dfg.oc.otc.aid.algorithms.xcsrUrban;

import de.dfg.oc.otc.aid.algorithms.xcsrUrban.continuous.RealValueAllele;

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
public class XCSRUrbanParameters implements Serializable {

    /**
     * Folder where the evaluation results are stored.
     */
    public static String EVALUATION_FOLDER = "..\\Studentische Arbeiten\\PM Sach\\Evaluation\\3 Auswertung\\AID\\";

    /**
     * Specifies the maximal number of micro-classifiers in the population.
     * In the multiplexer problem this value is set to 400, 800, 2000 in the 6, 11, 20 multiplexer resp..
     */
    public int maxPopSize = 4500;

    /**
     * Number of distinct actions.
     */
    final int theta_mna = 2;

    // Die untere und obere Schranke des Problemraums
    public final double minPhenotypeValue = 0.0;
    public final double maxPhenotypeValue = 1.0;
    /**
     * The reduction factor in the fitness evaluation.
     */
    public double alpha = 0.1;
    /**
     * The learning rate for updating fitness, prediction, prediction error,
     * and action set size estimate in XCS's classifiers.
     */
    public double beta = 0.2;
    /**
     * The fraction of the mean fitness of the population below which the fitness of a classifier may be considered
     * in its vote for deletion.
     */
    public double delta = 0.1;
    /**
     * Specifies the exponent in the power function for the fitness evaluation.
     */
    double  nu = 5.;
    /**
     * The threshold for the GA application in an action set.
     */
    public double theta_GA = 50;
    /**
     * The error threshold under which the accuracy of a classifier is set to one.
     */
    double epsilon_0 = 10.0;
    /**
     * Specified the threshold over which the fitness of a classifier may be considered in its deletion probability.
     */
    int theta_del = 20;
    /**
     * The probability of applying crossover in an offspring classifier.
     */
    public double pX = 0.5;
    /**
     * The probability of mutating one allele and the action in an offspring classifier.
     */
    public double pM = 0.05;
    /**
     * The probability of using a don't care symbol in an allele when covering.
     */
    double P_dontcare = 0.0;
    /**
     * The reduction of the prediction error when generating an offspring classifier.
     */
    public double predictionErrorReduction = 1.0;
    /**
     * The reduction of the fitness when generating an offspring classifier.
     */
    public double fitnessReduction = 0.1;
    /**
     * The experience of a classifier required to be a subsumer.
     */
    int theta_sub = 20;
    /**
     * The initial prediction value when generating a new classifier (e.g in covering).
     */
    final public double predictionIni = 10.0;
    /**
     * The initial prediction error value when generating a new classifier (e.g in covering).
     */
    final public double predictionErrorIni = 0.0;
    /**
     * The initial fitness value when generating a new classifier (e.g in covering).
     */
    final public double fitnessIni = 0.01;

    // Die verwendete Repräsentation der Condition-Allele (CSR, OBR, UBR)
    public final RealValueAllele.AlleleRepresentations usedGeneRepresentation =
            RealValueAllele.AlleleRepresentations.UNORDERED_BOUND;
    // s_0 Parameter (vgl. Wilson(2000)) für den standardmäßigen "spread" während der Covering-Routine
    private final double defaultSpread = 0.2;
    // Für den Mutation-Operator unter Verwendung der CENTER-SPREAD Representation
    private final double m_cs = 0.1;
    // Für den Mutation-Operator unter Verwendung der (UN-)ORDERED-BOUND Representation
    private final double m_ob = 0.2;
    // Für den Covering-Operator unter Verwendung der (UN-)ORDERED-BOUND Representation
    private final double r_ob = 0.2;

    /**
     * Constant for the random number generator (modulus of PMMLCG = 2^31 -1).
     */
    final private long _M = 2147483647;
    /**
     * Constant for the random number generator (default = 16807).
     */
    final private long _A = 16807;
    /**
     * Constant for the random number generator (=_M/_A).
     */
    final private long _Q = _M / _A;
    /**
     * Constant for the random number generator (=_M mod _A).
     */
    final private long _R = _M % _A;
    /**
     * The initialization of the pseudo random generator. Must be at lest one and smaller than _M.
     */
    private long seed = 1;

    /**
     * The default constructor.
     */
    public XCSRUrbanParameters() {
    }

    /**
     * Sets a random seed in order to randomize the pseudo random generator.
     */
    public void setSeed(long s) {
        seed = s;
    }

    public long getSeed()
    {
        return seed;
    }

    /**
     * Returns a random number in between zero and one.
     */
    public double drand() {
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
     * Returns a random number in between the specified bounds
     */
    public double drand(double lo, double hi)
    {
        double r = drand();
        return lo + (hi - lo) * r;
    }

    /**
     * Erstellt einen reellwertiges "Dont-Care" Allel (Wildcard Operator)
     * Entspricht einem Maximal generellen Intervall-Prädikat (minPhenotypeValue, maxPhenotypeValue)
     *
     * @return Reellwertiges "Dont-Care" Allel
     */
    RealValueAllele realDontCare() {
        switch (usedGeneRepresentation) {
            case ORDERED_BOUND:
                return new RealValueAllele(this, minPhenotypeValue, maxPhenotypeValue, RealValueAllele.AlleleRepresentations.ORDERED_BOUND);
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
                return new RealValueAllele(this, endpoint_left, endpoint_right, RealValueAllele.AlleleRepresentations.ORDERED_BOUND);
            case CENTER_SPREAD:
                return new RealValueAllele(this, minPhenotypeValue, maxPhenotypeValue, RealValueAllele.AlleleRepresentations.CENTER_SPREAD);
            default:
                return new RealValueAllele(this, minPhenotypeValue, maxPhenotypeValue, RealValueAllele.AlleleRepresentations.ORDERED_BOUND);
        }
    }

    /**
     * Generiert einen zufällig generierten "Spread" / Spanne / Abweichung zum Ausgangswert
     * in Abhängikeit von der verwendeten Condition-Repräsentation
     *
     * @return Einen zufällig generierten "Spread" / Spanne / Abweichung zum Ausgangswert
     */
    double getRandomSpread() {
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
    public double getRandomMutationValue() {
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

    public void setParameters(int maxPopSize, double beta, double theta_GA, double pX, double pM, double predictionErrorReduction)
    {
        this.maxPopSize = maxPopSize;
        this.beta = beta;
        this.theta_GA = theta_GA;
        this.pX = pX;
        this.pM = pM;
        this.predictionErrorReduction = predictionErrorReduction;

//        maxPopSize = (int)drand(4.0, 8.0) * 1000;   //1000-6000
//        alpha = drand(0.05, 0.5);
//        beta = drand(0.1, 0.5);             // 0.1-0.2
//        delta = drand(0.05, 0.5);
////        nu = drand(3, 7);           //5
//        theta_GA = drand(3, 7);     //25-50
//        epsilon_0 = drand(8,12);    //
//        theta_del = (int)drand(15,25);  //20
//        pX = drand(0.1,0.05);   //0.5-1
//        pM = drand(0.03, 0.07); //1%-5%
////        P_dontcare = drand(0.0, 0.05);
//        predictionErrorReduction = drand(0.15, 0.35);   //0.1 oder 1
//        fitnessReduction = drand(0.05, 0.25);   //1
//        theta_sub = (int)drand(10, 30);
    }

    public String printParameters()
    {
        String str = "maxPopSize = " + maxPopSize + "\n";
        str += "alpha = " + alpha + "\n";
        str += "beta = " + beta + "\n";
        str += "delta = " + delta + "\n";
        str += "nu = " + nu + "\n";
        str += "theta_GA = " + theta_GA + "\n";
        str += "epsilon_0 = " + epsilon_0 + "\n";
        str += "theta_del = " + theta_del + "\n";
        str += "pX = " + pX + "\n";
        str += "pM = " + pM + "\n";
        str += "P_dontcare = " + P_dontcare + "\n";
        str += "predictionErrorReduction = " + predictionErrorReduction + "\n";
        str += "fitnessReduction = " + fitnessReduction + "\n";
        str += "theta_sub = " + theta_sub + "\n";
        return str;
    }
}
