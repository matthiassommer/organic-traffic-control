package de.dfg.oc.otc.aid.algorithms.xcsr.performance_component;

import de.dfg.oc.otc.aid.algorithms.xcsr.XCSRConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Anthony on 09.09.2014.
 */
public class ActionSelection {
    private final int action_execute;

    public ActionSelection(PredictionArray pa, ACTION_SELECTION_REGIME regime) {
        switch (regime) {
            case BEST_ACTION_WINNER:
                this.action_execute = determineBestActionWinner(pa);
                break;
            case ROULETTE_WHEEL:
                this.action_execute = determineRouletteWheelWinner(pa);
                break;
            case RANDOM:
                this.action_execute = determineRandomWinner(pa);
                break;
            default:
                this.action_execute = determineBestActionWinner(pa);
        }
    }

    /**
     * Returns the action with the highest fitness-weighted score.
     *
     * @param pa prediction array
     * @return chosen action index
     */
    private int determineBestActionWinner(PredictionArray pa) {
        double highestEntry = -1000.0;
        int indexOfHighestEntry = -1;
        for (int i = 0; i < pa.getPA().length; i++) {
            double entry = pa.getPA()[i];
            if (!Double.isNaN(entry)) {
                if (entry > highestEntry) {
                    highestEntry = entry;
                    indexOfHighestEntry = i;
                }
            }
        }
        return indexOfHighestEntry;
    }

    /**
     * Fitness-weighted selection.
     *
     * @param pa prediction array
     * @return chosen action index
     */
    private int determineRouletteWheelWinner(PredictionArray pa) {
        double bidSum = 0.0;
        int i;
        for (i = 0; i < pa.getPA().length; i++) {
            double p = pa.getPA()[i];
            if (!Double.isNaN(p))
                bidSum += p;
        }

        if (bidSum == 0) {
            return 0;
        }

        bidSum *= XCSRConstants.drand();
        double bidC = 0.;
        for (i = 0; bidC < bidSum; i++) {
            double p = pa.getPA()[i];
            if (!Double.isNaN(p))
                bidC += p;
        }
        return i - 1;
    }

    /**
     * Returns a random action from the prediction array.
     *
     * @param pa prediction array
     * @return chosen action index
     */
    private int determineRandomWinner(PredictionArray pa) {
        List<Integer> nonNilIndizes = new ArrayList<>();
        for (int i = 0; i < pa.getPA().length; i++) {
            if (!Double.isNaN(pa.getPA()[i])) {
                nonNilIndizes.add(i);
            }
        }
        return nonNilIndizes.get((int) ((XCSRConstants.drand() * nonNilIndizes.size())));
    }

    public int getActionToExecute() {
        return this.action_execute;
    }

    public enum ACTION_SELECTION_REGIME {
        BEST_ACTION_WINNER, ROULETTE_WHEEL, RANDOM
    }
}
