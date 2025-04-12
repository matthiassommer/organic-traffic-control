package de.dfg.oc.otc.layer0.tlc;

/**
 * Wichtig für den Recall-Controller.
 * Ein Recall beschreibt die Möglichkeit, Phasen (unendlich) zu verlängern oder zu überspringen.
 * Kann für jede Phase einzeln gesetzt werden.
 */
public enum Recall {
    disable, max, min, no;

    public static Recall getRecallForId(final int id) {
        switch (id) {
            case 0:
                // Recall-Capability nicht möglich
                return Recall.disable;
            case 1:
                return Recall.no;
            case 2:
                return Recall.min;
            case 3:
                return Recall.max;
            default:
               throw new IllegalArgumentException("Recall type unkwown for this ID");
        }
    }
}
