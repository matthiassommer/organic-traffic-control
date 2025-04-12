package de.dfg.oc.otc.aid.algorithms.xcsrUrban.reinforcement_component;

import de.dfg.oc.otc.aid.algorithms.xcsrUrban.RClassifier;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrbanParameters;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.performance_component.Matching;

import java.util.Set;

/**
 * Created by Anthony on 09.09.2014.
 */
public class Reinforcement {
    private final Set<RClassifier> actionSet;
    private final int receivedReward;
    private XCSRUrbanParameters constants;

    public Reinforcement(XCSRUrbanParameters constants, int reward, int a_exec, Matching match) {
        this.constants = constants;
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
            cl.fitness += this.constants.beta * ((cl.getAccuracy() * cl.numerosity) / accummulated_accuracy - cl.fitness);
        }
    }

    private void updatePrediction() {
        for (RClassifier cl : this.actionSet) {
            if ((double) cl.experience < 1. / this.constants.beta) {
                cl.prediction = (cl.prediction * ((double) cl.experience - 1.) + this.receivedReward) / (double) cl.experience;
            } else {
                cl.prediction += this.constants.beta * (this.receivedReward - cl.prediction);
            }
        }
    }

    private void updatePredictionError() {
        for (RClassifier cl : this.actionSet) {
            if ((double) cl.experience < 1. / this.constants.beta) {
                cl.prediction_error = (cl.prediction_error * ((double) cl.experience - 1.) + Math.abs(this.receivedReward - cl.prediction)) / (double) cl.experience;
            } else {
                cl.prediction_error += this.constants.beta * (Math.abs(this.receivedReward - cl.prediction) - cl.prediction_error);
            }
        }
    }

    private void updateActionSetSize() {
        for (RClassifier cl : this.actionSet) {
            if (cl.experience < 1. / this.constants.beta) {
                cl.asSize = (cl.asSize * (double) (cl.experience - 1) + getRealActionSetSize()) / (double) cl.experience;
            } else {
                cl.asSize += this.constants.beta * (getRealActionSetSize() - cl.asSize);
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
