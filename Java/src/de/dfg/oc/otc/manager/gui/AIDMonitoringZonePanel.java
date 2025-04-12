package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.aid.AIDMonitoringZone;
import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.evaluation.AIDEvaluator;
import de.dfg.oc.otc.aid.evaluation.IncidentEvaluationListener;
import de.dfg.oc.otc.aid.AIDUtilities;
import de.dfg.oc.otc.manager.aimsun.detectors.AbstractDetectorGroup;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
/**
 * Panel used for displaying information about monitoring zones.
 */
class AIDMonitoringZonePanel extends JPanel implements ActionListener {
    /**
     * Monitoring zone associated with this panel.
     */
    private final AIDMonitoringZone monitoringZone;

    /**
     * Grid bag constrains for the info panel layout.
     */
    private final GridBagConstraints gridConstraints;

    /**
     * List of listeners which will be notified once an incident is evaluated.
     */
    private final List<IncidentEvaluationListener> evaluationListeners;

    /**
     * List of incident panels.
     */
    private final List<AIDIncidentPanel> incidentPanels;

    /**
     * Panel which contains the detected incidents.
     */
    private JPanel incidentContentPanel;

    /**
     * Label which contains the adjustable algorithm parameters.
     */
    private JLabel parametersText;

    /**
     * Label which contains the detection time.
     */
    private JLabel detectionTimeText;

    /**
     * Label which contains the detection rate.
     */
    private JLabel detectionRateText;
    /**
     * Label which contains the false alarm rate (algorithm applications).
     */
    private JLabel falseAlarmRateApplicationsText;
    /**
     * Label which contains the false alarm rate (detected incidents).
     */
    private JLabel falseAlarmRateIncidentsText;
    /**
     * Label which contains the false alarm count.
     */
    private JLabel falseAlarmCountText;
    /**
     * Label which contains the detected incidents count.
     */
    private JLabel detectedCountText;
    /**
     * Label which contains the not detected incidents count.
     */
    private JLabel notDetectedCountText;
    /**
     * Label which contains the evaluated incidents count.
     */
    private JLabel actualCountText;
    /**
     * Button for loading incidents.
     */
    private JButton loadIncidentsButton;
    /**
     * Button for refreshing the evaluation results.
     */
    private JButton refreshButton;
    /**
     * Button for refreshing the evaluation results.
     */
    private JButton logButton;
    /**
     * File chooser for loading incidents.
     */
    private JFileChooser incidentFileChooser;
    /**
     * Temporary indicator for calculating the grid bag constraints of the info
     * panel layout.
     */
    private boolean firstColumn;
    /**
     * Temporary row counter for calculating the grid bag constraints of the
     * info panel layout.
     */
    private int row;

    AIDMonitoringZonePanel(AIDMonitoringZone monitoringZone) {
        super();

        this.incidentPanels = new ArrayList<>(5);
        this.evaluationListeners = new ArrayList<>(5);
        this.gridConstraints = new GridBagConstraints();
        this.monitoringZone = monitoringZone;
        this.firstColumn = true;

        initialize();
    }

    /**
     * Initializes the general layout of the panel.
     */
    private void initialize() {
        setLayout(new BorderLayout(5, 5));
        setTitle("Monitoring Zone");

        createInfoPanel();
        createIncidentPanel();
    }

    /**
     * Creates the content panel for displaying the incidents.
     */
    private void createIncidentPanel() {
        incidentContentPanel = new JPanel();
        incidentContentPanel.setLayout(new BoxLayout(incidentContentPanel, BoxLayout.PAGE_AXIS));

        JPanel incidentPanel = new JPanel();
        incidentPanel.setLayout(new BorderLayout());
        incidentPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Detected Incidents"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        incidentPanel.add(incidentContentPanel);

        add(incidentPanel, BorderLayout.CENTER);
    }

    /**
     * Creates the data panel with information about the algorithm and the
     * monitoring zone.
     *
     * @return Data panel with information about the algorithm and the
     * monitoring zone
     */
    private JPanel createDataPanel() {
        JPanel dataPanel = new JPanel();
        dataPanel.setLayout(new GridBagLayout());

        if (monitoringZone != null) {
            // Create description for detector pairs
            String detectorPairs = "<html>";
            for (AbstractDetectorGroup pair : monitoringZone.getMonitoredDetectorPairs()) {
                detectorPairs = detectorPairs.concat(pair.getPairDescription() + " (" + pair.getId() + ")" + "<br>");
            }
            detectorPairs = detectorPairs.concat("</html>");

            JLabel pairsText = new JLabel(detectorPairs);
            JLabel algorithmText = new JLabel(monitoringZone.getAIDAlgorithm().getName());

            parametersText = new JLabel("");
            detectionTimeText = new JLabel("0s");
            detectionRateText = new JLabel("1.0");
            falseAlarmRateApplicationsText = new JLabel("0.0");
            falseAlarmRateIncidentsText = new JLabel("0.0");
            falseAlarmCountText = new JLabel("0");
            actualCountText = new JLabel("0");
            detectedCountText = new JLabel("0");
            notDetectedCountText = new JLabel("0");

            updateAlgorithmInfo();

            JLabel algorithmLabel = new JLabel("Algorithm:");
            algorithmLabel.setLabelFor(algorithmText);

            JLabel parametersLabel = new JLabel("Algorithm parameters:");
            parametersLabel.setLabelFor(parametersText);

            JLabel pairsLabel = new JLabel("Detector pairs (ID):");
            pairsLabel.setLabelFor(pairsText);

            JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            separator.setPreferredSize(new Dimension(this.getWidth(), 3));

            JLabel detectionTimeLabel = new JLabel("Average Time To Detection:");
            detectionTimeLabel.setLabelFor(detectionTimeText);

            JLabel detectionRateLabel = new JLabel("Detection Rate:");
            detectionRateLabel.setLabelFor(detectionRateText);

            JLabel falseAlarmRateApplicationsLabel = new JLabel("FAR (Algorithm Applications):");
            falseAlarmRateApplicationsLabel.setLabelFor(falseAlarmRateApplicationsText);

            JLabel falseAlarmRateIncidentsLabel = new JLabel("FAR (Detected Incidents):");
            falseAlarmRateIncidentsLabel.setLabelFor(falseAlarmRateIncidentsText);

            JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
            separator2.setPreferredSize(new Dimension(this.getWidth(), 3));

            JLabel actualCountLabel = new JLabel("Actual Incidents:");
            actualCountLabel.setLabelFor(actualCountText);

            JLabel detectedCountLabel = new JLabel("Incidents Alarms:");
            detectedCountLabel.setLabelFor(detectedCountText);

            JLabel falseAlarmCountLabel = new JLabel("False Alarms:");
            falseAlarmCountLabel.setLabelFor(falseAlarmCountText);

            JLabel notDetectedCountLabel = new JLabel("Not Detected Incidents:");
            notDetectedCountLabel.setLabelFor(notDetectedCountLabel);

            dataPanel.add(algorithmLabel, this.getGridConstraints());
            dataPanel.add(algorithmText, this.getGridConstraints());
            dataPanel.add(parametersLabel, this.getGridConstraints());
            dataPanel.add(parametersText, this.getGridConstraints());
            dataPanel.add(pairsLabel, this.getGridConstraints());
            dataPanel.add(pairsText, this.getGridConstraints());
            dataPanel.add(separator, this.getGridConstraints());
            this.getGridConstraints();

            dataPanel.add(detectionTimeLabel, this.getGridConstraints());
            dataPanel.add(detectionTimeText, this.getGridConstraints());
            dataPanel.add(detectionRateLabel, this.getGridConstraints());
            dataPanel.add(detectionRateText, this.getGridConstraints());
            dataPanel.add(falseAlarmRateApplicationsLabel, this.getGridConstraints());
            dataPanel.add(falseAlarmRateApplicationsText, this.getGridConstraints());
            dataPanel.add(falseAlarmRateIncidentsLabel, this.getGridConstraints());
            dataPanel.add(falseAlarmRateIncidentsText, this.getGridConstraints());
            dataPanel.add(separator2, this.getGridConstraints());
            this.getGridConstraints();

            dataPanel.add(actualCountLabel, this.getGridConstraints());
            dataPanel.add(actualCountText, this.getGridConstraints());
            dataPanel.add(detectedCountLabel, this.getGridConstraints());
            dataPanel.add(detectedCountText, this.getGridConstraints());
            dataPanel.add(falseAlarmCountLabel, this.getGridConstraints());
            dataPanel.add(falseAlarmCountText, this.getGridConstraints());
            dataPanel.add(notDetectedCountLabel, this.getGridConstraints());
            dataPanel.add(notDetectedCountText, this.getGridConstraints());

            dataPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            setTitle("Monitoring Zone " + monitoringZone.getId());
        }
        return dataPanel;
    }

    /**
     * Creates the information panel on the left side of the monitoring zone
     * panel.
     */
    private void createInfoPanel() {
        JPanel infoPanel = new JPanel();
        infoPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Data"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        infoPanel.setLayout(new BorderLayout(5, 5));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        // Load incidents button
        loadIncidentsButton = new JButton("Load Incidents");
        loadIncidentsButton.addActionListener(this);
        buttonPanel.add(loadIncidentsButton);

        // Refresh info button
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(this);
        buttonPanel.add(refreshButton);

        // Refresh info button
        logButton = new JButton("Log");
        logButton.addActionListener(this);
        buttonPanel.add(logButton);

        // Incident file chooser
        FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("Incident Files (*.xml)", "xml");
        incidentFileChooser = new JFileChooser();
        incidentFileChooser.setDialogTitle("Load evaluated incidents");
        incidentFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        incidentFileChooser.setMultiSelectionEnabled(false);
        incidentFileChooser.setFileFilter(xmlfilter);

        infoPanel.add(createDataPanel(), BorderLayout.NORTH);
        infoPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.WEST);
    }

    /**
     * Sets the grid bag constraints for the info panel layout.
     *
     * @return Grid bag constraints for the info panel layout
     */
    private GridBagConstraints getGridConstraints() {
        this.gridConstraints.anchor = GridBagConstraints.NORTHWEST;

        if (this.firstColumn) {
            this.gridConstraints.gridx = 0;
            this.gridConstraints.gridy = this.row;
            this.gridConstraints.weightx = 0;
            this.gridConstraints.insets = new Insets(0, 0, 5, 10);
            this.firstColumn = false;
        } else {
            this.gridConstraints.gridx = 1;
            this.gridConstraints.gridy = this.row++;
            this.gridConstraints.fill = GridBagConstraints.HORIZONTAL;
            this.gridConstraints.weightx = 1;
            this.gridConstraints.insets = new Insets(0, 0, 5, 0);
            this.firstColumn = true;
        }

        return gridConstraints;
    }

    /**
     * Sets the title of the monitoring zone panel.
     */
    private void setTitle(String title) {
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    }

    /**
     * Updates the info texts with the current algorithm metrics.
     */
    void updateAlgorithmInfo() {
        AIDEvaluator evaluator = monitoringZone.getEvaluator();

        // Refreshes the algorithm parameters field.
        parametersText.setText(monitoringZone.getAIDAlgorithm().getParametersAsString());

        // Refreshes the average detection time.
        detectionTimeText.setText(String.valueOf(evaluator.getAverageTimeToDetect() + "s"));

        // Refreshes the detection rate.
        detectionRateText.setText(String.valueOf(evaluator.getDetectionRate()) + "%");

        // Refreshes the false alarm rate (algorithm applications).
        falseAlarmRateApplicationsText.setText(String.valueOf(evaluator.getFalseAlarmRateApplications()) + "%");

        // Refreshes the false alarm rate (detected incidents).
        falseAlarmRateIncidentsText.setText(String.valueOf(evaluator.getFalseAlarmRateIncidents()) + "%");

        // Refreshes the false alarm count.
        falseAlarmCountText.setText(String.valueOf(evaluator.getFalseAlarmCount()));

        // Refreshes the actually occurred incidents count.
        actualCountText.setText(String.valueOf(evaluator.getTruePositiveAlarms()));

        // Refreshes the detected incidents count.
        detectedCountText.setText(String.valueOf(evaluator.getDetectedIncidentsCount()));

        notDetectedCountText.setText(String.valueOf(evaluator.getTruePositiveAlarms() - (evaluator.getDetectedIncidentsCount() - evaluator.getFalseAlarmCount())));

        // Refresh incident descriptions
        incidentPanels.forEach(AIDIncidentPanel::updateDescription);
    }

    /**
     * Adds an incident to the monitoring zone panel.
     *
     * @param incident which is added to the monitoring zone panel
     */
    void addIncident(Incident incident) {
        AIDIncidentPanel incidentPanel = new AIDIncidentPanel(incident);
        incidentPanel.addIncidentEvaluationListeners(evaluationListeners);

        incidentContentPanel.add(incidentPanel, 0);
        incidentPanels.add(incidentPanel);

        updateAlgorithmInfo();

        repaint();
    }

    /**
     * Method is called when calibration button or load incidents button is clicked.
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        // Load evaluated incidents from xml file
        if (actionEvent.getSource() == loadIncidentsButton) {
            // File chooser state
            int state = incidentFileChooser.showOpenDialog(this);

            if (state == JFileChooser.APPROVE_OPTION) {
                // Load incidents from file
                List<Incident> incidents = AIDUtilities.getInstance().loadIncidentsFromFile(incidentFileChooser.getSelectedFile());

                // Notify listeners about incidents
                for (Incident incident : incidents) {
                    for (IncidentEvaluationListener listener : evaluationListeners) {
                        listener.incidentEvaluated(incident, true);
                    }
                }
            }
        }
        // Refresh algorithm information
        else if (actionEvent.getSource() == refreshButton) {
            updateAlgorithmInfo();
        } else if (actionEvent.getSource() == logButton) {
            monitoringZone.getEvaluator().logResults();
        }
    }

    /**
     * Adds an incident evaluation listener.
     *
     * @param listener which is notified once an incident is evaluated
     */
    void addIncidentEvaluationListener(IncidentEvaluationListener listener) {
        evaluationListeners.add(listener);
    }
}
