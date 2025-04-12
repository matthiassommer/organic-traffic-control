package de.dfg.oc.otc.layer1.observer.monitoring;

abstract class MeasurementCapabilities {
    final int NaN = 1;
    final int PREVIOUS = 3;
    final int ZERO = 2;
    int numFeatures = -1;

    /**
     * Liefert eine Beschreibung der vorhandenen Features.
     *
     * @param feature Das fragliche Feature
     * @return Beschreibung
     */
    public abstract String getDescription(int feature);

    public abstract int getFeatureNumber(String description);

    public final int getNumFeatures() {
        return numFeatures;
    }

    /**
     * Gibt zur�ck, ob die Messwerte f�r das angegebene Feature diskret sind
     * oder kontinuierlich. Flow und alle Zeitangaben werden immer dann
     * gemessen, wenn ein Fahrzeug das betrachtete Netzwerkobjekt verl�sst, sind
     * also diskret. Die R�ckstaul�nge wird kontinuierlich gemessen, jede
     * Ver�nderung ist in der Datenreihe sichtbar, sie ist also kontinuierlich.
     * Diese Information wird ben�tigt, um den Durchschnitt korrekt berechnen zu
     * k�nnen.
     *
     * @param feature Das fragliche Feature.
     * @return True, wenn Feature diskrete Werte liefert, false wenn
     * kontinuierlich.
     */
    public abstract boolean isVehicleBased(int feature);
}
