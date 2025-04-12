package tests.layer2;

import de.dfg.oc.otc.layer2.OTCLayer2Announce;
import de.dfg.oc.otc.manager.OTCManager;
import org.apache.log4j.Logger;
import org.junit.Test;

import javax.swing.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Start the client and gui for Layer 2 and connect with the remote registry.
 */
public class OTCLevel2Client {
    private final Logger log = Logger.getLogger(OTCManager.class);
    private Registry rmiRegistry;

    @Test
    public void run() {
        OTCLevel2Client l2client = new OTCLevel2Client();
        l2client.connectRegistry();
        l2client.registerRMIMethods();

        OTCLevel2ClientGUI gui = new OTCLevel2ClientGUI();
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui.setVisible(true);
    }

    private void connectRegistry() {
        try {
            rmiRegistry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        } catch (RemoteException e) {
            log.info("Konnte RMI-Registry nicht neu erzeugen. Teste, ob schon eine Registry läuft.");
            try {
                rmiRegistry = LocateRegistry.getRegistry();
            } catch (RemoteException e1) {
                log.warn("Start der RMI-Registry fehlgeschlagen. Exception folgt.");
                rmiRegistry = null;
                return;
            }
        }
        log.info("Kontakt mit Registry hergestellt.");
    }

    private void registerRMIMethods() {
        Remote layer2Announce = new OTCLayer2Announce();
        try {
            Remote layer2AnnounceStub = UnicastRemoteObject.exportObject(layer2Announce, 0);
            rmiRegistry.rebind("Layer2Announce", layer2AnnounceStub);
        } catch (RemoteException e) {
            log.warn("Anmelden der RMI-Methoden fehlgeschlagen: ", e);
        }
    }
}