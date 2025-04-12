package de.dfg.oc.otc.layer2.ea;

import de.dfg.oc.otc.layer2.OTCLayer2AnnounceInterface;
import de.dfg.oc.otc.layer2.OptimisationResult;
import de.dfg.oc.otc.layer2.OptimisationTask;
import de.dfg.oc.otc.layer2.gui.Layer2Console;
import de.dfg.oc.otc.manager.OTCManager;

import javax.swing.*;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Die abstrakte Klasse {@code Server} dient als gemeinsame Oberklasse
 * für die auf Ebene 2 verwendeten Optimierungsserver. Sie kontaktiert bei ihrer
 * Erzeugung die {@code RMI-Registry} der Ebene 1 und meldet die Ebene 2
 * dort an. Ist Ebene 1 nicht vorhanden, wird eine neue
 * {@code RMI-Registry} erzeugt.
 *
 * @author hpr
 */
public class EAServer extends UnicastRemoteObject implements EAServerInterface {
    /**
     * Konsole zur Ausgabe von Informationen, Warnungen und Fehlermeldungen.
     */
    private final Layer2Console layer2Console;
    private EA ea;
    /**
     * Ist Ebene 1 vorhanden?
     */
    private boolean layer1Present;
    /**
     * Reference to RMI registry.
     */
    private Registry registry;
    /**
     * Server running the RMI registry.
     */
    private String serverName = "localhost";

    private EAServer() throws RemoteException {
        this.layer2Console = Layer2Console.getInstance();
        this.layer2Console.printServerInfo("Server started");
        // new EAInternalNEMA(this);
        // new EAExternalFTC(this);
        new EAInternalFTC(this);
    }

    /**
     * Die {@code run()}-Methode erzeugt eine Konsole zur Ausgabe von
     * Meldungen sowie einen Server, der auf neue Optimierungsaufgaben wartet.
     */
    public static void main(final String[] args) {
        OTCManager.setLayer2Attached();

        // Display status frame for Layer 2
        final Layer2Console console = Layer2Console.getInstance();
        console.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        console.setVisible(true);

        try {
            new EAServer();
        } catch (RemoteException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public final void addTask(final OptimisationTask task) {
        ea.startOptimisation(task);
    }

    final String getFilenamePrefix() {
        String filenamePrefix = "";

        try {
            final OTCLayer2AnnounceInterface remoteReference = (OTCLayer2AnnounceInterface) registry.lookup("Layer2Announce");
            filenamePrefix = remoteReference.getFilenamePrefix();
        } catch (RemoteException | NotBoundException e) {
            layer2Console.printServerWarning(e.getMessage());
        }

        return filenamePrefix;
    }

    final boolean isLayer1Present() {
        return layer1Present;
    }

    @Override
    public final int registerEAAtLayer1(final EA ea) {
        this.ea = ea;
        int eaId;

        do {
            try {
                this.registry = LocateRegistry.getRegistry(serverName);
                final OTCLayer2AnnounceInterface remoteReference = (OTCLayer2AnnounceInterface) registry.lookup("Layer2Announce");
                eaId = remoteReference.announce(this);
                this.layer1Present = true;
                break;
            } catch (AccessException e) {
                this.layer2Console.printServerWarning(e.getMessage());
            } catch (RemoteException e) {
                this.serverName = JOptionPane
                        .showInputDialog("Please enter the name or IP-address of the server running the registry.");
                if (this.serverName == null) {
                    // Cancel
                    System.exit(-1);
                }
            } catch (NotBoundException e) {
                layer2Console.printServerWarning("Layer 1 not present.");
            }
        } while (true);

        this.ea.setEaId(eaId);
        return eaId;
    }

    @Override
    public final void returnOptimisationResult(final OptimisationResult optimisationResult) {
        try {
            final OTCLayer2AnnounceInterface l2rStub = (OTCLayer2AnnounceInterface) registry.lookup("Layer2Announce");
            l2rStub.pushResult(ea.getEaId(), optimisationResult);
        } catch (RemoteException | NotBoundException e) {
            layer2Console.printEAWarning(e.getMessage());
        }
    }
}
