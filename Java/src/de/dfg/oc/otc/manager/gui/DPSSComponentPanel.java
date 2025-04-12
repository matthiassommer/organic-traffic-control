package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.region.DPSSManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Displays all established decentralised progressive signal systems.
 *
 * @author Matthias Sommer
 */
class DPSSComponentPanel extends JPanel {
    private final JLabel dpssActiveLabel = new JLabel("");
    /**
     * Shows all green waves.
     */
    private DPSSGraphicalCellPanel dpssGraphicalPanel;
    private final JLabel nextPSSCheckLabel = new JLabel("");
    private final JLabel nextPSSRunLabel = new JLabel("");
    private final JLabel numberOfPSSLabel = new JLabel("0");
    private final JLabel regionalManagerActiveLabel = new JLabel("");

    DPSSComponentPanel() {
        super();
        this.setLayout(new BorderLayout());

        createGUIElements();
        setDPSSActiveLabel();
        setRegionActiveLabel();
        setPSSCheckLabel();
        setPSSRunLabel();
    }

    private JScrollPane createDPSSPanel() {
        this.dpssGraphicalPanel = new DPSSGraphicalCellPanel();
        final TitledBorder border = BorderFactory.createTitledBorder("Progressive signal systems");
        this.dpssGraphicalPanel.setBorder(border);

        final JScrollPane scrollPane = new JScrollPane(this.dpssGraphicalPanel);
        scrollPane.setBorder(null);

        return scrollPane;
    }

    private void createGUIElements() {
        final JPanel panel = new JPanel(new GridBagLayout());
        int pos = 0;

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 5, 10, 5);

        c.gridx = 0;
        c.gridy = pos;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        final JLabel caption = new JLabel("Displays informations for the distributed progressive signal systems");
        final Font font = new Font("Verdana", Font.BOLD, 14);
        caption.setFont(font);
        caption.setForeground(new java.awt.Color(72, 148, 39));
        panel.add(caption, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>DPSS mechanism</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.dpssActiveLabel, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>RegionalManager</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.regionalManagerActiveLabel, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>Next time for PSS check</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.nextPSSCheckLabel, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>Next time for PSS run</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.nextPSSRunLabel, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridx = 0;
        c.gridy = ++pos;
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><b>Number of established PSS</b></html>"), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.gridy = pos;
        c.anchor = GridBagConstraints.EAST;
        panel.add(this.numberOfPSSLabel, c);

        add(panel, BorderLayout.NORTH);
        add(createDPSSPanel(), BorderLayout.CENTER);
    }

    private void setDPSSActiveLabel() {
        final boolean dpssActive = DPSSManager.getInstance().isDecentralPSSactive();
        if (dpssActive) {
            final Icon icon = new ImageIcon("icons/Circle_Green.png", "Active");
            this.dpssActiveLabel.setIcon(icon);
        } else {
            final Icon icon = new ImageIcon("icons/Circle_Red.png", "Not active");
            this.dpssActiveLabel.setIcon(icon);
        }
    }

    /**
     * Update the panel showing the DPSS.
     */
    private void setGraphicalDPSSPanel() {
        List<List<Integer>> dpssList = DPSSManager.getInstance().getEstablishedDPSSs();

        if (dpssList.isEmpty()) {
            this.numberOfPSSLabel.setText("0");
        } else {
            this.dpssGraphicalPanel.updatePanel(dpssList);
            this.numberOfPSSLabel.setText(String.valueOf(dpssList.size()));
        }
    }

    private void setPSSCheckLabel() {
        this.nextPSSCheckLabel.setText(String.valueOf(DPSSManager.getInstance().getNextTimeForPSSCheck()));
    }

    private void setPSSRunLabel() {
        this.nextPSSRunLabel.setText(String.valueOf(DPSSManager.getInstance().getNextTimeForPSSRun()));
    }

    private void setRegionActiveLabel() {
        final boolean regionActive = DPSSManager.getInstance().isRegionActive();
        if (regionActive) {
            final Icon icon = new ImageIcon("icons/Circle_Green.png", "Active");
            this.regionalManagerActiveLabel.setIcon(icon);
        } else {
            final Icon icon = new ImageIcon("icons/Circle_Red.png", "Not active");
            this.regionalManagerActiveLabel.setIcon(icon);
        }
    }

    /**
     * Update view if panel is now visible.
     */
    final void update() {
        setGraphicalDPSSPanel();
        setPSSCheckLabel();
        setPSSRunLabel();
    }

    /**
     * Update panel if DPSS check or DPSS run was executed.
     */
    final void updatePanel() {
        setPSSCheckLabel();
        setPSSRunLabel();

        final float time = OTCManager.getInstance().getTime();
        if (time == DPSSManager.getInstance().getNextTimeForPSSCheck()
                || time == DPSSManager.getInstance().getNextTimeForPSSRun()) {
            setGraphicalDPSSPanel();
        }
    }
}
