package de.dfg.oc.otc.aid.algorithms.xcsrUrban.discovery_component;

import de.dfg.oc.otc.aid.algorithms.xcsrUrban.RClassifier;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrbanParameters;

import java.util.LinkedHashSet;

/**
 * Genetic algorithm to create new classifiers.
 * Created by Anthony on 09.09.2014.
 */
public class GA {
    private final LinkedHashSet<RClassifier> initialPopulation; // [P] XOR [M] XOR [A]
    private final LinkedHashSet<RClassifier> offspring;
    private final int numberActions;
    private final int time;
    private XCSRUrbanParameters constants;

    public GA(XCSRUrbanParameters constants, LinkedHashSet<RClassifier> pop, int time, int numberActions) {
        this.constants = constants;
        this.numberActions = numberActions;
        this.initialPopulation = pop;
        this.offspring = new LinkedHashSet<>();
        this.time = time;
    }

    public boolean run() {
//        if (this.initialPopulation.size() == 0)
//            return false;
        if ((this.time - getAverageTimeSinceLastGA()) <= this.constants.theta_GA) {
            return false;
        }

        this.initialPopulation.forEach(cl -> cl.timeStamp = time);

        double fitnessSum = getFitnessSum();

        RClassifier parent1 = selectParent(fitnessSum);
        RClassifier parent2 = selectParent(fitnessSum);

//        if (parent1.condition == null)
//            parent1.condition = null;
        RClassifier child1 = new RClassifier(this.constants, parent1);
        RClassifier child2 = new RClassifier(this.constants, parent2);

        twoPointCrossover(child1, child2);
        applyMutation(child1);
        applyMutation(child2);

        child1.prediction = (child1.prediction + child2.prediction) / 2;
        child1.prediction_error = this.constants.predictionErrorReduction * ((child1.prediction_error + child2.prediction_error) / 2);
        child1.fitness = this.constants.fitnessReduction * ((child1.fitness + child2.fitness) / 2);

        child2.prediction = child1.prediction;
        child2.prediction_error = child1.prediction_error;
        child2.fitness = child1.fitness;

        if (!subsumeClassifier(child1, parent1, parent2)) {
            this.offspring.add(child1);
        }
        if (!subsumeClassifier(child2, parent1, parent2)) {
            this.offspring.add(child2);
        }

        return !this.offspring.isEmpty();
    }

    private int getAverageTimeSinceLastGA() {
        int sumTimeStamp = 0;
        int numerostiySum = 0;
        for (RClassifier cl : this.initialPopulation) {
            sumTimeStamp += cl.timeStamp * cl.numerosity;
            numerostiySum += cl.numerosity;
        }
        if (numerostiySum == 0) {
//            System.out.println(0);
            return 0;
        }
        return sumTimeStamp / numerostiySum;
    }

    private double getFitnessSum() {
        double sumF = 0.0;
        for (RClassifier cl : this.initialPopulation) {
            sumF += cl.fitness;
        }
        return sumF;
    }

    private RClassifier selectParent(double fitnessSum) {
        //Roulette-wheel Selection
        double choicePoint = this.constants.drand() * fitnessSum;
        double summedF = 0.0;
        RClassifier parent = null;

        for (RClassifier cl : this.initialPopulation) {
            summedF += cl.fitness;
            if (summedF >= choicePoint) {
                parent = cl;
                break;
            }
        }
        return parent;
    }

    private void twoPointCrossover(RClassifier cl1, RClassifier cl2) {
        boolean changed = false;
        if (this.constants.drand() < this.constants.pX) {
            double[] p1_x_chromo = cl1.condition.getChromosomeForCrossover();
            double[] p2_x_chromo = cl2.condition.getChromosomeForCrossover();

            if (p1_x_chromo.length != p2_x_chromo.length) {
                return;
            }

            int length = p1_x_chromo.length;
            int sep1 = (int) (this.constants.drand() * (length));
            int sep2 = (int) (this.constants.drand() * (length)) + 1; // +1 because of int cast

            if (sep1 > sep2) {
                int help = sep1;
                sep1 = sep2;
                sep2 = help;
            } else if (sep1 == sep2) {
                sep2++;
            }

            for (int i = sep1; i < sep2; i++) {
                if (p1_x_chromo[i] != p2_x_chromo[i]) {
                    changed = true;
                    double help = p1_x_chromo[i];
                    p1_x_chromo[i] = p2_x_chromo[i];
                    p2_x_chromo[i] = help;
                }
            }

            if (changed) {
                cl1.condition.setChromosome(p1_x_chromo);
                cl2.condition.setChromosome(p2_x_chromo);
            }
        }
    }


    /**
     * Applies a niche mutation to the classifier.
     *
     * @param cl The classifier that shall be mutated.
     */
    private void applyMutation(RClassifier cl) {
        mutateCondition(cl);
        mutateAction(cl);
    }

    /**
     * Mutates the condition of the classifier. If one allele is mutated depends on the constant pM.
     * This mutation is a niche mutation. It assures that the resulting classifier
     * still matches the current situation.
     * <p>
     * NOTICE: Due to the changes needed to reach the goals of XCS no niche mutation takes place any more.
     *
     * @param cl The classifier that shall be mutated.
     */
    private void mutateCondition(RClassifier cl) {
        double[] chromo = cl.condition.getChromosomeForCrossover();

        if (chromo == null) {
            return;
        }

        for (int i = 0; i < chromo.length; i++) {
            if (this.constants.drand() < this.constants.pM) {
                chromo[i] += this.constants.getRandomMutationValue();
            }
        }
        cl.condition.setChromosome(chromo);
    }

    /**
     * Mutates the action of the classifier.
     */
    private void mutateAction(RClassifier cl) {
        if (this.constants.drand() < this.constants.pM) {
            int act;
            do {
                act = (int) (this.constants.drand() * numberActions);
            } while (act == cl.action);
            cl.action = act;
        }
    }

    public LinkedHashSet<RClassifier> getOffspring() {
        return this.offspring;
    }

    private boolean subsumeClassifier(RClassifier child, RClassifier par1, RClassifier par2) {
        if (child.action == par1.action) {
            if (par1.isSubsumer()) {
                if (par1.isMoreGeneral(child)) {
                    par1.numerosity++;
                    return true;
                }
            }
        }

        if (child.action == par2.action) {
            if (par2.isSubsumer()) {
                if (par2.isMoreGeneral(child)) {
                    par2.numerosity++;
                    return true;
                }
            }
        }

        return false;
    }
}
