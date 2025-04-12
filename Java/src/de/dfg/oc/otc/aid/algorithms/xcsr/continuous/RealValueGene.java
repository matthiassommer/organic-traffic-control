package de.dfg.oc.otc.aid.algorithms.xcsr.continuous;

/**
 * Created by Anthony Stein on 27.02.14.
 * <p>
 * Diese Klasse implementiert die Logik eines Gens innerhalb eines Chromosoms
 */
public class RealValueGene {
    /* Eigene Position innerhalb des Chromosoms */
    private final int locus;
    /* Der Wert/Zustand des Gens */
    private final RealValueAllele allele;

    public RealValueGene(int locus, RealValueAllele allele) {
        this.locus = locus;
        this.allele = allele;
    }

    public RealValueAllele getAllele() {
        return this.allele;
    }
}
