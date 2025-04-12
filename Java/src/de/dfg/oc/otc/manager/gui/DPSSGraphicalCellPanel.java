package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.aimsun.AimsunNetwork;
import de.dfg.oc.otc.region.OTCNodeSynchronized;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Label shows a graphical representation for the established DPSS.
 *
 * @author Matthias Sommer
 */
class DPSSGraphicalCellPanel extends JPanel {
    private List<List<Integer>> establishedDPSSs = new ArrayList<>();
    private final int nodeBoxOffset_x = 20;
    private final List<List<JLabel>> nodeLabelsForDPSSs = new ArrayList<>();

    public DPSSGraphicalCellPanel() {
        this.setLayout(null);
    }

    /**
     * Get new list with DPSS and redraw the panel.
     *
     * @param dpss
     */
    final void updatePanel(final List<List<Integer>> dpss) {
        if (!dpss.isEmpty() && !dpss.equals(this.establishedDPSSs)) {
            this.establishedDPSSs = dpss;

            updateNodeLabels();
            this.repaint();
        }
    }

    /**
     * Redraw the panel showing the green waves.
     */
    private void updateNodeLabels() {
        this.nodeLabelsForDPSSs.clear();

        int dpssCounter = 0;
        final AimsunNetwork network = OTCManager.getInstance().getNetwork();

        for (List<Integer> nodePath : this.establishedDPSSs) {
            List<JLabel> nodeIDLabels = new ArrayList<>();
            int iteration = 0;

            for (Integer nodeID : nodePath) {
                iteration++;

                final JLabel label = new JLabel("<html><body>" + nodeID + "<br>"
                        + network.getNode(nodeID).getName() + "</body></html>");
                label.setForeground(Color.WHITE);
                label.setOpaque(true);
                label.setBackground(new java.awt.Color(72, 148, 39));
                label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                label.setSize(label.getPreferredSize());

                if (iteration == 1) {
                    int nodeBoxOffset_y = 20;
                    label.setLocation(10, nodeBoxOffset_y + dpssCounter * label.getHeight() + dpssCounter
                            * nodeBoxOffset_y);
                } else {
                    JLabel previousNodeIDLabel = nodeIDLabels.get(iteration - 2);
                    int nodeBoxOffset_y = 20;
                    label.setLocation(previousNodeIDLabel.getX() + previousNodeIDLabel.getWidth()
                            + this.nodeBoxOffset_x, nodeBoxOffset_y + dpssCounter * label.getHeight() + dpssCounter
                            * nodeBoxOffset_y);
                }

                nodeIDLabels.add(label);
            }

            dpssCounter++;

            this.nodeLabelsForDPSSs.add(nodeIDLabels);
        }
    }

    /**
     * Write ACT at the end of the line.
     */
    private void drawAgreedCylceTimes() {
        int iterationCounter = 0;
        final AimsunNetwork network = OTCManager.getInstance().getNetwork();

        for (List<Integer> dpss : this.establishedDPSSs) {
            final OTCNodeSynchronized node = (OTCNodeSynchronized) network.getNode(dpss.get(0));

            final List<JLabel> currentDPSSLabels = this.nodeLabelsForDPSSs.get(iterationCounter);
            final JLabel lastNodeLabel = currentDPSSLabels.get(currentDPSSLabels.size() - 1);

            final JLabel actLabel = new JLabel("Agreed Cycle Time: " + node.getAgreedCycleTime() + " sec");
            actLabel.setLocation(lastNodeLabel.getX() + lastNodeLabel.getWidth() + this.nodeBoxOffset_x,
                    lastNodeLabel.getY());
            actLabel.setSize(actLabel.getPreferredSize());
            add(actLabel);

            iterationCounter++;
        }
    }

    /**
     * Draw a connecting line between two boxes.
     *
     * @param g
     */
    private void drawLinesBetweenNodes(final Graphics g) {
        for (List<JLabel> dpssLabels : this.nodeLabelsForDPSSs) {
            int iterationCounter = 0;

            for (JLabel nodeIDLabel : dpssLabels) {
                iterationCounter++;

                final int lineOrigin_x = nodeIDLabel.getX() + nodeIDLabel.getWidth();
                final int lineOrigin_y = nodeIDLabel.getY() + nodeIDLabel.getHeight() / 2;
                final int lineEnd_x = lineOrigin_x + this.nodeBoxOffset_x;

                if (iterationCounter < dpssLabels.size()) {
                    g.drawLine(lineOrigin_x, lineOrigin_y, lineEnd_x, lineOrigin_y);
                }
            }
        }
    }

    /**
     * Draw a box for each node.
     */
    private void drawNodes() {
        final JSeparator lineSep = new JSeparator(SwingConstants.HORIZONTAL);

        for (List<JLabel> dpssLabels : this.nodeLabelsForDPSSs) {
            dpssLabels.forEach(this::add);
            add(lineSep);
        }
    }

    @Override
    protected final void paintComponent(final Graphics g) {
        if (!establishedDPSSs.isEmpty()) {
            super.paintComponent(g);

            this.removeAll();
            drawNodes();
            drawLinesBetweenNodes(g);
            drawAgreedCylceTimes();
        }
    }
}
