package de.dfg.oc.otc.manager.aimsun.detectors;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorCapabilities;
import de.dfg.oc.otc.layer1.observer.monitoring.SubDetectorValue;
import de.dfg.oc.otc.manager.OTCManagerException;

import java.util.Observable;
import java.util.Observer;

/**
 * Beschreibt eine Beziehung zwischen zwei Detektoren um den Verkehrfluss zu
 * messen. Normalerweise an einem Turning verwendet.
 */
public class CounterLoop extends Observable implements Observer {
    private SubDetector counterIn;
    private SubDetector counterOut;
    private int numberOfCarsInLoop;

    /**
     * Leerer CounterLoop notwendig.
     */
    public CounterLoop() {
        this.numberOfCarsInLoop = 1;
    }

    public CounterLoop(final SubDetector counterIn, final SubDetector counterOut) throws OTCManagerException {
        if (counterIn.getDetectorFeature() != DetectorCapabilities.COUNT
                || counterOut.getDetectorFeature() != DetectorCapabilities.COUNT) {
            throw new OTCManagerException("Einer der für die CounterLoop vorgesehenen SubDetectors ist kein Counter.");
        }

        this.counterIn = counterIn;
        this.counterOut = counterOut;
        this.counterIn.addObserver(this);
        this.counterOut.addObserver(this);
    }

    public final int getNumberOfCarsInLoop() {
        return numberOfCarsInLoop;
    }

    public final void update(final Observable o, final Object arg) throws OTCManagerException {
        final SubDetector subDetector = (SubDetector) o;
        if (arg == null) {
            // Es ist ein Neustart erfolgt, reset counter.
            this.numberOfCarsInLoop = 0;
            return;
        }

        final SubDetectorValue value = (SubDetectorValue) arg;

        if (subDetector == this.counterIn) {
            this.numberOfCarsInLoop += value.getValue();
        } else if (subDetector == this.counterOut) {
            this.numberOfCarsInLoop -= value.getValue();
            if (this.numberOfCarsInLoop < 0) {
                this.numberOfCarsInLoop = 0;
            }
        }
    }
}
