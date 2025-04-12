package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author rochner
 *
 */
class SaveLCSPopulationDialog extends JDialog {
    private static class JunctionChooserEntry {
        private String description = "";
        private final OTCNode theNode;

        JunctionChooserEntry(final OTCNode theNode) {
            this.theNode = theNode;
            description = "Junction " + theNode.getId();
        }

        OTCNode getNode() {
            return theNode;
        }

        public String toString() {
            return description;
        }
    }

    private static final long serialVersionUID = 1L;
    private JButton cancelButton;
    private JTextField fileNameInputField;
    private JComboBox<Object> junctionChooser;
    private JButton saveButton;

    SaveLCSPopulationDialog(final Frame owner, final Collection<OTCNode> nodesIter) {
        super(owner);
        initialize(nodesIter);
    }

    /**
     * This method initializes CancelButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.setText("Cancel");
            cancelButton.addActionListener(e -> dispose());
        }
        return cancelButton;
    }

    /**
     * This method initializes TimeInputField.
     *
     * @return javax.swing.JTextField
     */
    private JTextField getFileNameInputField() {
        if (fileNameInputField == null) {
            fileNameInputField = new JTextField("");
        }
        return fileNameInputField;
    }

    /**
     * This method initializes jContentPane.
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        final GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridy = 0;

        JLabel testlabel = new JLabel();
        testlabel.setText("Choose Node and Filename:");

        final GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
        gridBagConstraints6.gridx = 0;
        gridBagConstraints6.ipadx = 0;
        gridBagConstraints6.insets = new Insets(0, 10, 0, 10);
        gridBagConstraints6.gridy = 1;

        final GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
        gridBagConstraints7.gridx = 1;
        gridBagConstraints7.gridy = 4;
        gridBagConstraints7.anchor = GridBagConstraints.WEST;
        gridBagConstraints7.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints7.insets = new Insets(10, 5, 0, 5);

        final GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
        gridBagConstraints8.gridx = 0;
        gridBagConstraints8.gridy = 4;
        gridBagConstraints8.anchor = GridBagConstraints.WEST;
        gridBagConstraints8.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints8.insets = new Insets(10, 5, 0, 5);

        JLabel junctionChooserDescription = new JLabel();
        junctionChooserDescription.setText("Node:");

        final GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
        gridBagConstraints5.gridx = 2;
        gridBagConstraints5.ipadx = 30;
        gridBagConstraints5.ipady = 0;
        gridBagConstraints5.gridwidth = 1;
        gridBagConstraints5.anchor = GridBagConstraints.SOUTHEAST;
        gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints5.insets = new Insets(0, 5, 0, 5);
        gridBagConstraints5.gridy = 4;

        final GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
        gridBagConstraints4.gridx = 0;
        gridBagConstraints4.ipadx = 47;
        gridBagConstraints4.ipady = 57;
        gridBagConstraints4.gridy = 1;

        final GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
        gridBagConstraints3.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints3.gridx = 1;
        gridBagConstraints3.gridy = 2;
        gridBagConstraints3.ipadx = 0;
        gridBagConstraints3.ipady = 0;
        gridBagConstraints3.weightx = 1.0;
        gridBagConstraints3.gridwidth = 2;
        gridBagConstraints3.insets = new Insets(0, 0, 0, 1);

        final GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.ipadx = 0;
        gridBagConstraints2.ipady = 0;
        gridBagConstraints2.gridy = 2;

        final GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.gridy = 1;
        gridBagConstraints1.ipadx = 60;
        gridBagConstraints1.ipady = 0;
        gridBagConstraints1.weightx = 1.0;
        gridBagConstraints1.gridwidth = 2;
        gridBagConstraints1.gridx = 1;

        JLabel fileNameDescription = new JLabel("Filename:");

        JPanel jContentPane = new JPanel(new GridBagLayout());
        jContentPane.add(getJunctionChooser(), gridBagConstraints1);
        jContentPane.add(fileNameDescription, gridBagConstraints2);
        jContentPane.add(getFileNameInputField(), gridBagConstraints3);
        jContentPane.add(getSaveButton(), gridBagConstraints7);
        jContentPane.add(getSaveAllButton(), gridBagConstraints8);
        jContentPane.add(getCancelButton(), gridBagConstraints5);
        jContentPane.add(junctionChooserDescription, gridBagConstraints6);
        jContentPane.add(testlabel, gridBagConstraints);

        return jContentPane;
    }

    /**
     * This method initializes junctionChooser.
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<Object> getJunctionChooser() {
        if (junctionChooser == null) {
            junctionChooser = new JComboBox<>();
            junctionChooser.setPreferredSize(new Dimension(60, 25));

            junctionChooser.addActionListener(e -> updateFileNameInputField());
        }
        return junctionChooser;
    }

    /**
     * This method initializes OkButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getSaveAllButton() {
        JButton saveAllButton = new JButton("Save All");
        saveAllButton.addActionListener(e -> {
            try {
                for (int i = 0; i < getJunctionChooser().getItemCount(); i++) {
                    getJunctionChooser().setSelectedIndex(i);
                    final OTCNode theNode = ((JunctionChooserEntry) getJunctionChooser().getItemAt(i)).getNode();
                    final String filename = getFileNameInputField().getText();

                    theNode.getTLCSelector().saveMappingToFile(filename);
                }

            } catch (RuntimeException e2) {
                new TroubleDialog("Something went wrong: " + e2.getMessage());
                return;
            }
            dispose();
        });

        return saveAllButton;
    }

    /**
     * This method initializes OkButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getSaveButton() {
        if (saveButton == null) {
            saveButton = new JButton("Save");
            saveButton.addActionListener(e -> {
                if (getJunctionChooser().getSelectedIndex() < 0) {
                    new TroubleDialog("No junction chosen.");
                    return;
                }

                final String filename = getFileNameInputField().getText();
                OTCNode theNode;

                try {
                    theNode = ((JunctionChooserEntry) getJunctionChooser().getSelectedItem()).getNode();
                } catch (RuntimeException e2) {
                    new TroubleDialog("Something went wrong: " + e2.getMessage());
                    return;
                }

                theNode.getTLCSelector().saveMappingToFile(filename);
                dispose();
            });
        }
        return saveButton;
    }

    private void initialize(final Collection<OTCNode> nodesIter) {
        setSize(300, 200);
        setTitle("Save LCS Population");
        setContentPane(getJContentPane());
        if (nodesIter.isEmpty()) {
            getSaveButton().setEnabled(false);
            getJunctionChooser().addItem("No Nodes");
        } else {
            for (OTCNode theNode : nodesIter) {
                getJunctionChooser().addItem(new JunctionChooserEntry(theNode));
            }
            getJunctionChooser().setSelectedIndex(0);
            getJunctionChooser().setEnabled(true);

            // Update save file name after nodes were added
            updateFileNameInputField();
        }
        pack();
    }

    /**
     * Updates the {@code FileNameInputFile} that determines the filename
     * under which the LCS population is saved.
     *
     * @see #fileNameInputField
     */
    private void updateFileNameInputField() {
        final OTCNode currentNode = ((JunctionChooserEntry) getJunctionChooser().getSelectedItem()).getNode();
        String suffix = currentNode.getTLCSelector().getClass().getName();
        suffix = suffix.substring(suffix.lastIndexOf(".") + 1);
        final String filename = OTCManager.getInstance().getFilenamePrefix() + "_" + currentNode.getId() + "_" + suffix
                + ".txt";
        getFileNameInputField().setText(filename);
    }
}
