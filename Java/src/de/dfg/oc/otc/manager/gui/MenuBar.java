package de.dfg.oc.otc.manager.gui;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * Creates the menu for the gui.
 *
 * @author Matthias Sommer
 */
class MenuBar {
    private final Controller controller;
    private JMenuItem triggerLayer2;

    MenuBar(final Controller controller) {
        this.controller = controller;
    }

    private JMenu createActionMenu() {
        JMenu menu = new JMenu("Action");
        menu.add(getActionManualL2NonBusy());
        menu.add(createTriggerLayer2());
        menu.add(getViewSimL2());
        menu.add(getSaveLCSpopulation());
        menu.add(getLoadLCSpopulation());
        return menu;
    }

    /**
     * This method initializes fileMenu.
     *
     * @return javax.swing.JMenu
     */
    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(getFileExit());
        fileMenu.add(getFileShutDown());
        fileMenu.setMnemonic(KeyEvent.VK_UNDEFINED);
        return fileMenu;
    }

    /**
     * This method initializes the MenuBar.
     *
     * @return javax.swing.JMenuBar
     */
    JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createPreferencesMenu());
        menuBar.add(createActionMenu());
        return menuBar;
    }

    /**
     * This method initializes preferencesMenu.
     *
     * @return javax.swing.JMenu
     */
    private JMenu createPreferencesMenu() {
        JMenu menu = new JMenu("Preferences");
        menu.add(getLookAndFeelAction());
        return menu;
    }

    private JMenuItem getLookAndFeelAction() {
        JMenuItem item = new JMenuItem("Change to metal look");
        item.setActionCommand("changeLAF");
        item.addActionListener(controller);
        return item;
    }

    /**
     * This method initializes viewMenu.
     *
     * @return javax.swing.JMenu
     */
    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        menu.add(getViewPrintNetwork());
        menu.add(getViewAction());
        menu.add(getViewTurningStatistics());
        menu.add(getViewDetectorData());
        menu.add(getPrintSituationDiagrammToFile());
        menu.setMnemonic(KeyEvent.VK_UNDEFINED);
        return menu;
    }

    private JMenuItem getActionManualL2NonBusy() {
        JMenuItem item = new JMenuItem("Speed up simulation");
        item.setActionCommand("do_unbusy");
        item.addActionListener(controller);
        item.setEnabled(true);
        return item;
    }

    private JMenuItem getFileExit() {
        JMenuItem item = new JMenuItem("Exit");
        item.setActionCommand("exit");
        item.addActionListener(controller);
        return item;
    }

    private JMenuItem getFileShutDown() {
        JMenuItem item = new JMenuItem("Shutdown");
        item.setActionCommand("shutdown");
        item.addActionListener(controller);
        item.setIcon(new ImageIcon("icons/shutdown.png"));
        item.setToolTipText("Terminates GUI and the whole system including the RMI-Registry");
        return item;
    }

    private JMenuItem getLoadLCSpopulation() {
        JMenuItem item = new JMenuItem("Load TLC mapping");
        item.setActionCommand("do_loadLCSpopulation");
        item.addActionListener(controller);
        return item;
    }

    private JMenuItem getPrintSituationDiagrammToFile() {
        JMenuItem item = new JMenuItem("Print situation data to file");
        item.setActionCommand("do_printSitDiagrFile");
        item.addActionListener(controller);
        return item;
    }

    private JMenuItem getSaveLCSpopulation() {
        JMenuItem item = new JMenuItem("Save TLC mapping");
        item.setActionCommand("do_saveLCSpopulation");
        item.addActionListener(controller);
        return item;
    }

    private JMenuItem createTriggerLayer2() {
        triggerLayer2 = new JMenuItem("Trigger Layer 2");
        triggerLayer2.setActionCommand("do_triggerL2");
        triggerLayer2.addActionListener(controller);
        triggerLayer2.setEnabled(false);
        return triggerLayer2;
    }

    JMenuItem getTriggerLayer2() {
        return triggerLayer2;
    }

    private JMenuItem getViewAction() {
        JMenuItem item = new JMenuItem("Print statistical data");
        item.setActionCommand("printStatisticalData");
        item.addActionListener(controller);
        return item;
    }

    private JMenuItem getViewDetectorData() {
        JMenuItem item = new JMenuItem("Show detector data");
        item.setActionCommand("do_viewDetectorData");
        item.addActionListener(controller);
        return item;
    }

    private JMenuItem getViewPrintNetwork() {
        JMenuItem item = new JMenuItem("Show network data");
        item.setActionCommand("do_print_network");
        item.addActionListener(controller);
        return item;
    }

    private JMenuItem getViewSimL2() {
        JMenuItem item = new JMenuItem("Simulate Layer 2");
        item.setActionCommand("do_simL2");
        item.addActionListener(controller);
        return item;
    }

    private JMenuItem getViewTurningStatistics() {
        JMenuItem item = new JMenuItem("Show turning statistics");
        item.setActionCommand("do_viewTurnStats");
        item.addActionListener(controller);
        return item;
    }
}
