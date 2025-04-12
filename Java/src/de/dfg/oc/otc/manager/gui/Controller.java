package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.layer1.observer.DetectorObserver;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;
import de.dfg.oc.otc.publictransport.PublicTransportLine;
import de.dfg.oc.otc.publictransport.PublicTransportManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * This Controller manages Events of the OTCManager-GUI.
 */
class Controller implements ActionListener, Observer {
    /**
     * Ein Observer f�r alle Detektoren im gesamten Netzwerk. Wird nur f�r die
     * GUI gebraucht, daher hier.
     */
    private final DetectorObserver detectorObserver;
    private final OTCManager otcManager;
    private MainFrame gui;
    private String guiTitle = "";

    /**
     * Generates an instance of Controller.
     */
    Controller() {
        otcManager = OTCManager.getInstance();
        otcManager.addObserver(this);
        detectorObserver = new DetectorObserver();
    }

    void speedUpSimulation() {
        otcManager.setLayer2Busy(false);
    }

    /**
     * Gets actionEvent from the gui and distinguish according to actionCommand.
     *
     * @param arg0 The ActionEvent.
     */
    public final void actionPerformed(final ActionEvent arg0) {
        final String command = arg0.getActionCommand();
        switch (command) {
            case "exit":
                otcManager.deleteObserver(this);
                gui.dispose();
                break;
            case "shutdown":
                System.exit(0);
            case "do_print_network":
                printNetworkToScreen();
                break;
            case "printStatisticalData":
                printStatisticalData();
                break;
            case "do_printSitDiagrFile":
                gui.printSituationDataToFile();
                break;
            case "do_triggerL2": {
                final int[] junctionIds = new int[otcManager.getNetwork().getNumControlledJunctions()];
                final List<AimsunJunction> junctions = otcManager.getNetwork().getControlledJunctions();
                int i = 0;

                for (AimsunJunction junction : junctions) {
                    junctionIds[i] = junction.getId();
                    i++;
                }
                TriggerL2Dialog dialog = new TriggerL2Dialog(gui, junctionIds);
                dialog.setVisible(true);
                break;
            }
            case "do_unbusy":
                speedUpSimulation();
                break;
            case "do_saveLCSpopulation": {
                final Collection<OTCNode> nodesIter = otcManager.getNetwork().getNodes();
                final SaveLCSPopulationDialog dialog = new SaveLCSPopulationDialog(gui, nodesIter);
                dialog.setVisible(true);
                break;
            }
            case "do_loadLCSpopulation": {
                final Collection<OTCNode> nodesIter = otcManager.getNetwork().getNodes();
                final LoadLCSPopulationDialog dialog = new LoadLCSPopulationDialog(gui, nodesIter);
                dialog.setVisible(true);
                break;
            }
            case "do_viewTurnStats":
                final TurningStatisticsFrame frame = new TurningStatisticsFrame(otcManager.getNetwork());
                frame.setVisible(true);
                break;
            case "do_viewDetectorData":
                final DetectorDataFrame detDataFrame = new DetectorDataFrame(detectorObserver);
                detDataFrame.setVisible(true);
                break;
            case "changeLAF":
                setLookAndFeel();
                break;
        }
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.err.println(e.getMessage());
        }
        SwingUtilities.updateComponentTreeUI(gui);
    }

    /**
     * Checks whether unprocessed Messages are available (that were generated
     * before the GUI's Console registered as observer).
     */
    final void checkMessages() {
        final Exception exception = otcManager.getException();
        if (exception != null) {
            gui.printException(exception);
        }

        String text = otcManager.getWarning();
        if (text.length() > 2) {
            gui.printWarning(text);
        }

        text = otcManager.getInfo();
        if (text.length() > 2) {
            gui.printInfo(text);
        }
    }

    /**
     * Gibt den DetectorObserver für alle Detektoren im gesamten Netzwerk
     * zurück.
     *
     * @return DetectorObserver
     */
    final DetectorObserver getDetectorObserver() {
        return detectorObserver;
    }

    /**
     * Gets the network from the OTCManager, opens a new window and prints a
     * listing of network elements to that window.
     */
    private void printNetworkToScreen() {
        final NetworkFrame frame = new NetworkFrame();

        try {
            frame.setText(otcManager.getNetwork().toString());
        } catch (NullPointerException npe) {
            frame.setText("No network data received");
        }

        frame.setVisible(true);
    }

    private void printStatisticalData() {
        FileOutputStream file;
        PrintStream printStream;
        try {
            file = new FileOutputStream("logs/statdata.txt");
            printStream = new PrintStream(file);
        } catch (FileNotFoundException e1) {
            gui.printException(e1);
            return;
        }

        printStream.print(otcManager.getNetwork().printStatisticalData());
        printStream.close();
        try {
            file.close();
        } catch (IOException e) {
            gui.printException(e);
        }
    }

    /**
     * Registers the OTCManager-gui-frame at the controller.
     *
     * @param gui The MainFrame that will be controlled.
     */
    final void setControlledFrame(final JFrame gui) {
        this.gui = (MainFrame) gui;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    public final void update(final Observable o, final Object arg) {
        final String message = (String) arg;
        try {
            switch (message) {
                case "Network loaded":
                    if (guiTitle.isEmpty()) {
                        guiTitle = gui.getTitle();
                    }
                    gui.setTitle(guiTitle + " " + otcManager.getNetwork().getName());
                    gui.resetNetwork();
                    break;
                case "Network initialised":
                    Collection<Detector> detectors = new ArrayList<>();
                    try {
                        detectors = otcManager.getNetwork().getDetectors().values();
                    } catch (OTCManagerException e) {
                        // Exception thrown means no detector present or
                        // initJunctions() not yet called
                        gui.setNodeDetectors();
                    }

                    for (Detector detector : detectors) {
                        gui.addNetworkComponent(detector);
                        detectorObserver.addDetector(detector);
                    }


                    final Collection<AimsunJunction> junctions = otcManager.getNetwork().getJunctions().values();
                    junctions.forEach(gui::addNetworkComponent);

                    final Collection<Section> sections = otcManager.getNetwork().getSections();
                    sections.forEach(gui::addNetworkComponent);

                    final Map<Integer, Centroid> centroids = otcManager.getNetwork().getCentroidMap();
                    centroids.values().forEach(gui::addNetworkComponent);

                    gui.addNetworkComponent(PublicTransportManager.getInstance());

                    final Collection<PublicTransportLine> lines = PublicTransportManager.getInstance().getPublicTransportLines().values();
                    lines.forEach(gui::addNetworkComponent);

                    if (otcManager.isLayer2Present()) {
                        gui.setLayer2Present(true);
                    }
                    break;
                case "Simulation restarted":
                    gui.restart();
                    break;
                case "Time updated":
                    gui.setTime(otcManager.getTime());
                    break;
                case "New Info":
                    gui.printInfo(otcManager.getInfo());
                    break;
                case "New Warning":
                    gui.printWarning(otcManager.getWarning());
                    break;
                case "New Exception":
                    gui.printException(otcManager.getException());
                    break;
                case "Layer2 Present":
                    if (otcManager.isLayer2Present()) {
                        gui.setLayer2Present(true);
                    }
                    break;
            }
        } catch (OTCManagerException ome) {
            gui.printException(ome);
        }
    }
}
