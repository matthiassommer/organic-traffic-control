package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.layer2.OptimisationTask;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCManagerException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;

/**
 * @author rochner
 *
 */
class TriggerL2Dialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private JComboBox<Comparable> junctionChooser;
    private JButton okButton;
    private JTextField timeInputField;

    TriggerL2Dialog(final Frame owner, final int[] junctions) {
        super(owner);
        initialize(junctions);
    }

    private JButton createCancelButton() {
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        return cancelButton;
    }

    private JPanel createContentPane() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        final TitledBorder border = BorderFactory.createTitledBorder("Choose junction and time");
        panel.setBorder(border);

        final JPanel junction = new JPanel();
        junction.add(new JLabel("Junction"));
        junction.add(getJunctionChooser());

        final JPanel time = new JPanel();
        JLabel timeLabel = new JLabel("Time");
        time.add(timeLabel);
        time.add(getTimeInputField());

        final JPanel buttons = new JPanel();
        buttons.add(getOkButton());
        buttons.add(createCancelButton());

        panel.add(junction, BorderLayout.NORTH);
        panel.add(time, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private JComboBox<Comparable> getJunctionChooser() {
        if (junctionChooser == null) {
            junctionChooser = new JComboBox<>();
            junctionChooser.setPreferredSize(new Dimension(60, 25));
        }
        return junctionChooser;
    }

    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton("Ok");
            okButton.addActionListener(e -> {
                float time;
                try {
                    final String timeString = getTimeInputField().getText();
                    time = Float.parseFloat(timeString);
                } catch (NumberFormatException e1) {
                    new TroubleDialog("Number format of time not valid. Please change.");
                    return;
                }

                try {
                    final int replicationId = OTCManager.getInstance().getReplicationID();
                    final int junctionId = (Integer) getJunctionChooser().getSelectedItem();

                    final float[] situation = OTCManager.getInstance().getNetwork().getNode(junctionId)
                            .getSituation(DataSource.STATISTICS);

                    OTCManager.getInstance().addTask(
                            new OptimisationTask(null, junctionId, time, situation, null, replicationId, null,
                                    Attribute.LOS, 0));

                    OTCManager.getInstance().newInfo(
                            "Started optimisation for junction " + junctionId + " at time step " + time
                                    + " in replication " + replicationId);
                } catch (OTCManagerException otcme) {
                    OTCManager.getInstance().newInfo("No valid situation.");
                } catch (IOException e1) {
                    OTCManager.getInstance().newInfo(e1.getMessage());
                }

                dispose();
            });
        }
        return okButton;
    }

    private JTextField getTimeInputField() {
        if (timeInputField == null) {
            timeInputField = new JTextField("0.0", 8);
        }
        return timeInputField;
    }

    private void initialize(final int[] junctions) {
        this.setSize(250, 180);
        this.setTitle("Trigger Layer 2");
        this.setContentPane(createContentPane());

        if (junctions == null) {
            getOkButton().setEnabled(false);
            junctionChooser.addItem("No Junctions");
        } else {
            for (int junctionId : junctions) {
                junctionChooser.addItem(junctionId);
            }
            junctionChooser.setSelectedIndex(0);
        }
    }
}
