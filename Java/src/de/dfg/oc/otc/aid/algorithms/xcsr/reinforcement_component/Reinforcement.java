package de.dfg.oc.otc.aid.algorithms.xcsr.reinforcement_component;

import de.dfg.oc.otc.aid.algorithms.xcsr.RClassifier;
import de.dfg.oc.otc.aid.algorithms.xcsr.XCSRConstants;
import de.dfg.oc.otc.aid.algorithms.xcsr.performance_component.Matching;

import java.util.Set;

/**
 * Created by Anthony on 09.09.2014.
 */
public class Reinforcement {
    private final Set<RClassifier> actionSet;
    private final int receivedReward;

    public Reinforcement(int reward, int a_exec, Matching match) {
        this.receivedReward = reward;
        this.actionSet = match.generateActionSet(a_exec);
    }

    public void doReinforcement() {
        updatePredictionError();
        updatePrediction();
        updateActionSetSize();
        updateFitness();
    }

    private void updateFitness() {
        double accummulated_accuracy = 0.0;

        for (RClassifier cl : this.actionSet) {
            accummulated_accuracy += cl.getAccuracy() * cl.numerosity;
        }
        for (RClassifier cl : this.actionSet) {
            cl.fitness += XCSRConstants.beta * ((cl.getAccuracy() * cl.numerosity) / accummulated_accuracy - cl.fitness);
        }
    }

    private void updatePrediction() {
        for (RClassifier cl : this.actionSet) {
            if ((double) cl.experience < 1. / XCSRConstants.beta) {
                cl.prediction = (cl.prediction * ((double) cl.experience - 1.) + this.receivedReward) / (double) cl.experience;
            } else {
                cl.prediction += XCSRConstants.beta * (this.receivedReward - cl.prediction);
            }
        }
    }

    private void updatePredictionError() {
        for (RClassifier cl : this.actionSet) {
            if ((double) cl.experience < 1. / XCSRConstants.beta) {
                cl.prediction_error = (cl.prediction_error * ((double) cl.experience - 1.) + Math.abs(this.receivedReward - cl.prediction)) / (double) cl.experience;
            } else {
                cl.prediction_error += XCSRConstants.beta * (Math.abs(this.receivedReward - cl.prediction) - cl.prediction_error);
            }
        }
    }

    private void updateActionSetSize() {
        for (RClassifier cl : this.actionSet) {
            if (cl.experience < 1. / XCSRConstants.beta) {
                cl.asSize = (cl.asSize * (double) (cl.experience - 1) + getRealActionSetSize()) / (double) cl.experience;
            } else {
                cl.asSize += XCSRConstants.beta * (getRealActionSetSize() - cl.asSize);
            }
        }
    }

    private int getRealActionSetSize() {
        int size = 0;
        for (RClassifier cl : this.actionSet) {
            size += cl.numerosity;
        }
        return size;
    }
}
