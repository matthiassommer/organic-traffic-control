package de.dfg.oc.otc.aid.algorithms.xcsr.performance_component;

import de.dfg.oc.otc.aid.algorithms.xcsr.Population;
import de.dfg.oc.otc.aid.algorithms.xcsr.RClassifier;
import de.dfg.oc.otc.aid.algorithms.xcsr.Situation;

import java.util.LinkedHashSet;

/**
 * Created by Anthony on 09.09.2014.
 */
public class Matching {
    private final LinkedHashSet<RClassifier> matchSet;
    private final Population population;
    private final int numAct;
    private LinkedHashSet<RClassifier> actionSet;

    public Matching(Situation sigma_t, Population pop, int numAct) {
        this.population = pop;
        this.numAct = numAct;
        this.matchSet = createMatchSet(sigma_t);
    }

    /**
     * Checks every classifier of the population for a matching condition.
     *
     * @param sigma_t input vector
     * @return list of matching classifiers
     */
    private LinkedHashSet<RClassifier> createMatchSet(Situation sigma_t) {
        LinkedHashSet<RClassifier> ms = new LinkedHashSet<>();
        for (RClassifier cl : this.population.getCurrentPopulation()) {
            if (cl.match(sigma_t.getFeatureVector())) {
                ms.add(cl);
            }
        }
        return ms;
    }

    public LinkedHashSet<RClassifier> getMatchSet() {
        return this.matchSet;
    }

    /**
     * False if no classifier for action is available, true otherwise.
     *
     * @return boolean vector
     */
    public boolean[] getMissingActionsInMatchSet() {
        boolean[] actions = new boolean[this.numAct];
        for (int i = 0; i < this.numAct; i++)
            actions[i] = false;


        if (this.matchSet != null) {
            for (RClassifier cl : this.matchSet) {
                actions[cl.action] = true;
            }
        }
        return actions;
    }

    public LinkedHashSet<RClassifier> generateActionSet(int a_exec) {
        //If [A] has already been created!
        if (this.actionSet != null) {
            return actionSet;
        }

        this.actionSet = new LinkedHashSet<>();

        this.matchSet.stream().filter(cl -> cl.action == a_exec).forEach(cl -> {
            actionSet.add(cl);
            cl.experience++;
        });

        return actionSet;
    }
}
