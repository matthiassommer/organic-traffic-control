package de.dfg.oc.otc.aid.algorithms.xcsrUrban.continuous;

import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrbanParameters;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

/**
 * Created by Anthony Stein on 27.02.14.
 * <p>
 * Diese Klasse implementiert die nötige Funktioalität um reellwertgie Eingaben verarbeiten zu können.
 */
public class ContinuousCondition implements Serializable {
    // Dimensionen des Problemraums / der Situationsbeschreibung
    public final int dimension;
    // Der Condition-Teil eines Classifiers als Chromosom interpretiert
    private List<RealValueGene> chromosome;
    private XCSRUrbanParameters constants;

    public ContinuousCondition(XCSRUrbanParameters constants, int dimension) {
        this.constants = constants;
        this.chromosome = new Vector<>(dimension);
        this.dimension = dimension;
    }

    /**
     * Kopiert die übergebene <code>ContinuousCondition</code>
     *
     * @param oldCond Die zu kopierende Condition
     */
    public ContinuousCondition(XCSRUrbanParameters constants, ContinuousCondition oldCond) {
        this.constants = constants;
        this.chromosome = oldCond.chromosome;
        this.dimension = oldCond.dimension;
    }

    /**
     * Wandelt ein übergebenes Chromosom in in Array-Darstellung (für die Crossover und Mutate Operatoren nötig)
     * in die herkömmliche Repräsentation eines Vectors um und weist diesen der Objekt-Instanz zu.
     *
     * @param crossedChromo Das Chromosom in Array-Darstellung
     */
    public void setChromosome(double[] crossedChromo) {
        if (crossedChromo.length != this.dimension * 2) {
            return;
        }

        List<RealValueGene> newChromo = new Vector<>(dimension);

        for (int i = 1; i <= crossedChromo.length; i++) {
            if (i % 2 == 0) {
                newChromo.add(new RealValueGene(newChromo.size(), new RealValueAllele(this.constants, crossedChromo[i - 2], crossedChromo[i - 1], this.constants.usedGeneRepresentation)));
            }
        }
        this.chromosome = newChromo;
    }

    public void setGene(RealValueGene gene) {
        this.chromosome.add(gene);
    }

    /**
     * Liefert das Gen, das an dem übergebenen Lokus <code>locus</code> sitzt.
     *
     * @param locus Der Sitz des Gens innerhalb des Chromosoms
     * @return Das Gen als <code>RealValueGene</code> Instanz.
     */
    public RealValueGene getGene(int locus) {
        return this.chromosome.get(locus);
    }

    /**
     * Überprüft ob die übergebene <code>ContinuousCondition</code> identisch mit dieser Instanz ist
     *
     * @param cond Die auf Gleichheit zu überprüfenden Condition
     * @return Ein boolscher Wert, der aussagt, ob die beiden Conditions identisch sind oder nicht
     */
    public boolean equals(ContinuousCondition cond) {
        if (this.constants.usedGeneRepresentation == RealValueAllele.AlleleRepresentations.UNORDERED_BOUND) {
            return equalsUnorderdBound(cond);
        }

        if (this.dimension != cond.dimension) {
            return false;
        }

        for (int i = 0; i < this.dimension; i++) {
            double[] allele = getGene(i).getAllele().getSpecifiedRepresentation();
            double[] compareAllele = cond.getGene(i).getAllele().getSpecifiedRepresentation();

            if (allele[0] != compareAllele[0] || allele[1] != compareAllele[1])
                return false;
        }
        return true;
    }

    /**
     * Überprüft die übergebene <code>ContinuousCondition</code> auf Gleichheit, jedoch ohne
     * Beachtung der Ordnungs-Restriktion l_i < x < u_i. (Unordered Bound Representation UBR)
     *
     * @param cond Die auf Gleichheit zu überprüfende Condition
     * @return Ein boolscher Wert, der eine Aussage über die Gleichheit trifft.
     */
    private boolean equalsUnorderdBound(ContinuousCondition cond) {
        if (this.dimension != cond.dimension) {
            return false;
        }

        for (int i = 0; i < this.dimension; i++) {
            double[] allele = getGene(i).getAllele().getSpecifiedRepresentation();
            double[] compareAllele = cond.getGene(i).getAllele().getSpecifiedRepresentation();

            if ((allele[0] != compareAllele[0] && allele[0] != compareAllele[1]) ||
                    (allele[1] != compareAllele[1] && allele[1] != compareAllele[0])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Wandelt das Chromosom in eine Array-Darstellung um, die für die Anwendung der GA-Operatoren
     * Crossover und Mutation benötigt wird.
     *
     * @return Das Chromosom der Objekt-Instanz als <code>double[]</code>
     */
    public double[] getChromosomeForCrossover() {
        double[] chromo = new double[this.dimension * 2];
        int indexCounter = 0;
        for (int i = 0; i < this.chromosome.size(); i++) {
            for (int j = 0; j < 2; j++) {
                chromo[indexCounter] = chromosome.get(i).getAllele().getSpecifiedRepresentation()[j];
                indexCounter++;
            }
        }

        if (chromo.length != this.dimension * 2) {
            return null;
        }
        return chromo;
    }

    /**
     * Erstellt einen leicht lesbaren String des Chromosoms.
     *
     * @return Ein String, der die leicht verständliche textuelle Repräsentation enthält
     */
    public String printableString() {
        StringBuilder strBuf = new StringBuilder();
        double[] concatenatedChromosome = this.getChromosomeForCrossover();

        strBuf.append("|");

        for (int i = 0; i < concatenatedChromosome.length; i++) {
            if ((i + 1) % 2 == 0) {
                strBuf.append(concatenatedChromosome[i]);
                strBuf.append("]");
                strBuf.append("|");
            } else {
                strBuf.append("[");
                strBuf.append(concatenatedChromosome[i]);
                strBuf.append(";");
            }
        }
        return strBuf.toString();
    }
}
