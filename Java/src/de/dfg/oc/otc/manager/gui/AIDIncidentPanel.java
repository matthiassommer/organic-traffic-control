package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.aid.Incident;
import de.dfg.oc.otc.aid.Incident.EvaluationStatus;
import de.dfg.oc.otc.aid.evaluation.IncidentEvaluationListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Panel which displays information about a single incident.
 */
@SuppressWarnings("serial")
class AIDIncidentPanel extends JPanel implements ActionListener {
    /**
     * Incident which is being displayed.
     */
    private final Incident incident;
    /**
     * Determines if detailed information about the incident should be
     * displayed.
     */
    private final boolean detailedView;
    /**
     * List of evaluation listeners.
     */
    private final List<IncidentEvaluationListener> evaluationListeners;
    /**
     * Button for setting the incident status to correct alarm.
     */
    private JButton correctAlarm;
    /**
     * Button for setting the incident status to false alarm.
     */
    private JButton falseAlarm;
    /**
     * Label which displays the evaluation status.
     */
    private JLabel alarmSelection;
    /**
     * Text area which contains the description of the incident.
     */
    private JTextArea incidentDescription;

    /**
     * Default constructor for normal view.
     *
     * @param incident which is being displayed
     */
    AIDIncidentPanel(Incident incident) {
        this(incident, false);
    }

    /**
     * Additional constructor where view type can be adjusted.
     *
     * @param incident     which is being displayed
     * @param detailedView Show detailed information
     */
    AIDIncidentPanel(Incident incident, boolean detailedView) {
        super();

        this.detailedView = detailedView;
        this.incident = incident;
        this.evaluationListeners = new ArrayList<>(5);

        initialize();
    }

    /**
     * Initializes the run gui components of this panel.
     */
    private void initialize() {
        setLayout(new BorderLayout(5, 5));

        // Detailed information
        if (detailedView) {
            JPanel detailPanel = new JPanel(new GridLayout(0, 1, 5, 5));

            JLabel junctionLabel = new JLabel("Junction " + incident.getJunctionID());
            JLabel monitoringZoneLabel = new JLabel("Monitoring Zone " + incident.getMonitoringZoneID());

            detailPanel.add(junctionLabel);
            detailPanel.add(monitoringZoneLabel);

            add(detailPanel, BorderLayout.WEST);
        }

        // Incident details
        incidentDescription = new JTextArea(incident.getDescription());
        incidentDescription.setEditable(false);
        add(incidentDescription);

        // Buttons
        correctAlarm = new JButton("Correct Alarm");
        correctAlarm.addActionListener(this);

        falseAlarm = new JButton("False Alarm");
        falseAlarm.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(correctAlarm);
        buttonPanel.add(falseAlarm);

        alarmSelection = new JLabel("Incident has not been evaluated yet", SwingConstants.CENTER);

        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.add(buttonPanel, BorderLayout.SOUTH);
        optionsPanel.add(alarmSelection, BorderLayout.CENTER);
        add(optionsPanel, BorderLayout.EAST);

        // Set evaluation status if incident has already been evaluated
        setEvaluation(incident.getEvaluationStatus());

        setMaximumSize(new Dimension(Integer.MAX_VALUE, incidentDescription.getPreferredSize().height));
    }

    /**
     * Changes the evaluation status of the incident updates the panel buttons.
     *
     * @param evaluationStatus of the incident
     */
    private void setEvaluation(EvaluationStatus evaluationStatus) {
        if (evaluationStatus == EvaluationStatus.TRUE_POSITIVE) {
            correctAlarm.setEnabled(false);
            falseAlarm.setEnabled(true);

            alarmSelection.setText("Evaluation: Correct Alarm");
        } else if (evaluationStatus == EvaluationStatus.FALSE_POSITIVE) {
            correctAlarm.setEnabled(true);
            falseAlarm.setEnabled(false);

            alarmSelection.setText("Evaluation: False Alarm");
        }
    }

    /**
     * Method is called when an incident is evaluated.
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        EvaluationStatus status = EvaluationStatus.NOT_EVALUATED;

        if (actionEvent.getSource() == correctAlarm) {
            status = EvaluationStatus.TRUE_POSITIVE;
        } else if (actionEvent.getSource() == falseAlarm) {
            status = EvaluationStatus.FALSE_POSITIVE;
        }

        // Incident has been evaluated for the first time
        boolean firstEvaluation = incident.getEvaluationStatus() == EvaluationStatus.NOT_EVALUATED;

        // Set incident evaluation status
        incident.setEvaluationStatus(status);

        // Notify listeners that the incident has been evaluated
        for (IncidentEvaluationListener listener : evaluationListeners) {
            listener.incidentEvaluated(this.incident, firstEvaluation);
        }

        // Update evaluation buttons
        setEvaluation(status);
    }

    /**
     * Updates the incident description.
     */
    void updateDescription() {
        // Refresh description
        if (incidentDescription != null && incident != null) {
            incidentDescription.setText(incident.getDescription());

            repaint();
        }
    }

    /**
     * Adds a new evaluation listener which is notified once the incident has
     * been evaluated.
     *
     * @param listener which is notified once the incident has been evaluated
     */
    void addIncidentEvaluationListener(IncidentEvaluationListener listener) {
        evaluationListeners.add(listener);
    }

    /**
     * Adds multiple new evaluation listeners which are notified once the
     * incident has been evaluated.
     *
     * @param listeners which are notified once the incident has been evaluated
     */
    void addIncidentEvaluationListeners(Collection<IncidentEvaluationListener> listeners) {
        evaluationListeners.addAll(listeners);
    }
}
