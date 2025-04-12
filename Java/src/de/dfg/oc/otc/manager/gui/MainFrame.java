package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.layer1.observer.DetectorObserver;
import de.dfg.oc.otc.layer1.observer.monitoring.*;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import de.dfg.oc.otc.manager.aimsun.TrafficType;
import de.dfg.oc.otc.publictransport.PublicTransportController;
import de.dfg.oc.otc.publictransport.PublicTransportLine;
import de.dfg.oc.otc.publictransport.PublicTransportManager;
import de.dfg.oc.otc.routing.RoutingComponent;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Main frame for the OTC GUI.
 */
public class MainFrame extends JFrame {
    private AIDComponentPanel aidComponentPanel;
    private JComboBox<Object> aidComboBox;
    private TextConsole consolePane;
    private Controller controller;
    private JComboBox<Object> detectorComboBox;
    private JPanel detectorDataPanel;
    private DPSSComponentPanel dpssComponentPanel;
    private final JComboBox<Object> evaluationComboBox = (JComboBox<Object>) new JComboBox<>();
    private JPanel evaluationComponentPanel;
    private JPanel evaluationPanel;
    private LOSChartPanel losChartComponentPanel;
    private MenuBar menubar;
    private NetworkDataPane networkSplitPanel;
    /**
     * Der Node, der die aktuelle Junction kontrolliert, deren Daten gerade auf
     * dem Evaluation-Tab angezeigt werden.
     */
    private OTCNode node;
    /**
     * Der Observer, dessen Daten auf dem Detectors-Tab angezeigt werden.
     */
    private DetectorObserver observer;
    private JComboBox<Object> routingComboBox;

    private RoutingComponentPanel routingComponentPanel;
    private JPanel routingPanel;
    private SituationChartPanel situationFlowChartPanel;
    private SituationChartPanel situationSpeedChartPanel;
    private SituationChartPanel situationStopsChartPanel;
    /**
     * Status bar at the bottom of the frame.
     */
    private StatusBar statusBar;
    private int lastSelectedTab = -1;
    private PublicTransportComponentPanel publicTransportComponentPanel;
    private JPanel publicTransportPanel;
    private JComboBox<Object> publicTransportComboBox;
    private List<PublicTransportController> lastSelectedPublicTransportControllers;


    public MainFrame() {
        initialize();
    }

    final void addNetworkComponent(final Object component) {
        getNetworkSplitPanel().addNetworkComponent(component);

        if (component instanceof AimsunJunction) {
            final AimsunJunction junction = (AimsunJunction) component;
            final AimsunNetwork network = OTCManager.getInstance().getNetwork();

            initialiseDetectorCombobox(network, junction);
            initialiseEvaluationCombobox(network, junction);

            JunctionComboBoxEntry entry = new JunctionComboBoxEntry(junction);
            this.routingComboBox.addItem(entry);

            if (DefaultParams.AID_ACTIVE) {
                initializeAIDCombobox(network, junction);
                this.aidComponentPanel.registerToNode(junction.getNode());
            }
        }

        if (component instanceof PublicTransportLine || component instanceof PublicTransportManager) {
            this.publicTransportComboBox.addItem(new PublicTransportLineComboBoxEntry(component));
        }
    }

    private void initialiseDetectorCombobox(AimsunNetwork network, AimsunJunction junction) {
        if (!getDetectorsJunctionComboBox().isEnabled()) {
            this.detectorComboBox.removeAllItems();
            this.detectorComboBox.addItem(new JunctionComboBoxEntry(network));

            if (this.observer != null) {
                this.observer.clearAllObservers();
            }

            // Per Default werden alle Detektoren angezeigt, daher wird der
            // netzweite Observer eingetragen.
            this.observer = this.controller.getDetectorObserver();
            this.detectorComboBox.setEnabled(true);
            initializeDetectorDataPanel();
        }

        this.detectorComboBox.addItem(new JunctionComboBoxEntry(junction));
    }

    private void initialiseEvaluationCombobox(AimsunNetwork network, AimsunJunction junction) {
        if (!getEvaluationJunctionComboBox().isEnabled()) {
            this.evaluationComboBox.removeAllItems();
            this.evaluationComboBox.addItem(new JunctionComboBoxEntry(network));

            if (this.node != null) {
                this.node.getL1StatObserver().clearAllObservers();
            }

            this.evaluationComboBox.setEnabled(true);
            initializeEvaluationDataPanel();
        }

        this.evaluationComboBox.addItem(new JunctionComboBoxEntry(junction));
    }

    private void initializeAIDCombobox(AimsunNetwork network, AimsunJunction junction) {
        if (!getAIDComboBox().isEnabled()) {
            this.aidComboBox.removeAllItems();
            this.aidComboBox.addItem(new JunctionComboBoxEntry(network));
            this.aidComboBox.setEnabled(true);
            this.aidComponentPanel.updateNetworkPanel();
        }

        this.aidComboBox.addItem(new JunctionComboBoxEntry(junction));
    }

    /**
     * This method initializes evaluationJunctionComboBox.
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<Object> getAIDComboBox() {
        if (this.aidComboBox == null) {
            this.aidComboBox = new JComboBox<>();
            this.aidComboBox.setToolTipText("Choose junction for a detailed view of all related incidents or \"all\".");
            this.aidComboBox.addItem("No Network");
            this.aidComboBox.setEnabled(false);
            this.aidComboBox.addActionListener(e -> {
                if (!DefaultParams.AID_ACTIVE) {
                    return;
                }

                final JunctionComboBoxEntry selection = (JunctionComboBoxEntry) this.aidComboBox.getSelectedItem();

                if (selection == null) {
                    return;
                }

                if (selection.getNode() != null) {
                    this.aidComponentPanel.updateNodePanel(selection.getNode());
                } else {
                    this.aidComponentPanel.updateNetworkPanel();
                }
            });
        }
        return this.aidComboBox;
    }

    /**
     * Creates the panel for the AID view.
     *
     * @return the AID panel
     */
    private AIDComponentPanel createAIDPanel() {
        this.aidComponentPanel = new AIDComponentPanel();
        if (DefaultParams.AID_ACTIVE) {
            this.aidComponentPanel.add(getAIDComboBox(), BorderLayout.NORTH);
        }

        return this.aidComponentPanel;
    }

    private JScrollPane createConsoleScrollPane() {
        JScrollPane consoleScrollPane = new JScrollPane();
        consoleScrollPane.setViewportView(getConsolePane());
        return consoleScrollPane;
    }

    /**
     * This method initializes jContentPane.
     *
     * @return javax.swing.JPanel
     */
    private JPanel createContentPane() {
        final JPanel contentPane = new JPanel();
        contentPane.setBorder(null);
        contentPane.setLayout(new BorderLayout());
        contentPane.add(createTabbedPane(), BorderLayout.CENTER);
        contentPane.add(createStatusBar(), BorderLayout.SOUTH);

        return contentPane;
    }

    /**
     * This method initializes detectorDataScrollPane.
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane createDetectorDataScrollPane() {
        detectorDataPanel = new JPanel();
        final LayoutManager boxLayout = new BoxLayout(detectorDataPanel, BoxLayout.Y_AXIS);
        detectorDataPanel.setLayout(boxLayout);
        detectorDataPanel.add(Box.createVerticalGlue());

        JScrollPane detectorDataScrollPane = new JScrollPane();
        detectorDataScrollPane.setViewportView(detectorDataPanel);
        return detectorDataScrollPane;
    }

    /**
     * This method initializes detectorsPanel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel createDetectorsPanel() {
        JPanel detectorsPanel = new JPanel();
        detectorsPanel.setLayout(new BorderLayout());
        detectorsPanel.add(getDetectorsJunctionComboBox(), BorderLayout.NORTH);
        detectorsPanel.add(createDetectorDataScrollPane(), BorderLayout.CENTER);
        return detectorsPanel;
    }

    /**
     * Creates the panel for the DPSS view.
     *
     * @return the DPSS panel
     */
    private DPSSComponentPanel createDPSSPanel() {
        this.dpssComponentPanel = new DPSSComponentPanel();
        return dpssComponentPanel;
    }

    private EvaluationDataEntryPanel createEvaluationDataEntryPanel(final Attribute atrribute, final int id,
                                                                    final float value) {
        EvaluationDataEntryPanel panel = new EvaluationDataEntryPanel(atrribute);
        panel.setObjectID(id);
        panel.setCurrentValue(value);
        panel.setCurrentClassification(String.valueOf(LOS.getLevelName(LOS.getClassification(atrribute, value))));
        return panel;
    }

    private EvaluationDataEntryPanel createEvaluationDataEntryPanel(final Attribute attribute, final int curID,
                                                                    final float value, final String objID) {
        EvaluationDataEntryPanel panel = new EvaluationDataEntryPanel(attribute);
        panel.setObjectID(curID);
        panel.setCurrentValue(value);
        panel.setObservedObject(objID);
        panel.setCurrentClassification(String.valueOf(LOS.getLevelName(LOS.getClassification(attribute, value))));
        return panel;
    }

    /**
     * This method initializes detectorDataScrollPane.
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane createEvaluationDataScrollPane() {
        evaluationComponentPanel = new JPanel();
        final LayoutManager boxLayout = new BoxLayout(evaluationComponentPanel, BoxLayout.Y_AXIS);
        evaluationComponentPanel.setLayout(boxLayout);
        evaluationComponentPanel.add(Box.createVerticalGlue());

        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(evaluationComponentPanel);

        return scrollPane;
    }

    /**
     * Creates the panel for the routing view.
     *
     * @return the routing panel
     */
    private JScrollPane createRoutingComponentPanel() {
        routingComponentPanel = new RoutingComponentPanel();

        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(routingComponentPanel);

        return scrollPane;
    }

    private JScrollPane createPublicTransportComponentPanel() {
        this.publicTransportComponentPanel = new PublicTransportComponentPanel();

        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(publicTransportComponentPanel);

        return scrollPane;
    }

    /**
     * This method initializes myTabbedPane.
     *
     * @return javax.swing.JTabbedPane
     */
    private JTabbedPane createTabbedPane() {
        final JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("<html><p width='70' marginheight=5>Network</p></html>", new ImageIcon("icons/network.png"),
                getNetworkSplitPanel(), "General data on the network");
        tabs.addTab("<html><p width='70'>Detectors</p></html>", new ImageIcon("icons/signal.png"),
                createDetectorsPanel(), "Detailed information on detectors, current values");
        tabs.addTab("<html><p width='70'>Evaluation</p></html>", new ImageIcon("icons/graph.png"),
                getEvaluationPanel(), "Evaluation of current traffic situation on chosen junction");
        tabs.addTab("<html><p width='70'>DPSS</p></html>", new ImageIcon("icons/light.png"), createDPSSPanel(),
                "View the established DPSS");
        tabs.addTab("<html><p width='70'>Routing</p></html>", new ImageIcon("icons/target.png"), getRoutingPanel(),
                "General data of the routing mechanism");
        tabs.addTab("<html><p width='70'>Incidents</p></html>", new ImageIcon("icons/warning.png"), createAIDPanel(),
                "View and evaluate detected incidents.");
        tabs.addTab("<html><p width='70'>Public Transport</p></html>", new ImageIcon("icons/bus_small.png"), getPublicTransportPanel(),
                "Public transport data and logs.");

        // Wenn dieser Tab umbenannt wird, nicht mehr "Console" hei�t,
        // muss TextConsole.exceptionToConsole angepasst werden.
        tabs.addTab("<html><p width='60'>Console</p></html>", new ImageIcon("icons/form.png"),
                createConsoleScrollPane(), "Debug console");

        tabs.addChangeListener(e -> {
            if (lastSelectedTab == 4) {
                routingComponentPanel.removeObserversFromRoutingTables();
            }

            if (tabs.getSelectedIndex() == 3) {
                dpssComponentPanel.update();
            } else if (tabs.getSelectedIndex() == 4) {
                routingComponentPanel.showRoutingComponentInfo();
            }

            lastSelectedTab = tabs.getSelectedIndex();
        });

        return tabs;
    }

    private TextConsole getConsolePane() {
        if (consolePane == null) {
            consolePane = new TextConsole();
        }
        return consolePane;
    }

    /**
     * This method initializes detectorsJunctionComboBox.
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<Object> getDetectorsJunctionComboBox() {
        if (detectorComboBox == null) {
            detectorComboBox = new JComboBox<>();
            detectorComboBox.setToolTipText("Choose junction for a detailed view of all related detectors or \"all\".");
            detectorComboBox.addItem("No Network");
            detectorComboBox.setEnabled(false);
            detectorComboBox.addActionListener(e -> {
                if (detectorComboBox.getItemCount() < 1) {
                    return;
                }
                if (observer == null) {
                    return;
                }
                JunctionComboBoxEntry selection;
                if (detectorComboBox.getSelectedItem() != null
                        && detectorComboBox.getSelectedItem() instanceof JunctionComboBoxEntry) {
                    selection = (JunctionComboBoxEntry) detectorComboBox.getSelectedItem();
                } else {
                    return;
                }

                if (selection.observer == null) {
                    JOptionPane.showMessageDialog(null, "The junction does not have any detectors.");
                    return;
                }
                observer.clearAllObservers();
                observer = selection.observer;
                initializeDetectorDataPanel();
            });
        }
        return detectorComboBox;
    }

    /**
     * This method initializes evaluationJunctionComboBox.
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<Object> getEvaluationJunctionComboBox() {
        if (evaluationComboBox == null) {
            evaluationComboBox.setToolTipText("Choose one junction for detailed evaluation view or \"all\".");
            evaluationComboBox.addItem("No Network");
            evaluationComboBox.setEnabled(false);
            evaluationComboBox.addActionListener(e -> {
                JunctionComboBoxEntry selection;
                try {
                    selection = (JunctionComboBoxEntry) getEvaluationJunctionComboBox().getSelectedItem();
                } catch (Exception ex) {
                    return;
                }

                if (selection == null) {
                    return;
                }

                if (selection.getNode() == null) {
                    node = null;
                    try {
                        final Collection<OTCNode> nodes = OTCManager.getInstance().getNetwork().getNodes();
                        for (OTCNode node : nodes) {
                            node.getL1StatObserver().clearAllObservers();
                        }
                    } catch (NullPointerException npex) {
                        npex.getStackTrace();
                    }
                } else {
                    node = selection.getNode();
                    node.getL1StatObserver().clearAllObservers();
                }

                initializeEvaluationDataPanel();
            });
        }
        return evaluationComboBox;
    }

    /**
     * This method initializes evaluationPanel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getEvaluationPanel() {
        if (evaluationPanel == null) {
            evaluationPanel = new JPanel(new BorderLayout());
            evaluationPanel.add(getEvaluationJunctionComboBox(), BorderLayout.NORTH);
            evaluationPanel.add(createEvaluationDataScrollPane(), BorderLayout.CENTER);
        }
        return evaluationPanel;
    }

    /**
     * This method initialises networkSplitPanel.
     *
     * @return javax.swing.JSplitPane
     */
    private NetworkDataPane getNetworkSplitPanel() {
        if (this.networkSplitPanel == null) {
            this.networkSplitPanel = new NetworkDataPane();
        }
        return this.networkSplitPanel;
    }

    /**
     * This method initializes publicTransportComboBox.
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<Object> getPublicTransportComboBox() {
        if (this.publicTransportComboBox == null) {
            this.publicTransportComboBox = new JComboBox<>();
            this.publicTransportComboBox.setToolTipText("Choose one Line for detailed view.");
            this.publicTransportComboBox.addActionListener(e -> {
                final PublicTransportLineComboBoxEntry selection = (PublicTransportLineComboBoxEntry) this.publicTransportComboBox.getSelectedItem();

                if (selection == null) {
                    return;
                }

                // remove Observers from unused(unshown) PublicTransportControllers
                if (this.lastSelectedPublicTransportControllers != null) {
                    this.publicTransportComponentPanel.removeObserversFromControllers(this.lastSelectedPublicTransportControllers);
                }

                if (!selection.getPublicTransportControllers().isEmpty()) {
                    List<PublicTransportController> controllers = selection.getPublicTransportControllers();
                    this.lastSelectedPublicTransportControllers = controllers;
                    this.publicTransportComponentPanel.setPublicTransportControllers(controllers);
                } else {
                    this.publicTransportComponentPanel.removeAll();
                }
            });
        }
        return publicTransportComboBox;
    }

    /**
     * This method initializes evaluationJunctionComboBox.
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<Object> getRoutingComboBox() {
        if (this.routingComboBox == null) {
            this.routingComboBox = new JComboBox<>();
            this.routingComboBox.setToolTipText("Choose one junction for detailed evaluation view.");
            this.routingComboBox.addActionListener(e -> {
                final JunctionComboBoxEntry selection = (JunctionComboBoxEntry) this.routingComboBox.getSelectedItem();

                if (selection == null) {
                    return;
                }

                if (selection.getNode() != null) {
                    final RoutingComponent routingComponent = selection.getNode().getRoutingComponent();
                    if (routingComponent != null) {
                        if (routingComponentPanel.setRoutingComponent(routingComponent)) {
                            routingComponentPanel.showRoutingComponentInfo();
                        }
                    }
                }

            });
        }
        return routingComboBox;
    }

    /**
     * This method initializes the routing panel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getPublicTransportPanel() {
        if (publicTransportPanel == null) {
            publicTransportPanel = new JPanel(new BorderLayout());
            publicTransportPanel.add(getPublicTransportComboBox(), BorderLayout.NORTH);
            publicTransportPanel.add(createPublicTransportComponentPanel(), BorderLayout.CENTER);
        }
        return publicTransportPanel;
    }

    /**
     * This method initializes the routing panel.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getRoutingPanel() {
        if (routingPanel == null) {
            routingPanel = new JPanel(new BorderLayout());
            routingPanel.add(getRoutingComboBox(), BorderLayout.NORTH);
            routingPanel.add(createRoutingComponentPanel(), BorderLayout.CENTER);
        }
        return routingPanel;
    }

    private StatusBar createStatusBar() {
        this.statusBar = new StatusBar();
        return this.statusBar;
    }

    private void initialize() {
        controller = new Controller();

        if (DefaultParams.USE_GUI) {
            controller.setControlledFrame(this);

            this.menubar = new MenuBar(this.controller);
            setJMenuBar(menubar.createMenuBar());
            setContentPane(createContentPane());

            final int width = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
            final int height = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
            setSize(width / 2, height);

            controller.checkMessages();
        } else {
            controller.speedUpSimulation();
        }
    }

    private void initializeDetectorDataPanel() {
        detectorDataPanel.removeAll();

        if (observer == null) {
            throw new OTCManagerException("The observer of the mainFrame is not present.");
        }

        final Collection<DataStorage> storageIter = observer.getStorages();
        for (DataStorage storage : storageIter) {
            DetectorDataStorage theStorage = (DetectorDataStorage) storage;
            DetectorDataEntryPanel pddep;
            for (int i = 0; i < DetectorCapabilities.NUM; i++) {
                if (theStorage.getMappedFeature(i) == -1) {
                    // Feature nicht aktiv
                    continue;
                }
                if (theStorage.getMappedFeature(i) == DetectorCapabilities.PRESENCE) {
                    pddep = new DiscreteDetectorDataEntryPanel();
                } else {
                    pddep = new ContinuousDetectorDataEntryPanel();

                }

                pddep.setId(theStorage.getDetector().getSubDetector(i).getId());
                pddep.setDetectorId(theStorage.getDetector().getId());
                pddep.setSubDetectorFeature(i);

                try {
                    pddep.setCurrentDetectorValue(theStorage.getAverage(i, 30));
                    pddep.setAverageDetectorValue(theStorage.getLastRelevantDatum(i));
                } catch (OTCManagerException e) {
                    pddep.setCurrentDetectorValue(-1);
                    pddep.setAverageDetectorValue(-1);
                }

                pddep.setAlignmentX(0);
                detectorDataPanel.add(pddep);
                theStorage.addObserver(pddep);
            }
        }
        repaint();
    }

    private void initializeEvaluationDataPanel() {
        evaluationComponentPanel.removeAll();

        if (node != null) {
            createPanelForNode(node);
        } else {
            createPanelForNetwork();
        }
        evaluationComponentPanel.repaint();
        repaint();
    }

    private void createPanelForNetwork() {
        // noch keine grafische Auswertung für das Gesamtnetzwerk
        losChartComponentPanel = null;

        final Collection<OTCNode> nodes = OTCManager.getInstance().getNetwork().getNodes();
        for (OTCNode node : nodes) {
            float valueLOS = node.getEvaluation(DataSource.STATISTICS, Attribute.LOS,
                    TrafficType.INDIVIDUAL_TRAFFIC, false);
            float valueQueue = node.getEvaluation(DataSource.STATISTICS, Attribute.QUEUELENGTH,
                    TrafficType.INDIVIDUAL_TRAFFIC, false);
            float valueAverageStops = node.getEvaluation(DataSource.STATISTICS, Attribute.AVSTOPS,
                    TrafficType.INDIVIDUAL_TRAFFIC, false);
            float valueStop = node.getEvaluation(DataSource.STATISTICS, Attribute.MAXSTOPS,
                    TrafficType.INDIVIDUAL_TRAFFIC, false);
            float valueUtil = node.getEvaluation(DataSource.STATISTICS, Attribute.UTILISATION,
                    TrafficType.INDIVIDUAL_TRAFFIC, false);
            final int id = node.getId();
            String objID = "Junction " + id;

            EvaluationDataEntryPanel panelLOS = createEvaluationDataEntryPanel(Attribute.LOS, id, valueLOS,
                    objID);
            evaluationComponentPanel.add(panelLOS);

            EvaluationDataEntryPanel panelQueueLength = createEvaluationDataEntryPanel(Attribute.QUEUELENGTH, id,
                    valueQueue, objID);
            evaluationComponentPanel.add(panelQueueLength);

            EvaluationDataEntryPanel panelAverageStop = createEvaluationDataEntryPanel(Attribute.AVSTOPS, id,
                    valueAverageStops, objID);
            evaluationComponentPanel.add(panelAverageStop);

            EvaluationDataEntryPanel panelMaxStops = createEvaluationDataEntryPanel(Attribute.MAXSTOPS, id,
                    valueStop, objID);
            evaluationComponentPanel.add(panelMaxStops);

            EvaluationDataEntryPanel panelUtilisationDegree = createEvaluationDataEntryPanel(Attribute.UTILISATION, id,
                    valueUtil, objID);
            evaluationComponentPanel.add(panelUtilisationDegree);

            // Panels als Observer anbinden
            try {
                Collection<DataStorage> storages = node.getL1StatObserver().getStorages();
                for (DataStorage storage : storages) {
                    StatisticalDataStorage stor = (StatisticalDataStorage) storage;
                    stor.addObserver(panelLOS);
                    stor.addObserver(panelQueueLength);
                    stor.addObserver(panelAverageStop);
                    stor.addObserver(panelMaxStops);
                    stor.addObserver(panelUtilisationDegree);
                }
            } catch (NullPointerException npe) {
                JOptionPane.showMessageDialog(null, "There are no turnings for the current node!");
            }
        }
    }

    private void createPanelForNode(OTCNode node) {
        // First Panel(s) to be added is for LoS
        float valueQueue = node.getEvaluation(DataSource.STATISTICS, Attribute.QUEUELENGTH,
                TrafficType.INDIVIDUAL_TRAFFIC, false);
        float valueAverageStops = node.getEvaluation(DataSource.STATISTICS, Attribute.AVSTOPS,
                TrafficType.INDIVIDUAL_TRAFFIC, false);
        float valueStop = node.getEvaluation(DataSource.STATISTICS, Attribute.MAXSTOPS, TrafficType.INDIVIDUAL_TRAFFIC,
                false);
        float valueUtil = node.getEvaluation(DataSource.STATISTICS, Attribute.UTILISATION,
                TrafficType.INDIVIDUAL_TRAFFIC, false);
        final int nodeID = node.getId();

        EvaluationDataEntryPanel panelLOS = createLOSPanel(nodeID);

        EvaluationDataEntryPanel panelQueue = createEvaluationDataEntryPanel(Attribute.QUEUELENGTH, nodeID,
                valueQueue);
        evaluationComponentPanel.add(panelQueue);

        EvaluationDataEntryPanel panelAverageStops = createEvaluationDataEntryPanel(Attribute.AVSTOPS, nodeID,
                valueAverageStops);
        evaluationComponentPanel.add(panelAverageStops);

        EvaluationDataEntryPanel panelStops = createEvaluationDataEntryPanel(Attribute.MAXSTOPS, nodeID, valueStop);
        evaluationComponentPanel.add(panelStops);

        EvaluationDataEntryPanel panelUtilisationDegree = createEvaluationDataEntryPanel(Attribute.UTILISATION, nodeID,
                valueUtil);
        evaluationComponentPanel.add(panelUtilisationDegree);

        // Panels als Observer anbinden
        Collection<DataStorage> storages = node.getL1StatObserver().getStorages();
        for (DataStorage storage : storages) {
            StatisticalDataStorage stor = (StatisticalDataStorage) storage;
            stor.addObserver(panelLOS);
            stor.addObserver(panelQueue);
            stor.addObserver(panelAverageStops);
            stor.addObserver(panelStops);
            stor.addObserver(panelUtilisationDegree);
        }

        if (losChartComponentPanel == null || losChartComponentPanel.getNodeId() != node.getId()) {
            losChartComponentPanel = new LOSChartPanel(node);
            evaluationComponentPanel.add(losChartComponentPanel);
            try {
                situationFlowChartPanel = new SituationChartPanel(node, StatisticsCapabilities.FLOW);
                evaluationComponentPanel.add(situationFlowChartPanel);
                situationStopsChartPanel = new SituationChartPanel(node, StatisticsCapabilities.NUMSTOPS);
                evaluationComponentPanel.add(situationStopsChartPanel);
                situationSpeedChartPanel = new SituationChartPanel(node, StatisticsCapabilities.SPEED);
                evaluationComponentPanel.add(situationSpeedChartPanel);
            } catch (RuntimeException e) {
                printException(e);
            }
        }
    }

    private EvaluationDataEntryPanel createLOSPanel(int nodeID) {
        float los = node.getEvaluation(DataSource.STATISTICS, Attribute.LOS, TrafficType.INDIVIDUAL_TRAFFIC, false);

        EvaluationDataEntryPanel panelLOS = new EvaluationDataEntryPanel(Attribute.LOS);
        panelLOS.setObjectID(nodeID);
        panelLOS.setCurrentValue(los);
        panelLOS.setCurrentClassification(String.valueOf(LOS.getLevelName(LOS.getLos(los, TrafficType.INDIVIDUAL_TRAFFIC))));
        evaluationComponentPanel.add(panelLOS);

        return panelLOS;
    }

    final void printException(final Exception exception) {
        getConsolePane().exceptionToConsole(exception);
    }

    final void printInfo(final String text) {
        getConsolePane().infoToConsole(text);
    }

    final void printSituationDataToFile() {
        if (situationFlowChartPanel != null) {
            situationFlowChartPanel.printDataToFile("logs/SituationFlowChartData.txt");
        }

        if (situationSpeedChartPanel != null) {
            situationSpeedChartPanel.printDataToFile("logs/SituationSpeedChartData.txt");
        }

        if (situationStopsChartPanel != null) {
            situationStopsChartPanel.printDataToFile("logs/SituationStopsChartData.txt");
        }
    }

    final void printWarning(final String text) {
        getConsolePane().warnToConsole(text);
    }

    final void resetNetwork() {
        restart();

        getNetworkSplitPanel().clearDetectors();
        networkSplitPanel.clearJunctions();
        networkSplitPanel.clearSections();

        getDetectorsJunctionComboBox().removeAllItems();
        detectorComboBox.addItem("No Network");
        detectorComboBox.setEnabled(false);

        getEvaluationJunctionComboBox().removeAllItems();
        evaluationComboBox.addItem("No Network");
        evaluationComboBox.setEnabled(false);

        observer = null;
        node = null;
    }

    final void restart() {
        this.statusBar.changeText("0");
    }

    final void setLayer2Present(final boolean presence) {
        this.menubar.getTriggerLayer2().setEnabled(presence);
    }

    final void setNodeDetectors() {
        getNetworkSplitPanel().setDetectorNodeDescription("No detector on current network");
    }

    /**
     * Called in every time step.
     */
    final void setTime(final float time) {
        // do not update the statusbar in every step
        if (time % 15 == 0) {
            this.statusBar.changeText(String.valueOf(time));
        }

        if (getEvaluationPanel().isVisible()) {
            if (losChartComponentPanel != null) {
                losChartComponentPanel.evaluate(time);
            }

            if (situationFlowChartPanel != null) {
                situationFlowChartPanel.evaluate(time);
            }

            if (situationSpeedChartPanel != null) {
                situationSpeedChartPanel.evaluate(time);
            }

            if (situationStopsChartPanel != null) {
                situationStopsChartPanel.evaluate(time);
            }
        } else if (this.dpssComponentPanel.isVisible()) {
            this.dpssComponentPanel.updatePanel();
        }
    }

    private class JunctionComboBoxEntry {
        private String description = "";
        private DetectorObserver observer;
        private OTCNode node;

        JunctionComboBoxEntry(final Object entry) {
            if (entry instanceof AimsunNetwork) {
                this.description = "Network view";
                this.observer = controller.getDetectorObserver();
            } else if (entry instanceof AimsunJunction) {
                final AimsunJunction junction = (AimsunJunction) entry;
                if (junction.isControlled()) {
                    this.observer = junction.getNode().getL1DetectorObserver();
                    this.node = junction.getNode();
                    this.description = "Junction " + junction.getId();
                } else {
                    this.observer = null;
                    this.node = null;
                    this.description = "Junction " + junction.getId() + " (uncontrolled)";
                }
            } else {
                throw new OTCManagerException("Entry is of unknown type (neither AimsunJunction nor AimsunNetwork)");
            }
        }

        OTCNode getNode() {
            return this.node;
        }

        public String toString() {
            return this.description;
        }
    }

    /**
     * Class for the publicTransportComboBox
     */
    private static class PublicTransportLineComboBoxEntry {
        private String description = "";
        private PublicTransportLine line;
        private List<PublicTransportController> controllers = new ArrayList<>();

        PublicTransportLineComboBoxEntry(final Object entry) {
            if (entry instanceof PublicTransportManager) {
                PublicTransportManager manager = (PublicTransportManager) entry;
                if (manager.getPublicTransportLines().isEmpty()) {
                    this.description = "No Public Transport Lines in Simulation!";
                } else {
                    this.description = "All Lines";
                    this.line = null;
                    this.controllers.clear();
                    for (PublicTransportLine line : manager.getPublicTransportLines().values()) {
                        this.controllers.addAll(line.getPublicTransportControllers());
                    }
                }
            } else if (entry instanceof PublicTransportLine) {
                this.line = (PublicTransportLine) entry;
                if (line.getPublicTransportControllers() == null) {
                    this.description = "Line " + line.getID() + "has no controllers!";
                    this.controllers.clear();
                } else {
                    this.description = "Line " + line.getID();
                    this.controllers = line.getPublicTransportControllers();
                }
            } else {
                throw new OTCManagerException("Entry is of unknown type, no PublicTransportLine");
            }
        }

        List<PublicTransportController> getPublicTransportControllers() {
            return this.controllers;
        }

        public String toString() {
            return this.description;
        }
    }
}
