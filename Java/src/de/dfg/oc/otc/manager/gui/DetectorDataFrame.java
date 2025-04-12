package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.layer1.observer.DetectorObserver;
import de.dfg.oc.otc.layer1.observer.monitoring.DataStorage;
import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataStorage;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * @author rochner
 */
class DetectorDataFrame extends JFrame {
    private final DetectorObserver observer;
    private JComboBox<DetectorComboBoxEntry> detectorComboBox;
    private JTextPane textPanel;

    DetectorDataFrame(final DetectorObserver observer) throws HeadlessException {
        super("Detector Data");
        this.observer = observer;

        if (observer == null || observer.getNumObservedObjects() < 1) {
            JOptionPane.showMessageDialog(null, "No data to choose from!", "Detector data",
                    JOptionPane.ERROR_MESSAGE);
        }

        this.setContentPane(createContentPane());
        this.pack();
        this.setSize(this.getWidth(), 300);
    }

    /**
     * This method initializes detectorsJunctionComboBox.
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<DetectorComboBoxEntry> createDetectorComboBox() {
        detectorComboBox = new JComboBox<>();
        detectorComboBox.setToolTipText("Choose detector to view detailled informations.");

        final Collection<DataStorage> storageIter = observer.getStorages();
        for (DataStorage theStorage : storageIter) {
            DetectorDataStorage storage = (DetectorDataStorage) theStorage;
            Detector detector = storage.getDetector();
            detectorComboBox.addItem(new DetectorComboBoxEntry(detector, storage));
        }

        detectorComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DetectorComboBoxEntry selection = (DetectorComboBoxEntry) detectorComboBox.getSelectedItem();
                printData(selection);
            }

            private void printData(final DetectorComboBoxEntry entry) {
                final String linesep = System.getProperty("line.separator");
                final Detector detector = entry.detector;
                String output = "Data for detector " + detector.getId() + linesep;
                output = output.concat("(section " + detector.getSectionId() + ", lanes " + detector.getFirstLane()
                        + " to " + detector.getLastlane() + ")" + linesep);
                output = output.concat(entry.storage.toString("\t"));
                textPanel.setText(output);
            }
        });
        return detectorComboBox;
    }

    private JPanel createContentPane() {
        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(createDetectorComboBox(), BorderLayout.NORTH);
        contentPane.add(createTextScrollPanel(), BorderLayout.CENTER);
        return contentPane;
    }

    private JScrollPane createTextScrollPanel() {
        JScrollPane scrollPanel = new JScrollPane();

        textPanel = new JTextPane();
        textPanel.setText("Please select a detector from the drop down list above.");

        scrollPanel.setViewportView(textPanel);
        return scrollPanel;
    }

    private static final class DetectorComboBoxEntry {
        private final Detector detector;
        private final DataStorage storage;
        private String description = "";

        private DetectorComboBoxEntry(final Detector detector, final DetectorDataStorage storage) {
            this.detector = detector;
            this.storage = storage;
            if (storage == null) {
                throw new OTCManagerException("No valid storage for Detector " + detector.getId());
            } else {
                description = "Detector " + detector.getId() + "(section " + detector.getSectionId() + ")";
            }
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
