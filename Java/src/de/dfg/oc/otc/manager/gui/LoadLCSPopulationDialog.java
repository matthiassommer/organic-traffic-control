package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rochner
 */
class LoadLCSPopulationDialog extends JDialog {
    private final Frame ownerFrame;
    private JButton fileChooseButton;
    private JTextField fileNameInputField;
    private JComboBox<Object> junctionChooser;
    private JButton okButton;

    LoadLCSPopulationDialog(final Frame owner, final Collection<OTCNode> nodesIter) {
        super(owner);
        ownerFrame = owner;
        initialize(nodesIter);
    }

    /**
     * This method initializes CancelButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getCancelButton() {
        JButton cancelButton = new JButton();
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(e -> dispose());
        return cancelButton;
    }

    /**
     * This method initializes FileChooseButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getFileChooseButton() {
        if (fileChooseButton == null) {
            fileChooseButton = new JButton();
            fileChooseButton.setText("Choose File");
            fileChooseButton.addActionListener(e -> {
                String pfad = DefaultParams.PATH;
                pfad = pfad.substring(0, pfad.lastIndexOf("java"));

                final FileNameExtensionFilter filter = new FileNameExtensionFilter("LCS Population (*.txt)", "txt");
                final JFileChooser fc = new JFileChooser(pfad);
                fc.setDialogTitle("Choose LCS Population");
                fc.setFileFilter(filter);
                final int returnVal = fc.showOpenDialog(ownerFrame);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    final String filename = fc.getSelectedFile().getPath();
                    fileNameInputField.setText(filename);
                }
            });
        }
        return fileChooseButton;
    }

    /**
     * This method initializes FileChooseButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getFileChooseManyButton() {
        JButton fileChooseManyButton = new JButton();
        fileChooseManyButton.setText("Choose multiple files");
        fileChooseManyButton.addActionListener(e -> {
            String pfad = DefaultParams.PATH;
            pfad = pfad.substring(0, pfad.lastIndexOf("java"));
            final FileNameExtensionFilter filter = new FileNameExtensionFilter("LCS Population (*.txt)", "txt");
            final JFileChooser fc = new JFileChooser(pfad);
            fc.setDialogTitle("Choose LCS Populations");
            fc.setFileFilter(filter);
            fc.setMultiSelectionEnabled(true);
            final int returnVal = fc.showOpenDialog(ownerFrame);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                final File[] filenames = fc.getSelectedFiles();
                final Map<Integer, File> idToNode = new HashMap<>();
                final OTCManager otcm = OTCManager.getInstance();

                for (File filename1 : filenames) {
                    try {
// Determine node id from filename
                        String filename = filename1.getPath();

                        if (filename.contains("_LCS.txt")) {
                            filename = filename.split("_LCS.txt")[0];
                        } else {
                            filename = filename.split("_XCST.txt")[0];
                        }

                        final String[] andEverything = filename.split("_");
                        final int nodeId = Integer.parseInt(andEverything[andEverything.length - 1]);

// Store node id and filename
                        idToNode.put(nodeId, filename1);
                    } catch (NumberFormatException e1) {
                        JOptionPane.showMessageDialog(ownerFrame, "Classifiers from file " + filename1.getPath()
                                + " could not be restored.");
                    }
                }

                for (OTCNode node : otcm.getNetwork().getNodes()) {
                    int nodeId = node.getId();

                    if (idToNode.containsKey(nodeId)) {
                        node.getTLCSelector().loadMappingFromFile(idToNode.get(nodeId).getPath());
                        otcm.newInfo("Added classifiers to LCS of node " + nodeId);
                    }
                }
                dispose();
            }
        });

        return fileChooseManyButton;
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

        JLabel testlabel = new JLabel("Choose Node and Filename:");

        final GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
        gridBagConstraints6.gridx = 0;
        gridBagConstraints6.ipadx = 0;
        gridBagConstraints6.insets = new Insets(0, 10, 0, 10);
        gridBagConstraints6.gridy = 1;

        final GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
        gridBagConstraints9.gridx = 0;
        gridBagConstraints9.ipadx = 30;
        gridBagConstraints9.ipady = 0;
        gridBagConstraints9.gridwidth = 3;
        gridBagConstraints9.anchor = GridBagConstraints.SOUTHEAST;
        gridBagConstraints9.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints9.insets = new Insets(0, 5, 0, 5);
        gridBagConstraints9.gridy = 5;

        final GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
        gridBagConstraints8.gridx = 0;
        gridBagConstraints8.ipadx = 30;
        gridBagConstraints8.ipady = 0;
        gridBagConstraints8.gridwidth = 3;
        gridBagConstraints8.anchor = GridBagConstraints.SOUTHEAST;
        gridBagConstraints8.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints8.insets = new Insets(0, 5, 0, 5);
        gridBagConstraints8.gridy = 4;

        final GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
        gridBagConstraints7.gridx = 0;
        gridBagConstraints7.gridy = 6;
        gridBagConstraints7.anchor = GridBagConstraints.WEST;
        gridBagConstraints7.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints7.insets = new Insets(10, 5, 0, 5);
        gridBagConstraints7.gridwidth = 2;
        JLabel junctionChooserDescription = new JLabel("Node:");

        final GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
        gridBagConstraints5.gridx = 2;
        gridBagConstraints5.ipadx = 30;
        gridBagConstraints5.ipady = 0;
        gridBagConstraints5.gridwidth = 1;
        gridBagConstraints5.anchor = GridBagConstraints.SOUTHEAST;
        gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints5.insets = new Insets(0, 5, 0, 5);
        gridBagConstraints5.gridy = 6;

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

        JPanel jContentPane = new JPanel();
        jContentPane.setLayout(new GridBagLayout());
        jContentPane.add(getJunctionChooser(), gridBagConstraints1);
        jContentPane.add(fileNameDescription, gridBagConstraints2);
        jContentPane.add(getFileNameInputField(), gridBagConstraints3);
        jContentPane.add(getOkButton(), gridBagConstraints7);
        jContentPane.add(getCancelButton(), gridBagConstraints5);
        jContentPane.add(getFileChooseButton(), gridBagConstraints8);
        jContentPane.add(getFileChooseManyButton(), gridBagConstraints9);
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
        }
        return junctionChooser;
    }

    /**
     * This method initializes OkButton.
     *
     * @return javax.swing.JButton
     */
    private JButton getOkButton() {
        okButton = new JButton();
        okButton.setText("Ok");
        okButton.addActionListener(e -> {
            if (getJunctionChooser().getSelectedIndex() < 0) {
                new TroubleDialog("No junction selected.");
                return;
            }
            final String filename = getFileNameInputField().getText();
            final File testFile = new File(filename);
            if (!testFile.isFile()) {
                new TroubleDialog("Filename invalid.");
                return;
            }

            try {
                final OTCNode theNode = ((JunctionChooserEntry) getJunctionChooser().getSelectedItem()).getNode();
                final boolean loadOk = theNode.getTLCSelector().loadMappingFromFile(filename);
                if (!loadOk) {
                    JOptionPane.showMessageDialog(ownerFrame, "Some classifiers could not be restored, "
                            + "see log file for details.");
                }
            } catch (NullPointerException e1) {
                JOptionPane.showMessageDialog(ownerFrame, "Could not obtain the LCS for the selected node. "
                        + "No classifiers were added. " + "Is the node is not controlled by OTC?");
            } catch (HeadlessException e2) {
                new TroubleDialog("Something went wrong: " + e2.getMessage());
                return;
            }
            dispose();
        });

        return okButton;
    }

    private void initialize(final Collection<OTCNode> nodesIter) {
        setSize(300, 200);
        setTitle("Add Classifiers to LCS");
        setContentPane(getJContentPane());

        if (nodesIter.isEmpty()) {
            okButton.setEnabled(false);
            getFileChooseButton().setEnabled(false);
            junctionChooser.addItem("No Nodes");
        } else {
            for (OTCNode theNode : nodesIter) {
                junctionChooser.addItem(new JunctionChooserEntry(theNode));
            }
            junctionChooser.setSelectedIndex(0);
            junctionChooser.setEnabled(true);
            getFileNameInputField().setText("Please choose file (press button)");
        }
        pack();
    }

    private static final class JunctionChooserEntry {
        private final OTCNode node;
        private String description = "";

        private JunctionChooserEntry(final OTCNode theNode) {
            this.node = theNode;
            this.description = "Junction " + theNode.getId();
        }

        private OTCNode getNode() {
            return this.node;
        }

        public String toString() {
            return this.description;
        }
    }
}
