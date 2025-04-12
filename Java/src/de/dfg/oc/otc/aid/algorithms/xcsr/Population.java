package de.dfg.oc.otc.aid.algorithms.xcsr;

import java.util.LinkedHashSet;

/**
 * Created by Anthony on 08.09.2014.
 */
public class Population {
    private final LinkedHashSet<RClassifier> pop;
    private final int maxSize;

    public Population(int maxSize) {
        this.maxSize = maxSize;
        this.pop = new LinkedHashSet<>(maxSize);
    }

    void addClassifier(RClassifier cl_new) {
        while (getRealSize() + 1 > maxSize) {
            deleteClassifier();
        }

        for (RClassifier cl : this.pop) {
            if (cl.compareTo(cl_new) == 0) {
                cl.numerosity++;
                return;
            }
        }

        pop.add(cl_new);
    }

    void addClassifiers(LinkedHashSet<RClassifier> new_classifiers) {
        if (new_classifiers == null || new_classifiers.isEmpty()) {
            return;
        }

        new_classifiers.forEach(this::addClassifier);
    }

    private void deleteClassifier() {
        double meanFitness = determineMeanFitness();
        double voteSum = 0.0;

        for (RClassifier cl : this.pop) {
            voteSum += cl.getDeletionProbability(meanFitness);
        }

        double choicePoint = voteSum * XCSRConstants.drand();
        voteSum = 0.0;

        RClassifier cl_to_delete = null;

        for (RClassifier cl : this.pop) {
            voteSum += cl.getDeletionProbability(meanFitness);
            if (voteSum > choicePoint) {
                cl_to_delete = cl;
                break;
            }
        }

        if (cl_to_delete != null) {
            if (cl_to_delete.numerosity > 1) {
                cl_to_delete.numerosity--;
            } else {
                this.pop.remove(cl_to_delete);
            }
        }
    }

    private double determineMeanFitness() {
        double meanFitness = 0.0;
        for (RClassifier cl : this.pop) {
            if (Double.isNaN(cl.fitness)) {
                continue;
            }
            meanFitness += cl.fitness;
        }
        return meanFitness / getRealSize();
    }

    public LinkedHashSet<RClassifier> getCurrentPopulation() {
        return this.pop;
    }

    int getRealSize() {
        int sumNumerosity = 0;
        for (RClassifier cl : this.pop) {
            sumNumerosity += cl.numerosity;
        }
        return sumNumerosity;
    }
}
