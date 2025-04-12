package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.layer1.observer.monitoring.DataStorage;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import de.dfg.oc.otc.manager.aimsun.TrafficType;
import de.dfg.oc.otc.manager.aimsun.Turning;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author rochner
 *
 */
class TurningStatisticsFrame extends JFrame {
    private static final class TurningComboBoxEntry {
        private String description = "";
        private final DataStorage storage;
        private final Turning turning;

        private TurningComboBoxEntry(final Turning turning, final AimsunJunction junction,
                             final boolean statBased) {
            this.turning = turning;
            final OTCNode node = junction.getNode();

            if (statBased) {
                storage = node.getL1StatObserver().getStorage(turning.getId());
            } else {
                storage = node.getL1DetectorObserver().getStorage(turning.getId());
            }

            if (storage == null) {
                throw new OTCManagerException("Did not find a matching storage for turning " + turning.getId());
            } else {
                description = junction.getId() + " Turning " + turning.getId() + " ("
                        + turning.getInSection().getId() + "->" + turning.getOutSection().getId() + ")";
            }
        }

        public String toString() {
            return description;
        }
    }

    private final AimsunNetwork network;
    private JTextPane textPanel;
    private JComboBox<TurningComboBoxEntry> turningComboBox;

    TurningStatisticsFrame(final AimsunNetwork network) throws HeadlessException {
        super("Turning Statistics");
        this.network = network;

        final ImageIcon img = new ImageIcon("icons/turning.png");
        setIconImage(img.getImage());

        if (network == null || network.getJunctions().isEmpty()) {
            JOptionPane.showMessageDialog(null, "No valid network present!", "Turning Statistics: Fatal Error",
                    JOptionPane.ERROR_MESSAGE);
            throw new OTCManagerException("No valid network present!");
        }

        setContentPane(createJContentPane());
        pack();

        final int height = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
        final int width = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
        setSize(width / 2, height);
    }

    private JPanel createJContentPane() {
        final LayoutManager borderLayout = new BorderLayout();

        JPanel contentPane = new JPanel();
        contentPane.setLayout(borderLayout);
        contentPane.add(getTurningComboBox(), BorderLayout.NORTH);

        JScrollPane textScrollPanel = new JScrollPane();
        textPanel = new JTextPane();
        textPanel.setText("Please select a turning from the drop down list above.");
        textScrollPanel.setViewportView(textPanel);

        contentPane.add(textScrollPanel, BorderLayout.CENTER);
        return contentPane;
    }

    private JComboBox<TurningComboBoxEntry> getTurningComboBox() {
        if (turningComboBox == null) {
            turningComboBox = new JComboBox<>();
            turningComboBox.setToolTipText("Choose turning to view statistical data.");

            final Collection<AimsunJunction> junctions = network.getJunctions().values();
            for (AimsunJunction junction : junctions) {
                for (Turning turning : junction.getTurnings(TrafficType.ALL)) {
                    turningComboBox.addItem(new TurningComboBoxEntry(turning, junction, true));
                }
            }

            turningComboBox.addActionListener(new java.awt.event.ActionListener() {
                private void printStatistics(final TurningComboBoxEntry entry) {
                    final String linesep = System.getProperty("line.separator");
                    final Turning turning = entry.turning;

                    String output = "Statistics for turning " + turning.getId() + " (from section "
                            + turning.getInSection().getId() + " to section " + turning.getOutSection().getId() + ")" + linesep;
                    output = output.concat(entry.storage.toString("\t"));

                    textPanel.setText(output);
                }

                public void actionPerformed(final java.awt.event.ActionEvent e) {
                    final TurningComboBoxEntry selection = (TurningComboBoxEntry) turningComboBox.getSelectedItem();
                    printStatistics(selection);
                }
            });
        }
        return turningComboBox;
    }
}
