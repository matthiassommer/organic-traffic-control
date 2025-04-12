package de.dfg.oc.otc.publictransport;

import de.dfg.oc.otc.manager.OTCManager;

import java.util.Observable;
import java.util.Observer;

/**
 * This class holds information about the signal changes of the corresponding PublicTransportController.
 * The data is then used for showcasing in the GUI.
 * <p>
 * Created by Dominik on 31.07.2014.
 */
public class PublicTransportComponent extends Observable {
    /**
     * The ID of the corresponding junction
     */
    private final PublicTransportController publicTransportController;

    PublicTransportComponent(PublicTransportController controller) {
        this.publicTransportController = controller;
    }

    /**
     * Send data to Panel
     *
     * @param message The message to be shown in the JTable.
     */
    void sendMessageToGUI(String message) {
        setChanged();
        notifyObservers(new Object[]{publicTransportController, OTCManager.getInstance().getTime(), message});
    }

    public final void registerObserver(final Observer observer) {
        this.addObserver(observer);
    }

    public final void removeObserver(final Observer observer) {
        this.deleteObserver(observer);
    }
}
