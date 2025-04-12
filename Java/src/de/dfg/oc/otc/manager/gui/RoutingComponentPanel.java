package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.aid.disturbance.Disturbance;
import de.dfg.oc.otc.aid.disturbance.DisturbanceManager;
import de.dfg.oc.otc.routing.RoutingComponent;
import de.dfg.oc.otc.routing.distanceVector.RegionalDistanceVectorRC;
import de.dfg.oc.otc.routing.distanceVector.RegionalTDVRRC;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Panel for the routing components view.
 *
 * @author Matthias Sommer
 */
class RoutingComponentPanel extends JPanel implements Observer {
    private final JLabel delayCalculation = new JLabel();
    private final JPanel disturbancePanel = new JPanel();
    /**
     * A mapping for the inSection ID and the corresponding routingTable
     * {@code JTable} object.
     */
    private final Map<Integer, JTable> inSectionRoutingTableMapping = new HashMap<>();
    /**
     * Maps the unique insection - target pair (String) to the corresponding row
     * number in the corresponding routingTable, therefore the row numbers can
     * appear more than once.
     */
    private final Map<String, Integer> inSectionTargetToRowMapping = new HashMap<>(6);
    private final JLabel neighbourNodes = new JLabel();
    private final JLabel protocolType = new JLabel();
    private final JLabel regionalRCType = new JLabel();
    private final JLabel regionID = new JLabel();
    private RoutingComponent routingComponent;
    private JPanel routingTablePanel;
    private final JLabel messageCounter = new JLabel("0");

    RoutingComponentPanel() {
        super();
        this.setLayout(new BorderLayout());
        createGUIElements();
    }

    /**
     * Adds this class to the Observer-List of each Routing Table for every
     * existent in section.
     */
    private void addObserversToRoutingTables() {
        for (Integer inSectionID : this.routingComponent.getInSectionIDs()) {
            this.routingComponent.getRoutingTableForSection(inSectionID).addObserver(this);
        }
    }

    private void addRoutingTableForInSectionToPanel(int inSection) {
        this.routingTablePanel.add(createRoutingTableForInSection(inSection));
    }

    /**
     * Creates a single disturbance entry as a single panel for the predefined
     * Box Layout.
     *
     * @param description A {@code String} object containing the disturbance
     *                    information.
     * @return A {@code JPanel} object representing the disturbance
     * information.
     */
    private JPanel createDistrurbancePanelEntry(final String description) {
        final JPanel distPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        final ImageIcon icon = new ImageIcon("icons/build.png");
        final JLabel distIconLabel = new JLabel(icon);

        // Description String parsing
        String shortenedString = description.replace("No turnings affected. ", "");
        if (shortenedString.equals(description)) {
            shortenedString = description.replace(", No sections affected. ", "");
        }

        final JLabel distDescriptionLabel = new JLabel(shortenedString);

        distPanel.add(distIconLabel);
        distPanel.add(distDescriptionLabel);

        return distPanel;
    }

    /**
     * Creates all gui elements for this view.
     */
    private void createGUIElements() {
        final JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 5, 10, 5);

        int pos = 0;

        c.gridx = 0;
        c.gridy = pos;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        final JLabel caption = new JLabel("Displays informations for the routing component of the chosen junction");
        final Font font = new Font("Verdana", Font.BOLD, 14);
        caption.setFont(font);
        caption.setForeground(new java.awt.Color(72, 148, 39));
        panel.add(caption, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>Protocol type</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.protocolType, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>Neighbour nodes</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.neighbourNodes, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>Regional RC type</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.regionalRCType, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>Delay calculation method</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.delayCalculation, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>Region ID</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.regionID, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        panel.add(new JLabel("<html><b>Disturbances</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.disturbancePanel, c);
        this.disturbancePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        this.disturbancePanel.setLayout(new BoxLayout(this.disturbancePanel, BoxLayout.Y_AXIS));

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>Message counter (network wide)</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.messageCounter, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        panel.add(new JLabel("<html><b>Routing tables</b></html>"), c);
        c.gridwidth = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.BELOW_BASELINE_TRAILING;
        panel.add(createRoutingTablePanel(), c);

        add(panel, BorderLayout.NORTH);
    }

    private JPanel createRoutingTableForInSection(final int inSection) {
        final DefaultTableModel tableModel = new MyDefaultTableModel();

        final JTable table = new JTable(tableModel);
        table.setShowVerticalLines(false);

        this.inSectionRoutingTableMapping.put(inSection, table);

        final JPanel routingTablePanel = new JPanel();
        routingTablePanel.setLayout(new BoxLayout(routingTablePanel, BoxLayout.Y_AXIS));
        routingTablePanel.add(table.getTableHeader());
        routingTablePanel.add(table);
        routingTablePanel.setBorder(BorderFactory.createTitledBorder("Incoming Section: " + inSection));

        return routingTablePanel;
    }

    private JPanel createRoutingTablePanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        this.routingTablePanel = panel;
        return panel;
    }

    /**
     * Removes this class from the Observer-List of each Routing Table for every
     * existent in section.
     */
    final void removeObserversFromRoutingTables() {
        if (this.routingComponent != null) {
            for (Integer inSectionID : this.routingComponent.getInSectionIDs()) {
                this.routingComponent.getRoutingTableForSection(inSectionID).deleteObserver(this);
            }
        }
    }

    /**
     * Provides the row number of the data entry with the passed unique
     * inSection-Target combination within the table.
     *
     * @param inSectionTarget The unique inSection-Target String pair
     * @return An integer value representing the row
     */
    private int getRowOfEntry(final String inSectionTarget) {
        return this.inSectionTargetToRowMapping.get(inSectionTarget) - 1;
    }

    /**
     * Checks whether the passed string pair built from the in-section id and
     * the target-id is already visible or existent in the table / tableModel.
     *
     * @param inSectionTarget key for the Map
     * @return A boolean value indicating whether the passed pair is already
     * present or not.
     */
    private boolean isTableContainingEntry(final String inSectionTarget) {
        for (String entry : this.inSectionTargetToRowMapping.keySet()) {
            if (entry.equals(inSectionTarget)) {
                return true;
            }
        }
        return false;
    }

    private void loadDisturbances() {
        this.disturbancePanel.removeAll();

        final List<Disturbance> disturbances = DisturbanceManager.getInstance().getDisturbances();
        for (Disturbance disturbance : disturbances) {
            disturbancePanel.add(createDistrurbancePanelEntry(disturbance.getDescription()));
        }
    }

    private void updateMessageCounter() {
        this.messageCounter.setText(String.valueOf(this.routingComponent.getCommunicationCounter()));
    }

    /**
     * Reset tables, reset RoutingTablePanel and remove observers for old
     * routing component.
     */
    private void resetTables() {
        removeObserversFromRoutingTables();
        this.inSectionTargetToRowMapping.clear();
        this.inSectionRoutingTableMapping.clear();
        this.routingTablePanel.removeAll();
    }

    private void setDelayCalculation() {
        if (this.routingComponent.getDelayCalculationMethod() == 1) {
            this.delayCalculation.setText("Webster");
        } else if (this.routingComponent.getDelayCalculationMethod() == 2) {
            this.delayCalculation.setText("Statistics");
        }
    }

    private void setNeighbourNodes() {
        String ids = "";
        for (RoutingComponent rc : this.routingComponent.getNeighbourRCs()) {
            ids += rc.getNodeID() + ", ";
        }
        this.neighbourNodes.setText(ids);
    }

    private void setProtocolType() {
        this.protocolType.setText(this.routingComponent.getProtocolType().toString());
    }

    private void setRegionalRCType() {
        this.regionalRCType.setText(this.routingComponent.getRegionalRCType().toString());
    }

    /**
     * Set new routing component and update the gui (labels etc.).
     *
     * @return routingComponent inactive?
     */
    final boolean setRoutingComponent(final RoutingComponent routingComponent) {
        if (this.routingComponent != null && this.routingComponent.equals(routingComponent)) {
            return false;
        }

        resetTables();

        this.inSectionRoutingTableMapping.clear();

        this.routingComponent = routingComponent;
        addObserversToRoutingTables();

        this.routingComponent.getInSectionIDs().forEach(this::addRoutingTableForInSectionToPanel);

        showRegionID();
        setProtocolType();
        setDelayCalculation();

        return true;
    }

    private void showRegionID() {
        if (routingComponent instanceof RegionalDistanceVectorRC || routingComponent instanceof RegionalTDVRRC) {
            this.regionID.setText(String.valueOf(routingComponent.getRegionId()));
        }
    }

    void showRoutingComponentInfo() {
        if (this.routingComponent != null) {
            setNeighbourNodes();
            setRegionalRCType();
        }

        loadDisturbances();
    }


    /**
     * Updates the {@code routingTable} JTable for the passed inSection.
     * <p>
     * When the new data entry (respectively the target id and the in-section
     * id) isn't already existent a new row is inserted at the end of the table.
     * <p>
     * In the case that the data entry is already present, the corresponding
     * data row is identified and modified partially (next hop and delay
     * values).
     *
     * @param arg routing table entry: insection, outsection, centroid id, costs
     */
    @Override
    public final void update(final Observable o, final Object arg) {
        if (this.isVisible()) {
            String[] data = (String[]) arg;
            final JTable table = this.inSectionRoutingTableMapping.get(Integer.valueOf(data[0]));
            final DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
            final String inSectionToTarget = data[0] + data[2];

            if (isTableContainingEntry(inSectionToTarget)) {
                // update table entry
                final int row = getRowOfEntry(inSectionToTarget);
                tableModel.setValueAt(data[2], row, 1);
                tableModel.setValueAt(data[3], row, 2);
            } else {
                // add new table entry
                // Data array without the inSection ID because it is already shown
                // in the title border!
                tableModel.addRow(new String[]{data[1], data[2], data[3]});
                int rowCount = tableModel.getRowCount();
                this.inSectionTargetToRowMapping.put(inSectionToTarget, rowCount);
            }

            updateMessageCounter();
        }
    }

    private static class MyDefaultTableModel extends DefaultTableModel {
        MyDefaultTableModel() {
            super(null, new String[]{"Next section", "Target", "Costs (sec)"});
        }

        @Override
        public boolean isCellEditable(final int row, final int column) {
            return false;
        }
    }
}
