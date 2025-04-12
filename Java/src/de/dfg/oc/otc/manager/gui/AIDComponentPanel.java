package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.aid.AIDModule;
import de.dfg.oc.otc.aid.AIDMonitoringZone;
import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.evaluation.IncidentEvaluationListener;
import de.dfg.oc.otc.manager.OTCNode;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Main panel for displaying aid information.
 */
@SuppressWarnings("serial")
public class AIDComponentPanel extends JPanel implements Observer, IncidentEvaluationListener {
    /**
     * Current monitoring zone panels for selected junction. Key =
     * monitoringZoneID, Value = panel.
     */
    private final Map<Integer, AIDMonitoringZonePanel> monitoringZonePanels;
    /**
     * Map of currently monitored nodes. Key = monitoringZoneID, Value = node.
     */
    private final Map<Integer, OTCNode> monitoredNodes;
    /**
     * List of all reported incidents.
     */
    private final List<Incident> reportedIncidents;
    /**
     * Panel for detailed information of a node. Consists of monitoring zone
     * panels.
     */
    private JPanel nodePanel;
    /**
     * Panel for overview over all incidents independent of their node.
     */
    private JPanel networkPanel;
    /**
     * Scroll panel for node / network view.
     */
    private JScrollPane scrollPane;
    /**
     * Currently selected node (from junction dropdown). Null if 'all' is
     * selected.
     */
    private OTCNode selectedNode;

    AIDComponentPanel() {
        super();

        monitoredNodes = new HashMap<>(5);
        monitoringZonePanels = new HashMap<>(5);
        reportedIncidents = new ArrayList<>();

        this.setLayout(new BorderLayout());
        createContentPanel();
    }

    /**
     * Creates the run content panel.
     */
    private void createContentPanel() {
        nodePanel = new JPanel();
        nodePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        networkPanel = new JPanel();
        networkPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        scrollPane = new JScrollPane();
        scrollPane.setBorder(null);

        this.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Switches the content of the panel.
     *
     * @param content of the panel
     */
    private void switchContent(Component content) {
        scrollPane.setViewportView(content);
        repaint();
    }

    /**
     * Registers the gui as observer to the aid algorithms.
     *
     * @param node which is being monitored
     */
    void registerToNode(OTCNode node) {
        node.getAIDComponent().setGUI(this);

        // Add node to monitored nodes
        monitoredNodes.put(node.getId(), node);
    }

    /**
     * This method is called once an aid algorithm detects a new incident.
     *
     * @param observable Algorithm which reported the incident
     * @param object     Incident which has been reported
     */
    @Override
    public void update(Observable observable, Object object) {
        Incident incident = (Incident) object;

        if (selectedNode != null && selectedNode.getId() == incident.getJunctionID()) {
            // Add incident to MonitoringZone Panel
            monitoringZonePanels.get(incident.getMonitoringZoneID()).addIncident(incident);

            repaint();
        } else if (selectedNode == null) {
            // Add incident to Network Panel
            addIncidentToNetworkPanel(incident);
        }

        reportedIncidents.add(incident);
    }

    /**
     * Updates the node panel to display the information for the selected node.
     *
     * @param node which is being selected
     */
    void updateNodePanel(OTCNode node) {
        this.selectedNode = node;
        this.monitoringZonePanels.clear();

        this.nodePanel.removeAll();
        this.nodePanel.setLayout(new GridLayout(0, 1, 0, 20));

        List<AIDMonitoringZone> monitoringZones = node.getAIDComponent().getMonitoringZones();

        if (monitoringZones.isEmpty()) {
            this.nodePanel.add(new JLabel("The node of the selected junction has no monitoring zones - Monitoring is not possible"));
        } else {
            // Create panel for each monitoring zone
            for (AIDMonitoringZone monitoringZone : monitoringZones) {
                AIDMonitoringZonePanel mzPanel = new AIDMonitoringZonePanel(monitoringZone);
                mzPanel.addIncidentEvaluationListener(this);

                // Add incidents to panel
                monitoringZone.getAIDAlgorithm().getDetectedIncidents().forEach(mzPanel::addIncident);

                this.nodePanel.add(mzPanel);
                this.monitoringZonePanels.put(monitoringZone.getId(), mzPanel);
            }
        }

        switchContent(this.nodePanel);
    }

    /**
     * Updates the network view (all incidents).
     */
    void updateNetworkPanel() {
        selectedNode = null;

        networkPanel.removeAll();
        networkPanel.setLayout(new BoxLayout(networkPanel, BoxLayout.PAGE_AXIS));

        final JLabel caption = new JLabel("All reported incidents");
        final Font font = new Font("Verdana", Font.BOLD, 15);
        caption.setFont(font);
        caption.setForeground(new java.awt.Color(72, 148, 39));
        networkPanel.add(caption);

        reportedIncidents.forEach(this::addIncidentToNetworkPanel);

        switchContent(networkPanel);
    }

    /**
     * Adds an incident to the overview panel.
     *
     * @param incident which is added to the overview panel
     */
    private void addIncidentToNetworkPanel(Incident incident) {
        AIDIncidentPanel incidentPanel = new AIDIncidentPanel(incident, true);
        incidentPanel.addIncidentEvaluationListener(this);
        networkPanel.add(incidentPanel, 1);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the incident has been evaluated for the first time then the
     * corresponding algorithm evaluator is informed about it.
     */
    @Override
    public void incidentEvaluated(Incident incident, boolean firstEvaluation) {
        int junctionID = incident.getJunctionID();
        int monitoringZoneID = incident.getMonitoringZoneID();

        AIDModule aidModule = monitoredNodes.get(junctionID).getAIDComponent();
        AIDMonitoringZone monitoringZone = aidModule.getMonitoringZone(monitoringZoneID);

        // Incident has been evaluated for the first time
        if (firstEvaluation) {
            monitoringZone.getEvaluator().addEvaluatedIncident(incident);
        }

        // Single node view has been selected
        if (selectedNode != null && selectedNode.getId() == junctionID) {
            AIDMonitoringZonePanel mzPanel = monitoringZonePanels.get(monitoringZoneID);
            mzPanel.updateAlgorithmInfo();
        }
    }
}
