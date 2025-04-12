package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.layer0.tlc.AbstractTLC;
import de.dfg.oc.otc.publictransport.PublicTransportComponent;
import de.dfg.oc.otc.publictransport.PublicTransportController;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;

/**
 * Panel shows informations about the public transport in the network.
 * Design orients at {@Link ForecastComponentPanel}.
 *
 * @author Matthias Sommer
 */
class PublicTransportComponentPanel extends JPanel implements Observer {
    /**
     * * A mapping for the PublicTransportController ID and the corresponding routingTable.
     */
    private Map<PublicTransportController, JTable> publicTransportTableMapping;
    private Map<PublicTransportController, JLabel> defaultTLCLabelMapping;
    private Map<PublicTransportController, JLabel> activeTLCLabelMapping;
    private Map<PublicTransportController, JLabel> deniedLabelMapping;
    private Map<PublicTransportController, JLabel> numSignalChangesLabelMapping;

    /**
     * This method creates a JTable for the given {@Link PublicTransportComponent}
     * and puts it in publicTransportTableMapping.
     *
     * @param controller the {@Link PublicTransportComponent}
     * @return the created panel containing all the JTables
     */
    private JPanel createSignalChangeTablePanel(PublicTransportController controller) {
        JTable table = createTLCTable(new String[]{"Time", "Event"});

        final JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.PAGE_AXIS));
        tablePanel.add(table.getTableHeader());
        tablePanel.add(table);

        this.publicTransportTableMapping.put(controller, table);

        return tablePanel;
    }

    /**
     * Create a panel with informations on the chosen public transport controller.
     *
     * @param controller
     * @return new JPanel
     */
    private JPanel createInfoPanel(final PublicTransportController controller) {
        final JPanel panel = new JPanel();

        final String header = "<html><b>PublicTransportController at junction " + controller.getJunction().getId() + " in section " + controller.getSection().getId() + "</b></html>";
        final TitledBorder border = BorderFactory.createTitledBorder(header);
        panel.setBorder(border);

        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        // Label for default TLC
        JLabel defaultTLClabel = new JLabel();
        defaultTLClabel.setToolTipText("Interphases are colored in red, green phases are colored in green.");
        defaultTLClabel.setText(formatText(controller.getIDsOfGreenPhases(), "DefaultTLC", controller.getDefaultTLC()));
        this.defaultTLCLabelMapping.put(controller, defaultTLClabel);
        panel.add(defaultTLClabel);

        //Label for active TLC
        JLabel activeTLClabel = new JLabel();
        activeTLClabel.setToolTipText("Interphases are colored in red, green phases are colored in green");
        activeTLClabel.setText(formatText(controller.getIDsOfGreenPhases(), "Active TLC", controller.getDefaultTLC()));
        this.activeTLCLabelMapping.put(controller, activeTLClabel);
        panel.add(activeTLClabel);

        panel.add(new JLabel("<html><b>Cycle time: " + controller.getDefaultTLC().getCycleTime() + "s</b></html>"));
        panel.add(new JLabel("<html><b>Detector distance to junction: " + Math.round(controller.getDetectorDistanceToExit()) + " m</b></html>"));
        panel.add(new JLabel(" "));

        panel.add(new JLabel(getFeatureSetting(controller)));
        panel.add(new JLabel(" "));

        JLabel appliedLabel = new JLabel();
        appliedLabel.setToolTipText("The total number of phase changes.");
        appliedLabel.setText("<html><b>Phase Changes: 0</b></html>");
        this.numSignalChangesLabelMapping.put(controller, appliedLabel);
        panel.add(appliedLabel);

        JLabel deniedLabel = new JLabel();
        deniedLabel.setToolTipText("The number of phase changes that were denied because it was not allowed yet.");
        deniedLabel.setText("<html><b>Denied Phase Changes: 0</b></html>");
        this.deniedLabelMapping.put(controller, deniedLabel);
        panel.add(deniedLabel);

        return panel;
    }

    /**
     * Creates a text about the enabled features of PublicTransport.
     *
     * @param controller
     * @return text as String
     */
    private String getFeatureSetting(PublicTransportController controller) {
        String text = "<html><b>";

        boolean[] features = controller.getFeatureSetting();
        if (features[0]) {
            text += "Phase Length Adjustment is enabled.";
        } else {
            text += "Phase Length Adjustment is disabled.";
        }
        text += "<br>";

        if (features[1]) {
            text += "Phase Rotation is enabled.";
        } else {
            text += "Phase Rotation is disabled.";
        }
        text += "</b></html>";

        return text;
    }

    /**
     * Creates a String text containing all phases of the default TLC colorcoded.
     *
     * @param greens the IDs of the Greenphases
     * @param start  shown name of the TLC
     * @param tlc    the TLC to shown as text (default or active)
     * @return a String text of start followed by the Greentimes of the TLC
     */
    private String formatText(Collection<Integer> greens, String start, AbstractTLC tlc) {
        boolean[] isInterphases = tlc.getParameters().getIsInterPhase();
        float[] greenTimes = tlc.getParameters().getGreenTimes();
        int[] ids = tlc.getParameters().getIds();

        String text = "<html><b>" + start;
        for (int i = 0; i < greenTimes.length; i++) {
            text += ", ";
            if (isInterphases[i]) {
                text += "<font color='red'>" + Math.round(greenTimes[i] * 10) / 10.0 + "s</font>";
            } else if (greens.contains(ids[i + 1])) {
                text += "<font color='green'>" + Math.round(greenTimes[i] * 10) / 10.0 + "s</font>";
            } else {
                text += Math.round(greenTimes[i] * 10) / 10.0 + "s";
            }
        }
        text += "</b></html>";

        return text;
    }

    /**
     * Create a GUI for the Line showing all the information.
     *
     * @param controllers the given PublicTransportControllers
     */
    private void createPublicTransportControllerGUI(final Iterable<PublicTransportController> controllers) {
        for (PublicTransportController controller : controllers) {
            final PublicTransportComponent publicTransportComponent = controller.getPublicTransportComponent();
            if (publicTransportComponent == null) {
                return;
            }

            publicTransportComponent.registerObserver(this);

            // create Panel for each PublicTransportController
            final JPanel infoPanel = createInfoPanel(controller);
            add(infoPanel);

            final JPanel tablePanel = createSignalChangeTablePanel(controller);
            add(tablePanel);
        }
    }

    /**
     * Create a JTable for showing the TLC changes of a PublicTransport Controller.
     *
     * @return table, created JTable
     */
    private JTable createTLCTable(final String[] header) {
        final DefaultTableModel tableModel = new MyDefaultTableModel(header);

        final JTable table = new JTable(tableModel);
        table.setShowVerticalLines(true);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        return table;
    }

    /**
     * Sets the layout for the PublicTransportControllers.
     */
    final void setPublicTransportControllers(final Collection<PublicTransportController> controllers) {
        int size = controllers.size();
        this.publicTransportTableMapping = new HashMap<>(size);
        this.defaultTLCLabelMapping = new HashMap<>(size);
        this.activeTLCLabelMapping = new HashMap<>(size);
        this.deniedLabelMapping = new HashMap<>(size);
        this.numSignalChangesLabelMapping = new HashMap<>(size);

        this.removeAll();
        this.setLayout(new GridLayout(controllers.size(), 2));

        createPublicTransportControllerGUI(controllers);
    }

    /**
     * Update method of Observer.
     *
     * @param o   the PublicTransportComponent
     * @param arg Object holding the parameters ( 3 parameters)
     */
    @Override
    public void update(Observable o, Object arg) {
        final Object[] entry = (Object[]) arg;

        updatePublicTransportControllerTablePanel((PublicTransportController) entry[0], (float) entry[1], (String) entry[2]);
        updatePublicTransportInfoPanel((PublicTransportController) entry[0]);
        removeLastRows((PublicTransportController) entry[0]);
    }

    /**
     * Updates the Info Panel at the left side, the default TLC, the active TLC and how many TLC changes were applied/denied by the PublicTransportController.
     *
     * @param controller
     */
    private void updatePublicTransportInfoPanel(PublicTransportController controller) {
        final JLabel defaultTLCLabel = this.defaultTLCLabelMapping.get(controller);
        defaultTLCLabel.setText(formatText(controller.getIDsOfGreenPhases(), "Default TLC", controller.getDefaultTLC()));

        final JLabel activeTLCLabel = this.activeTLCLabelMapping.get(controller);
        activeTLCLabel.setText(formatText(controller.getIDsOfGreenPhases(), "Active TLC ", controller.getJunction().getActiveTLC()));

        final JLabel deniedLabel = this.deniedLabelMapping.get(controller);
        deniedLabel.setText("<html><b>Denied phase changes: " + controller.getDeniedPhaseChanges() + "</b></html>");

        final JLabel numSignalChangesLabel = this.numSignalChangesLabelMapping.get(controller);
        numSignalChangesLabel.setText("<html><b>Phase changes: " + controller.getNumberOfPhaseChanges() + "</b></html>");
    }

    /**
     * Adds a new entry (row) to the table.
     *
     * @param controller {@Link TLCChangeComponent}
     * @param time       the simulation time
     * @param message    a message of the occured event
     */
    private void updatePublicTransportControllerTablePanel(final PublicTransportController controller, final float time, final String message) {
        final JTable table = this.publicTransportTableMapping.get(controller);
        final DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        tableModel.addRow(new String[]{"<html><b>" + String.valueOf(time) + "</b></html>", String.valueOf(message)});
    }

    /**
     * Remove the oldest (first) row from all tables if the number of maximum rows is reached.
     *
     * @param controller the PublicTransportController
     */
    private void removeLastRows(final PublicTransportController controller) {
        final DefaultTableModel tableModel = (DefaultTableModel) this.publicTransportTableMapping.get(controller).getModel();
        // The number of rows that shall be displayed within the TLC Tables.
        if (tableModel.getRowCount() > 10) {
            tableModel.removeRow(0);
        }
    }

    /**
     * Remove Observer of this ComponentPanel from the PublicTransportComponents of the given PublicTransportControllers.
     *
     * @param controllers
     */
    public void removeObserversFromControllers(Iterable<PublicTransportController> controllers) {
        for (PublicTransportController controller : controllers) {
            controller.getPublicTransportComponent().removeObserver(this);
        }
    }

    private static class MyDefaultTableModel extends DefaultTableModel {
        public MyDefaultTableModel(String[] header) {
            super(null, header);
        }

        @Override
        public boolean isCellEditable(final int row, final int column) {
            return false;
        }
    }
}
