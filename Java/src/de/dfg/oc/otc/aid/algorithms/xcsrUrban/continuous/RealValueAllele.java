package de.dfg.oc.otc.aid.algorithms.xcsrUrban.continuous;

import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrbanParameters;

import java.io.Serializable;

/**
 * Created by Anthony Stein on 27.02.14.
 */
public class RealValueAllele implements Serializable {
    private double lowerBound;
    private double upperBound;
    private double center;
    private double spread;
    private XCSRUrbanParameters constants;

    public RealValueAllele(XCSRUrbanParameters constants, double first, double last, AlleleRepresentations representation) {
        this.constants = constants;
        switch (representation) {
            case CENTER_SPREAD:
                setValuesWithCenterSpreadRepresentation(first, last);
                break;
            case ORDERED_BOUND:
                setValuesWithOrderedBoundRepresentation(first, last);
                break;
            case UNORDERED_BOUND:
                // Bias already prevented in XRClassifer
                // @see XRClassifier::createMatchingConditionUnorderedBound(double[])
                setValuesWithOrderedBoundRepresentation(first, last);
                break;
        }
        shrinkToBounds();
    }

    private double[] getCenterSpreadRepresentation() {
        return new double[]{center, spread};
    }

    private double[] getOrderedBoundRepresentation() {
        return new double[]{lowerBound, upperBound};
    }

    private void setValuesWithCenterSpreadRepresentation(double center, double spread) {
        this.center = center;
        this.spread = spread;
        this.lowerBound = center - spread;
        this.upperBound = center + spread;
    }

    private void setValuesWithOrderedBoundRepresentation(double lowerBound, double upperBound) {
        // TODO: Check case that spread is negative
        // Could be caused through Unorderd Bound Representation
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.center = this.lowerBound + ((this.upperBound - this.lowerBound) / 2);
        this.spread = Math.abs(this.upperBound - this.center);
    }

    public boolean equals(RealValueAllele allele) {
        double[] values = getSpecifiedRepresentation();
        double[] compValues = allele.getSpecifiedRepresentation();

        return values[0] == compValues[0] && values[1] == compValues[1];
    }

    public double[] getSpecifiedRepresentation() {
        switch (this.constants.usedGeneRepresentation) {
            case CENTER_SPREAD:
                return getCenterSpreadRepresentation();
            case ORDERED_BOUND:
                return getOrderedBoundRepresentation();
            case UNORDERED_BOUND:
                return getOrderedBoundRepresentation();
            default:
                return getOrderedBoundRepresentation();
        }
    }

    public boolean subsumes(RealValueAllele allele) {
        switch (this.constants.usedGeneRepresentation) {
            case CENTER_SPREAD:
                return subsumesCenterSpread(allele);
            case ORDERED_BOUND:
                return subsumesOrderedBound(allele);
            case UNORDERED_BOUND:
                return subsumesUnorderedBound(allele);
            default:
                return subsumesOrderedBound(allele);
        }
    }

    private boolean subsumesCenterSpread(RealValueAllele allele) {
        double[] values = getSpecifiedRepresentation();
        double[] compValues = allele.getSpecifiedRepresentation();

        return (values[0] - values[1]) <= (compValues[0] - compValues[1]) &&
                (values[0] + values[1]) >= (compValues[0] + compValues[1]);
    }

    private boolean subsumesOrderedBound(RealValueAllele allele) {
        double[] values = getSpecifiedRepresentation();
        double[] compValues = allele.getSpecifiedRepresentation();

        return values[0] <= compValues[0] && values[1] >= compValues[1];
    }

    private boolean subsumesUnorderedBound(RealValueAllele allele) {
        double[] values = getSpecifiedRepresentation();
        double[] compValues = allele.getSpecifiedRepresentation();
        double endpoint_left, endpoint_right, comp_endpoint_left, comp_endpoint_right;

        if (values[0] <= values[1]) {
            endpoint_left = values[0];
            endpoint_right = values[1];
        } else {
            endpoint_left = values[1];
            endpoint_right = values[0];
        }

        if (compValues[0] <= compValues[1]) {
            comp_endpoint_left = compValues[0];
            comp_endpoint_right = compValues[1];
        } else {
            comp_endpoint_left = compValues[1];
            comp_endpoint_right = compValues[0];
        }

        return endpoint_left <= comp_endpoint_left && endpoint_right >= comp_endpoint_right;
    }

    private void shrinkToBounds() {
        // TODO: Think about Center-Spread representation
        boolean shrinkNecessary = false;

        if (this.constants.usedGeneRepresentation == AlleleRepresentations.CENTER_SPREAD) {
            double endpoint_right = this.center + this.spread;
            double endpoint_left = this.center - this.spread;

            if (endpoint_left < this.constants.minPhenotypeValue) {
                shrinkNecessary = true;
                endpoint_left = this.constants.minPhenotypeValue;
            } else if (endpoint_left > this.constants.maxPhenotypeValue) {
                shrinkNecessary = true;
                endpoint_left = this.constants.maxPhenotypeValue;
            }

            if (endpoint_right < this.constants.minPhenotypeValue) {
                shrinkNecessary = true;
                endpoint_right = this.constants.minPhenotypeValue;
            } else if (endpoint_right > this.constants.maxPhenotypeValue) {
                shrinkNecessary = true;
                endpoint_right = this.constants.maxPhenotypeValue;
            }

            if (shrinkNecessary) {
                setValuesWithOrderedBoundRepresentation(endpoint_left, endpoint_right);
            }
        } else {
            // Valid for Ordered and Unorderd Bound Representation
            if (this.lowerBound < this.constants.minPhenotypeValue) {
                shrinkNecessary = true;
                this.lowerBound = this.constants.minPhenotypeValue;
            } else if (this.lowerBound > this.constants.maxPhenotypeValue) {
                shrinkNecessary = true;
                this.lowerBound = this.constants.maxPhenotypeValue;
            }

            if (this.upperBound < this.constants.minPhenotypeValue) {
                shrinkNecessary = true;
                this.upperBound = this.constants.minPhenotypeValue;
            } else if (this.upperBound > this.constants.maxPhenotypeValue) {
                shrinkNecessary = true;
                this.upperBound = this.constants.maxPhenotypeValue;
            }

            if (shrinkNecessary) {
                setValuesWithOrderedBoundRepresentation(this.lowerBound, this.upperBound);
            }
        }
    }

    public enum AlleleRepresentations {
        CENTER_SPREAD, ORDERED_BOUND, UNORDERED_BOUND
    }
}
