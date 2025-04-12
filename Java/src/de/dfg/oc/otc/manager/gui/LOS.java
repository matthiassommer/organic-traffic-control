package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.manager.aimsun.TrafficType;

/**
 * Returns the level of service class for a criterion. A is best, F is worst.
 */
abstract class LOS {
    private static final int ERR = -1;
    private static final int A = 0;
    private static final int B = 1;
    private static final int C = 2;
    private static final int D = 3;
    private static final int E = 4;
    private static final int F = 5;

    /**
     * Method specifies the Service Classes for the evaluation criterion
     * 'MAXSTOPS' (maximal amount of stops).
     *
     * @param value : The current value calculated for the MAXSTOPS criterion
     * @return the classification value
     */
    private static int getClassForAverageStops(final float value) {
        if (value < 0) {
            return ERR;
        } else if (value < 1) {
            return A;
        } else if (value < 2) {
            return B;
        } else if (value < 4) {
            return C;
        } else if (value < 6) {
            return D;
        } else if (value < 8) {
            return E;
        }
        return F;
    }

    /**
     * Method specifies the Service Classes for the evaluation criterion
     * 'AVSTOPS' (average stops).
     *
     * @param value : The current value calculated for the AVSTOPS criterion
     * @return the classification value
     */
    private static int getClassForMaxStops(final float value) {
        if (value < 0) {
            return ERR;
        } else if (value < 1) {
            return A;
        } else if (value < 2) {
            return B;
        } else if (value < 4) {
            return C;
        } else if (value < 6) {
            return D;
        } else if (value < 8) {
            return E;
        }
        return F;
    }

    /**
     * Method specifies the Service Classes for the evaluation criterion
     * 'QUEUELENGTH' (amount of vehicles waiting within the queue).
     *
     * @param value : The current value calculated for the QUEUELENGTH criterion
     * @return the classification value
     */
    private static int getClassForQueueLength(final float value) {
        if (value < 0) {
            return ERR;
        } else if (value < 1) {
            return A;
        } else if (value < 2) {
            return B;
        } else if (value < 4) {
            return C;
        } else if (value < 6) {
            return D;
        } else if (value < 8) {
            return E;
        }
        return F;
    }

    /**
     * Method specifies the Service Classes for the evaluation criterion
     * 'UTILISATION' (degree of utilisation of the current node).
     *
     * @param value : The current value calculated for the UTILISATION criterion
     * @return the classification value
     */
    private static int getClassForUtilisation(final float value) {
        if (value < 0) {
            return ERR;
        } else if (value < 50) {
            return A;
        } else if (value < 70) {
            return B;
        } else if (value < 80) {
            return C;
        } else if (value < 90) {
            return D;
        } else if (value < 100) {
            return E;
        } else if (value >= 100) {
            return F;
        }
        return ERR;
    }

    /**
     * Returns classification of a criterion.
     *
     * @param criterion
     * @param value
     * @return class
     */
    static int getClassification(final Attribute criterion, final float value) {
        if (new Float(value).isNaN() || value < 0) {
            return ERR;
        }

        switch (criterion) {
            case AVSTOPS:
                return getClassForAverageStops(value);
            case LOS:
                return getLos(value, TrafficType.INDIVIDUAL_TRAFFIC);
            case MAXSTOPS:
                return getClassForMaxStops(value);
            case QUEUELENGTH:
                return getClassForQueueLength(value);
            case UTILISATION:
                return getClassForUtilisation(value);
        }

        return ERR;
    }

    /**
     * Methode gibt den Namen der Service-Klasse anhand der ID zur�ck.
     *
     * @param levelKey : Id der Service-Klasse
     * @return Der Name der Klasse
     */

    static char getLevelName(final int levelKey) {
        switch (levelKey) {
            case 0:
                return 'A';
            case 1:
                return 'B';
            case 2:
                return 'C';
            case 3:
                return 'D';
            case 4:
                return 'E';
            case 5:
                return 'F';
            default:
                return '?';
        }
    }

    /**
     * Die Methode gibt die jeweilige LoS Klasse in Abhängigkeit von dem
     * gemessenen LoS-Wert und des zugehörigen Verkehrstyps zurück.
     *
     * @param value   : gemessener LoS Wert
     * @param traffic : der Verkehrstyp
     * @return Klassifiezierung
     */
    static int getLos(final float value, final TrafficType traffic) {
        // check for invalid parameters
        if (Float.isNaN(value) || value < 0) {
            return ERR;
        }

        switch (traffic) {
            case PUBLIC_TRANSPORT:
                if (value <= 5) {
                    return A;
                } else if (value <= 15) {
                    return B;
                } else if (value <= 25) {
                    return C;
                } else if (value <= 40) {
                    return D;
                } else if (value <= 60) {
                    return E;
                }
            case INDIVIDUAL_TRAFFIC:
                if (value <= 20) {
                    return A;
                } else if (value <= 35) {
                    return B;
                } else if (value <= 50) {
                    return C;
                } else if (value <= 70) {
                    return D;
                } else if (value <= 100) {
                    return E;
                }
            case PEDESTRIANS:
                if (value <= 15) {
                    return A;
                } else if (value <= 20) {
                    return B;
                } else if (value <= 25) {
                    return C;
                } else if (value <= 30) {
                    return D;
                } else if (value <= 35) {
                    return E;
                }
            case CYCLISTS:
                if (value <= 15) {
                    return A;
                } else if (value <= 25) {
                    return B;
                } else if (value <= 35) {
                    return C;
                } else if (value <= 45) {
                    return D;
                } else if (value <= 60) {
                    return E;
                }
        }

        return F;
    }

    /**
     * Methode gibt eine vollst�ndige Beschreibung der jeweiligen LoS-Klasse
     * (identifiziert anhand �bergebenem ID-Wert) zur�ck.
     *
     * @param levelKey : ID-Wert der LoS-Klasse
     * @return Vollst�ndige Beschreibung
     */
    public static String toString(final int levelKey) {
        switch (levelKey) {
            case 0:
                return "LOS A";
            case 1:
                return "LOS B";
            case 2:
                return "LOS C";
            case 3:
                return "LOS D";
            case 4:
                return "LOS E";
            case 5:
                return "LOS F";
            default:
                throw new IllegalArgumentException("Unknown Service Level");
        }
    }
}
