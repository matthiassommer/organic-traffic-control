package de.dfg.oc.otc.aid.algorithms.xcsr;

import de.dfg.oc.otc.aid.algorithms.xcsr.continuous.ContinuousCondition;
import de.dfg.oc.otc.aid.algorithms.xcsr.continuous.RealValueAllele;
import de.dfg.oc.otc.aid.algorithms.xcsr.continuous.RealValueGene;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Anthony on 08.09.2014.
 */
public class RClassifier implements Comparable {
    public ContinuousCondition condition;
    public int action;
    public double prediction;
    public double prediction_error;
    public double fitness;
    public int experience;
    public double asSize;
    public int numerosity;
    public int timeStamp;

    // Covering Creation
    public RClassifier(Situation sigma_t, int probCount, int action, double p, double e, double f) {
        this.action = action;
        this.prediction = p;
        this.prediction_error = e;
        this.fitness = f;
        this.experience = 0;
        this.asSize = 1.0;
        this.numerosity = 1;
        this.timeStamp = probCount;

        createMatchingCondition(sigma_t.getFeatureVector());
    }

    // GA Creation
    public RClassifier(RClassifier cl2copy) {
        this.condition = new ContinuousCondition(cl2copy.condition);
        this.action = cl2copy.action;
        this.prediction = cl2copy.prediction;
        this.prediction_error = cl2copy.prediction_error;
        this.fitness = cl2copy.fitness;
        this.experience = 0;
        this.asSize = cl2copy.asSize;
        this.numerosity = 1;
        this.timeStamp = cl2copy.timeStamp;
    }

    /**
     * Returns if the classifier matches in the current situation.
     * Distingushes between the applied Condition-Representation.
     *
     * @param state The current situation which can be the current state or problem instance.
     */
    public boolean match(double[] state) {
        switch (XCSRConstants.usedGeneRepresentation) {
            case CENTER_SPREAD:
                return matchCenterSpread(state);
            case ORDERED_BOUND:
                return matchOrderedBound(state);
            case UNORDERED_BOUND:
                return matchUnorderedBound(state);
            default:
                return matchOrderedBound(state);
        }
    }

    /**
     * Siehe Kommentar der Methode <code>match(...)</code>
     * für den Fall der Center-Spread-Representation
     */
    private boolean matchCenterSpread(double[] state) {
        if (condition.dimension != state.length) {
            return false;
        }

        double center, spread;

        for (int i = 0; i < condition.dimension; i++) {
            center = condition.getGene(i).getAllele().getSpecifiedRepresentation()[0];
            spread = condition.getGene(i).getAllele().getSpecifiedRepresentation()[1];
            if (state[i] < center - spread || state[i] >= (center + spread)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Siehe Kommentar der Methode <code>match(...)</code>
     * für den Fall der Ordered-Bound-Representation
     */
    private boolean matchOrderedBound(double[] state) {
        if (condition.dimension != state.length) {
            return false;
        }

        double lower, upper;

        for (int i = 0; i < condition.dimension; i++) {
            lower = condition.getGene(i).getAllele().getSpecifiedRepresentation()[0];
            upper = condition.getGene(i).getAllele().getSpecifiedRepresentation()[1];
            if (!(state[i] >= lower) || (!(state[i] < upper))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Siehe Kommentar der Methode <code>match(...)</code>
     * für den Fall der Unordered-Bound-Representation
     */
    private boolean matchUnorderedBound(double[] state) {
        if (condition.dimension != state.length) {
            return false;
        }

        double lower, upper;
        double endpoint_left, endpoint_right;

        for (int i = 0; i < condition.dimension; i++) {
            endpoint_left = condition.getGene(i).getAllele().getSpecifiedRepresentation()[0];
            endpoint_right = condition.getGene(i).getAllele().getSpecifiedRepresentation()[1];
            if (endpoint_left <= endpoint_right) {
                lower = endpoint_left;
                upper = endpoint_right;
            } else {
                lower = endpoint_right;
                upper = endpoint_left;
            }

            if (!(state[i] >= lower) || !(state[i] < upper)) {
                return false;
            }
        }
        return true;
    }

    public double getAccuracy() {
        if (prediction_error < XCSRConstants.epsilon_0) {
            return 1.;
        }
        return XCSRConstants.alpha * Math.pow(this.prediction_error / XCSRConstants.epsilon_0, (XCSRConstants.nu * -1));
    }

    double getDeletionProbability(double meanFitness) {
        double vote = this.asSize * this.numerosity;
        if (this.experience > XCSRConstants.theta_del && this.fitness / this.numerosity < XCSRConstants.delta * meanFitness) {
            vote = vote * meanFitness / (this.fitness / this.numerosity);
        }
        return vote;
    }

    /**
     * Is this classifier better than another? More experience and less error.
     *
     * @return better or not
     */
    public boolean isSubsumer() {
        return this.experience > XCSRConstants.theta_sub && this.prediction_error < XCSRConstants.epsilon_0;
    }

    /**
     * Creates a matching condition considering the constant <code>P_dontcare<\code>.
     */
    private void createMatchingCondition(double[] cond) {
        switch (XCSRConstants.usedGeneRepresentation) {
            case ORDERED_BOUND:
                condition = createMatchingConditionOrderedBound(cond);
                break;
            case CENTER_SPREAD:
                condition = createMatchingConditionCenterSpread(cond);
                break;
            case UNORDERED_BOUND:
                condition = createMatchingConditionUnorderedBound(cond);
                break;
            default:
                condition = createMatchingConditionOrderedBound(cond);
        }
    }

    /**
     * Siehe Kommentar der Methode <code>createMatchingCondition(...)</code>
     * für den Fall der Center-Spread-Representation
     */
    private ContinuousCondition createMatchingConditionCenterSpread(double[] cond) {
        // TODO: Evtl. die Länge der Situation gegen die spezifizierte Dimension checken!
        ContinuousCondition matchCond = new ContinuousCondition(cond.length);
        for (int i = 0; i < cond.length; i++) {
            if (XCSRConstants.drand() < XCSRConstants.P_dontcare) {
                matchCond.setGene(new RealValueGene(i, XCSRConstants.realDontCare()));
            } else {
                matchCond.setGene(new RealValueGene(i,
                        new RealValueAllele(cond[i],
                                XCSRConstants.getRandomSpread(),
                                XCSRConstants.usedGeneRepresentation)));
            }
        }
        return matchCond;
    }

    /**
     * Siehe Kommentar der Methode <code>createMatchingCondition(...)</code>
     * für den Fall der Ordered-Bound-Representation
     */
    private ContinuousCondition createMatchingConditionOrderedBound(double[] cond) {
        // TODO: Evtl. die Länge der Situation gegen die spezifizierte Dimension checken!
        ContinuousCondition matchCond = new ContinuousCondition(cond.length);
        for (int i = 0; i < cond.length; i++) {
            if (XCSRConstants.drand() < XCSRConstants.P_dontcare) {
                matchCond.setGene(new RealValueGene(i, XCSRConstants.realDontCare()));
            } else {
                double newLower = cond[i] - XCSRConstants.getRandomSpread();
                double newUpper = cond[i] + XCSRConstants.getRandomSpread();

                if (newLower < XCSRConstants.minPhenotypeValue) {
                    newLower = XCSRConstants.minPhenotypeValue;
                }
                if (newUpper > XCSRConstants.maxPhenotypeValue) {
                    newUpper = XCSRConstants.maxPhenotypeValue;
                }

                matchCond.setGene(new RealValueGene(i,
                        new RealValueAllele(newLower,
                                newUpper,
                                XCSRConstants.usedGeneRepresentation)));
            }
        }
        return matchCond;
    }

    /**
     * Siehe Kommentar der Methode <code>createMatchingCondition(...)</code>
     * für den Fall der Unordered-Bound-Representation
     */
    private ContinuousCondition createMatchingConditionUnorderedBound(double[] cond) {
        // TODO: Evtl. die Länge der Situation gegen die spezifizierte Dimension checken!
        ContinuousCondition matchCond = new ContinuousCondition(cond.length);
        for (int i = 0; i < cond.length; i++) {
            if (XCSRConstants.drand() < XCSRConstants.P_dontcare)
                matchCond.setGene(new RealValueGene(i, XCSRConstants.realDontCare()));
            else {
                double newLower = cond[i] - XCSRConstants.getRandomSpread();
                double newUpper = cond[i] + XCSRConstants.getRandomSpread();

                if (newLower < XCSRConstants.minPhenotypeValue) {
                    newLower = XCSRConstants.minPhenotypeValue;
                }
                if (newUpper > XCSRConstants.maxPhenotypeValue) {
                    newUpper = XCSRConstants.maxPhenotypeValue;
                }

                // To prevent bias from condition creation in unordered bound representation
                if (XCSRConstants.drand() < 0.5) {
                    double temp = newLower;
                    newLower = newUpper;
                    newUpper = temp;
                }
                matchCond.setGene(new RealValueGene(i,
                        new RealValueAllele(newLower,
                                newUpper,
                                XCSRConstants.usedGeneRepresentation)));
            }
        }
        return matchCond;
    }

    /**
     * Returns if the classifier is more general than cl. It is made sure that the classifier is indeed more general and
     * not equally general as well as that the more specific classifier is completely included in the more general one
     * (do not specify overlapping regions)
     *
     * @param cl The classifier that is tested to be more specific.
     */
    public boolean isMoreGeneral(RClassifier cl) {
        // TODO: Wildcards betrachten
        boolean ret = false;
        int length = condition.dimension;
        for (int i = 0; i < length; i++) {
            ret = condition.getGene(i).getAllele().subsumes(cl.condition.getGene(i).getAllele());
        }
        return ret;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o.getClass().equals(this.getClass())) {
            RClassifier cl_o = (RClassifier) o;
            boolean isEqual = (cl_o.condition.equals(this.condition) && cl_o.action == this.action);

            if (isEqual) {
                return 0;
            }
            return -1;
        }
        return -1;
    }

}
