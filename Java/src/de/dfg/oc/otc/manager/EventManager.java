package de.dfg.oc.otc.manager;

import de.dfg.oc.otc.config.DefaultParams;

import javax.swing.*;

/**
 * Loads the JavaNativeEvent.dll.
 */
abstract class EventManager {
    static {
        try {
            System.load(DefaultParams.PATH + "JavaNativeEvent.dll");
        } catch (UnsatisfiedLinkError ule) {
            JOptionPane.showMessageDialog(null, ule.getMessage(), "OTCManager: Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.err.println(ule.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Sobald eine Instanz des OTCManagers existiert,kann das Event abgesetzt werden
     * auf das die API-Seite wartet, bevor sie weiterarbeitet.
     */
    public static native void setEvent();
}