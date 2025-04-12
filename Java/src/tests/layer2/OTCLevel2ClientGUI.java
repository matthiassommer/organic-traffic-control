package tests.layer2;

import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer2.OptimisationTask;
import de.dfg.oc.otc.layer2.TurningData;
import de.dfg.oc.otc.layer2.ea.EAConfig;
import de.dfg.oc.otc.manager.OTCManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * GUI for the Level 2 Client.
 */
class OTCLevel2ClientGUI extends JFrame {
    private final OTCLevel2ClientGUI frame;
    private JCheckBox jCheckBoxAvgFitness;
    private JCheckBox jCheckBoxUseFixedSeed;
    private JCheckBox jCheckBoxWebster;
    private JTextField jTextFieldCycleConstraint;
    private JTextField jTextFieldMaxGen;
    private JTextField jTextFieldNodeID;
    private JTextField jTextFieldNumChildren;
    private JTextField jTextFieldPopSize;
    private JTextField jTextFieldRandSeed;
    private JTextField jTextFieldRepID;
    private JTextField jTextFieldSimDur;
    private JTextField jTextFieldTime;
    private JTextField jTextFieldWarmDur;

    OTCLevel2ClientGUI() {
        super();
        frame = this;
        initialize();
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            OTCLevel2ClientGUI gui = new OTCLevel2ClientGUI();
            gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            gui.setVisible(true);
        });
    }

    private int getCycleConstraint() {
        return new Integer(this.jTextFieldCycleConstraint.getText());
    }

    private JButton getJButtonANGFile() {
        JButton button = new JButton();
        button.setText("ANG File");
        button.setEnabled(true);

        button.addActionListener(e -> {
            String filename = File.separator + "ang";
            JFileChooser fc = new JFileChooser(new File(filename));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("ANG files", "ang");
            fc.setFileFilter(filter);
            fc.setCurrentDirectory(new File("."));
            fc.showOpenDialog(frame);
            File selFile = fc.getSelectedFile();
            if (selFile != null) {
                frame.getJTextFieldModel().setText(selFile.getAbsolutePath());
            } else {
                frame.getJTextFieldModel().setText("");
            }
        });

        return button;
    }

    private JButton getJButtonSend() {
        JButton button = new JButton();
        button.setText("Send");
        button.setEnabled(true);
        button.addActionListener(e -> {
            try {
                EAConfig eaConf = new EAConfig(getSimDur(), getWarmDur(), getPopSize(), getMaxGen(),
                        getNumberOfChildren(), getRandSeed(), isUseFixedSeed(), isAvgFitness(), isWebster());

                float[] situation = {830, 1800, 530, 190, 740, 610};
                // Spitzenstunde vorm.

                int[] sectionIdsForSit = {155, 115, 682, 159, 682, 149, 206, 207, 272, 159, 211, 212};
                Map<String, TurningData> turnData = new HashMap<>();

                TurningData td = new TurningData(272, 159, 3, 740);
                td.addPhase(7);
                td.addPhase(8);
                td.addPhase(9);
                turnData.put("272;159", td);

                td = new TurningData(206, 207, 2, 190);
                td.addPhase(5);
                td.addPhase(6);
                td.addPhase(7);
                td.addPhase(8);
                turnData.put("206;207", td);

                td = new TurningData(682, 149, 2, 530);
                td.addPhase(4);
                td.addPhase(5);
                turnData.put("682;149", td);

                td = new TurningData(682, 159, 3, 1800);
                td.addPhase(2);
                td.addPhase(3);
                td.addPhase(4);
                td.addPhase(5);
                turnData.put("682;159", td);

                td = new TurningData(211, 212, 2, 610);
                td.addPhase(1);
                td.addPhase(2);
                td.addPhase(8);
                td.addPhase(9);
                td.addPhase(10);
                turnData.put("211;212", td);

                td = new TurningData(155, 115, 3, 830);
                td.addPhase(2);
                turnData.put("155;115", td);

                String angFileName = frame.getJTextFieldModel().getText();
                if (!angFileName.isEmpty()) {
                    OptimisationTask optTask = new OptimisationTask(new File("angNodeFiles/" + angFileName), getNodeID(), getTime(),
                            situation, sectionIdsForSit, getReplicationID(), eaConf, Attribute.LOS,
                            getCycleConstraint());
                    optTask.setTurningData(turnData);
                    OTCManager.getInstance().addTask(optTask);
                } else {
                    OptimisationTask optTask = new OptimisationTask(null, getNodeID(), getTime(), situation,
                            sectionIdsForSit, getReplicationID(), eaConf, Attribute.LOS, getCycleConstraint());
                    optTask.setTurningData(turnData);
                    OTCManager.getInstance().addTask(optTask);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        return button;
    }

    private JCheckBox getJCheckBoxAvgFitness() {
        jCheckBoxAvgFitness = new JCheckBox("Avg. fitness", false);
        return jCheckBoxAvgFitness;
    }

    private JCheckBox getJCheckBoxUseFixedSeed() {
        jCheckBoxUseFixedSeed = new JCheckBox("Fixed seed", true);
        return jCheckBoxUseFixedSeed;
    }

    private JCheckBox getJCheckBoxWebster() {
        if (jCheckBoxWebster == null) {
            jCheckBoxWebster = new JCheckBox("Webster", false);
        }

        jCheckBoxWebster.addActionListener(actionEvent -> {
            AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
            boolean selected = abstractButton.getModel().isSelected();
            // Sinnvolle Einstellungen erzwingen
            if (selected) {
                getJCheckBoxUseFixedSeed().setSelected(true);
                getJCheckBoxUseFixedSeed().setEnabled(false);
                getJCheckBoxAvgFitness().setSelected(false);
                getJCheckBoxAvgFitness().setEnabled(false);
            } else {
                getJCheckBoxUseFixedSeed().setEnabled(true);
                getJCheckBoxAvgFitness().setEnabled(true);
            }
        });
        return jCheckBoxWebster;
    }

    private JPanel getJContentPane() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(8, 9, 0, 0);
        constraints.gridy = 0;
        JLabel jLabel2AIMSUN = new JLabel();
        jLabel2AIMSUN.setText("AIMSUN configuration");

        // ANG-File textfield
        GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
        gridBagConstraints13.gridx = 2;
        gridBagConstraints13.gridy = 1;

        // ANG-File button
        GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
        gridBagConstraints14.gridx = 1;
        gridBagConstraints14.gridy = 1;

        GridBagConstraints gridBagConstraints101 = new GridBagConstraints();
        gridBagConstraints101.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints101.gridy = 8;
        gridBagConstraints101.weightx = 1.0;
        gridBagConstraints101.gridx = 3;
        GridBagConstraints gridBagConstraints91 = new GridBagConstraints();
        gridBagConstraints91.gridx = 3;
        gridBagConstraints91.gridy = 7;
        JLabel jLabel1 = new JLabel();
        jLabel1.setText("Random Seed");
        GridBagConstraints gridBagConstraints81 = new GridBagConstraints();
        gridBagConstraints81.gridx = 1;
        gridBagConstraints81.gridy = 9;

        GridBagConstraints gridBagConstraints71 = new GridBagConstraints();
        gridBagConstraints71.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints71.gridy = 10;
        gridBagConstraints71.weightx = 1.0;
        gridBagConstraints71.gridx = 1;

        GridBagConstraints gridBagConstraints62 = new GridBagConstraints();
        gridBagConstraints62.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints62.gridy = 5;
        gridBagConstraints62.weightx = 1.0;
        gridBagConstraints62.gridx = 3;

        GridBagConstraints gridBagConstraints52 = new GridBagConstraints();
        gridBagConstraints52.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints52.gridy = 6;
        gridBagConstraints52.weightx = 1.0;
        gridBagConstraints52.gridx = 3;

        GridBagConstraints gridBagConstraints32 = new GridBagConstraints();
        gridBagConstraints32.gridx = 3;
        gridBagConstraints32.gridy = 10;

        GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
        gridBagConstraints9.gridx = 1;
        gridBagConstraints9.anchor = GridBagConstraints.WEST;
        gridBagConstraints9.insets = new Insets(8, 9, 0, 0);
        gridBagConstraints9.gridy = 4;

        JLabel jLabelEAConfig = new JLabel();
        jLabelEAConfig.setText("EA configuration");

        GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
        gridBagConstraints8.gridx = 1;
        gridBagConstraints8.gridy = 7;
        JLabel jLabelWarmDur = new JLabel();
        jLabelWarmDur.setText("WarmUp Duration");

        GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
        gridBagConstraints7.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints7.gridy = 8;
        gridBagConstraints7.weightx = 1.0;
        gridBagConstraints7.gridx = 1;

        GridBagConstraints gridBagConstraints61 = new GridBagConstraints();
        gridBagConstraints61.gridx = 2;
        gridBagConstraints61.gridy = 5;

        JLabel jLabelPopSize = new JLabel();
        jLabelPopSize.setText("Population Size");

        GridBagConstraints gridBagConstraints51 = new GridBagConstraints();
        gridBagConstraints51.gridx = 2;
        gridBagConstraints51.gridy = 7;

        JLabel jLabelSimDur = new JLabel();
        jLabelSimDur.setText("Simulation Duration");

        GridBagConstraints gridBagConstraints41 = new GridBagConstraints();
        gridBagConstraints41.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints41.gridy = 8;
        gridBagConstraints41.weightx = 1.0;
        gridBagConstraints41.gridx = 2;
        GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
        gridBagConstraints31.gridx = 1;
        gridBagConstraints31.gridy = 5;

        JLabel jLabelMaxGen = new JLabel();
        jLabelMaxGen.setText("#Generations");

        GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
        gridBagConstraints21.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints21.gridy = 6;
        gridBagConstraints21.weightx = 1.0;
        gridBagConstraints21.gridx = 2;

        GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
        gridBagConstraints11.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints11.gridy = 6;
        gridBagConstraints11.weightx = 1.0;
        gridBagConstraints11.gridx = 1;

        GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
        gridBagConstraints6.gridx = 3;
        gridBagConstraints6.anchor = GridBagConstraints.SOUTH;
        gridBagConstraints6.gridy = 11;

        GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
        gridBagConstraints5.gridx = 3;
        gridBagConstraints5.gridy = 2;

        JLabel jLabelTime = new JLabel();
        jLabelTime.setText("Time (secs since start)");

        GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
        gridBagConstraints4.gridx = 2;
        gridBagConstraints4.gridy = 2;

        JLabel jLabelNodeID = new JLabel();
        jLabelNodeID.setText("Node ID");

        GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
        gridBagConstraints3.gridx = 1;
        gridBagConstraints3.gridy = 2;

        JLabel jLabelRepID = new JLabel();
        jLabelRepID.setText("Replication ID");

        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints2.gridy = 3;
        gridBagConstraints2.weightx = 1.0;
        gridBagConstraints2.gridx = 3;

        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints1.gridy = 3;
        gridBagConstraints1.weightx = 1.0;
        gridBagConstraints1.gridx = 2;

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.gridx = 1;

        GridBagConstraints gridBagConstraints42 = new GridBagConstraints();
        gridBagConstraints42.gridy = 10;
        gridBagConstraints42.gridx = 2;

        GridBagConstraints gridBagConstraints43 = new GridBagConstraints();
        gridBagConstraints43.gridy = 9;
        gridBagConstraints43.gridx = 2;

        JLabel jLabelCycleConstraint = new JLabel();
        jLabelCycleConstraint.setText("Cycle constraint");

        JPanel jContentPane = new JPanel();
        jContentPane.setLayout(new GridBagLayout());
        jContentPane.add(getJTextFieldRepID(), gridBagConstraints);
        jContentPane.add(getJTextFieldNodeID(), gridBagConstraints1);
        jContentPane.add(getJTextFieldTime(), gridBagConstraints2);
        jContentPane.add(jLabelRepID, gridBagConstraints3);
        jContentPane.add(jLabelNodeID, gridBagConstraints4);
        jContentPane.add(jLabelTime, gridBagConstraints5);
        jContentPane.add(getJButtonSend(), gridBagConstraints6);
        jContentPane.add(getJTextFieldMaxGen(), gridBagConstraints11);
        jContentPane.add(getJTextFieldPopSize(), gridBagConstraints21);
        jContentPane.add(jLabelMaxGen, gridBagConstraints31);
        jContentPane.add(getJTextFieldSimDur(), gridBagConstraints41);
        jContentPane.add(jLabelSimDur, gridBagConstraints51);
        jContentPane.add(jLabelPopSize, gridBagConstraints61);
        jContentPane.add(getJTextFieldWarmDur(), gridBagConstraints7);
        jContentPane.add(jLabelWarmDur, gridBagConstraints8);
        jContentPane.add(jLabelEAConfig, gridBagConstraints9);
        jContentPane.add(getJPanel(), gridBagConstraints32);
        jContentPane.add(getJTextFieldNumChildren(), gridBagConstraints52);
        jContentPane.add(getJTextField2(), gridBagConstraints62);
        jContentPane.add(jLabel1, gridBagConstraints91);
        jContentPane.add(getJTextField_randSeed(), gridBagConstraints101);
        jContentPane.add(jLabel2AIMSUN, constraints);
        jContentPane.add(jLabelCycleConstraint, gridBagConstraints43);
        jContentPane.add(getJTextFieldCycleConstraint(), gridBagConstraints42);
        jContentPane.add(getJTextFieldModel(), gridBagConstraints13);
        jContentPane.add(getJButtonANGFile(), gridBagConstraints14);

        return jContentPane;
    }

    private JPanel getJPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints10 = new GridBagConstraints();

        // Use fixed random seed?
        gridBagConstraints10.gridx = 1;
        gridBagConstraints10.gridy = 0;
        jPanel.add(getJCheckBoxUseFixedSeed(), gridBagConstraints10);

        // Use average fitness?
        gridBagConstraints10.gridx = 1;
        gridBagConstraints10.gridy = 1;
        jPanel.add(getJCheckBoxAvgFitness(), gridBagConstraints10);

        // Use Webster?
        gridBagConstraints10.gridx = 2;
        gridBagConstraints10.gridy = 1;
        jPanel.add(getJCheckBoxWebster(), gridBagConstraints10);

        return jPanel;
    }

    private JTextField getJTextFieldCycleConstraint() {
        jTextFieldCycleConstraint = new JTextField();
        jTextFieldCycleConstraint.setText("0");
        jTextFieldCycleConstraint.setHorizontalAlignment(JTextField.TRAILING);
        jTextFieldCycleConstraint.setPreferredSize(new Dimension(75, 20));

        return jTextFieldCycleConstraint;
    }

    private JTextField getJTextFieldMaxGen() {
        jTextFieldMaxGen = new JTextField();
        jTextFieldMaxGen.setPreferredSize(new Dimension(75, 20));
        jTextFieldMaxGen.setText("2");
        jTextFieldMaxGen.setHorizontalAlignment(JTextField.TRAILING);

        return jTextFieldMaxGen;
    }

    private JTextField getJTextFieldModel() {
        JTextField jTextFieldModel = new JTextField();
        jTextFieldModel.setPreferredSize(new Dimension(250, 20));

        return jTextFieldModel;
    }

    private JTextField getJTextFieldNodeID() {
        jTextFieldNodeID = new JTextField();
        jTextFieldNodeID.setPreferredSize(new Dimension(75, 20));
        jTextFieldNodeID.setHorizontalAlignment(JTextField.TRAILING);
        jTextFieldNodeID.setText("160");

        return jTextFieldNodeID;
    }

    private JTextField getJTextFieldNumChildren() {
        jTextFieldNumChildren = new JTextField();
        jTextFieldNumChildren.setPreferredSize(new Dimension(75, 20));
        jTextFieldNumChildren.setHorizontalAlignment(JTextField.TRAILING);
        jTextFieldNumChildren.setText("5");

        return jTextFieldNumChildren;
    }

    private JTextField getJTextFieldPopSize() {
        jTextFieldPopSize = new JTextField();
        jTextFieldPopSize.setPreferredSize(new Dimension(75, 20));
        jTextFieldPopSize.setText("5");
        jTextFieldPopSize.setHorizontalAlignment(JTextField.TRAILING);

        return jTextFieldPopSize;
    }

    private JTextField getJTextField_randSeed() {
        jTextFieldRandSeed = new JTextField();
        jTextFieldRandSeed.setPreferredSize(new Dimension(75, 20));
        jTextFieldRandSeed.setHorizontalAlignment(JTextField.TRAILING);
        jTextFieldRandSeed.setText("0");

        return jTextFieldRandSeed;
    }

    private JTextField getJTextFieldRepID() {
        jTextFieldRepID = new JTextField();
        jTextFieldRepID.setPreferredSize(new Dimension(75, 20));
        jTextFieldRepID.setHorizontalAlignment(JTextField.TRAILING);
        jTextFieldRepID.setText("676");

        return jTextFieldRepID;
    }

    private JTextField getJTextFieldSimDur() {
        jTextFieldSimDur = new JTextField();
        jTextFieldSimDur.setPreferredSize(new Dimension(75, 20));
        jTextFieldSimDur.setText("3600");
        jTextFieldSimDur.setHorizontalAlignment(JTextField.TRAILING);

        return jTextFieldSimDur;
    }

    private JTextField getJTextFieldTime() {
        jTextFieldTime = new JTextField();
        jTextFieldTime.setPreferredSize(new Dimension(75, 20));
        jTextFieldTime.setHorizontalAlignment(JTextField.TRAILING);
        jTextFieldTime.setText("0.00");

        return jTextFieldTime;
    }

    private JTextField getJTextFieldWarmDur() {
        jTextFieldWarmDur = new JTextField();
        jTextFieldWarmDur.setPreferredSize(new Dimension(75, 20));
        jTextFieldWarmDur.setText("900");
        jTextFieldWarmDur.setHorizontalAlignment(JTextField.TRAILING);

        return jTextFieldWarmDur;
    }

    private JTextField getJTextField2() {
        JTextField jTextField2 = new JTextField();
        jTextField2.setText("# children");
        jTextField2.setEditable(false);
        jTextField2.setFont(new Font("Dialog", Font.BOLD, 12));

        return jTextField2;
    }

    private int getMaxGen() {
        return new Integer(this.jTextFieldMaxGen.getText());
    }

    private int getNodeID() {
        return new Integer(this.jTextFieldNodeID.getText());
    }

    private int getNumberOfChildren() {
        return new Integer(this.jTextFieldNumChildren.getText());
    }

    private int getPopSize() {
        return new Integer(this.jTextFieldPopSize.getText());
    }

    private long getRandSeed() {
        return new Long(this.jTextFieldRandSeed.getText());
    }

    private int getReplicationID() {
        return new Integer(this.jTextFieldRepID.getText());
    }

    private int getSimDur() {
        return new Integer(this.jTextFieldSimDur.getText());
    }

    private float getTime() {
        return new Float(this.jTextFieldTime.getText());
    }

    private int getWarmDur() {
        return new Integer(this.jTextFieldWarmDur.getText());
    }

    private void initialize() {
        this.setSize(800, 350);
        this.setContentPane(getJContentPane());
        this.setTitle("OTCLevel2Client");
    }

    private boolean isAvgFitness() {
        return this.jCheckBoxAvgFitness.isSelected();
    }

    private boolean isUseFixedSeed() {
        return this.jCheckBoxUseFixedSeed.isSelected();
    }

    private boolean isWebster() {
        return this.jCheckBoxWebster.isSelected();
    }

} // @jve:decl-index=0:visual-constraint="10,10"
