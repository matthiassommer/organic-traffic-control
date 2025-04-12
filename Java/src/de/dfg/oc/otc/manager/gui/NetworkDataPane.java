package de.dfg.oc.otc.manager.gui;

import de.dfg.oc.otc.manager.aimsun.AimsunJunction;
import de.dfg.oc.otc.manager.aimsun.Centroid;
import de.dfg.oc.otc.manager.aimsun.Section;
import de.dfg.oc.otc.manager.aimsun.detectors.Detector;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

/**
 * Shows information for the chosen section/turning/junction in the network tree
 * viewer.
 */
class NetworkDataPane extends JPanel implements TreeSelectionListener {
    private final boolean INCLUDE_DETECTOR_VALUES = false;
    private String htmlStartString;
    private JEditorPane networkDataPane;
    private DefaultMutableTreeNode junctionNode;
    private DefaultMutableTreeNode detectorNode;
    private DefaultMutableTreeNode sectionNode;
    private DefaultMutableTreeNode centroidNode;
    private JTree tree;

    NetworkDataPane() {
        super();
        initialize();
    }

    private void addCentroid(final Centroid centroid) {
        final MutableTreeNode node = new DefaultMutableTreeNode(new NodeData("Centroid " + centroid.getId(),
                centroid.getDescription()));
        getCentroidNode().add(node);
    }

    private void addDetector(final Detector detector) {
        final MutableTreeNode node = new DefaultMutableTreeNode(new NodeData("Detector " + detector.getId(),
                detector.getSimpleDescription(INCLUDE_DETECTOR_VALUES)));
        getDetectorNode().add(node);
    }

    private void addJunction(final AimsunJunction junction) {
        final String controlled = junction.isControlled() ? "" : " (NC)";
        final MutableTreeNode node = new DefaultMutableTreeNode(new NodeData("Junction " + junction.getId()
                + controlled, junction.getDescription()));
        getJunctionNode().add(node);
    }

    final void addNetworkComponent(final Object component) {
        if (component instanceof AimsunJunction) {
            addJunction((AimsunJunction) component);
        } else if (component instanceof Detector) {
            addDetector((Detector) component);
        } else if (component instanceof Section) {
            addSection((Section) component);
        } else if (component instanceof Centroid) {
            addCentroid((Centroid) component);
        }
    }

    private void addSection(final Section section) {
        final MutableTreeNode node = new DefaultMutableTreeNode(new NodeData("Section " + section.getId(),
                section.getDescription(INCLUDE_DETECTOR_VALUES)));
        getSectionNode().add(node);
    }

    final void clearDetectors() {
        getDetectorNode().removeAllChildren();
    }

    final void clearJunctions() {
        getJunctionNode().removeAllChildren();
    }

    final void clearSections() {
        getSectionNode().removeAllChildren();
    }

    private void displayDescription(final String description) {
        if (!description.isEmpty()) {
            getNetworkDataPane().setText(htmlStartString + description + "</body></html>");
            getNetworkDataPane().setCaretPosition(0);
        }
    }

    private DefaultMutableTreeNode getCentroidNode() {
        if (centroidNode == null) {
            centroidNode = new DefaultMutableTreeNode(new NodeData("Centroids", "View all centroids"));
        }
        return centroidNode;
    }

    /**
     * This method initializes descriptionScrollPane.
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane createDescriptionScrollPane() {
        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(getNetworkDataPane());
        return scrollPane;
    }

    private DefaultMutableTreeNode getDetectorNode() {
        if (detectorNode == null) {
            detectorNode = new DefaultMutableTreeNode(new NodeData("Detectors", "View all detectors"));
        }
        return detectorNode;
    }

    private DefaultMutableTreeNode getJunctionNode() {
        if (junctionNode == null) {
            junctionNode = new DefaultMutableTreeNode(new NodeData("Junctions", "View all junctions"));
        }
        return junctionNode;
    }

    /**
     * This method initializes networkDataPane.
     *
     * @return javax.swing.JEditorPane
     */
    private JEditorPane getNetworkDataPane() {
        if (networkDataPane == null) {
            networkDataPane = new MyJEditorPane();
            networkDataPane.setContentType("text/html");
            networkDataPane.setEditable(false);
            networkDataPane.setOpaque(false);
        }

        return networkDataPane;
    }

    /**
     * This method initializes networkScrollPane.
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane createNetworkScrollPane() {
        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(getNetworkTree());
        return scrollPane;
    }

    /**
     * This method initializes networkSplitPane.
     *
     * @return javax.swing.JSplitPane
     */
    private JSplitPane createNetworkSplitPane() {
        final JSplitPane splitPane = new JSplitPane();
        splitPane.setRightComponent(createDescriptionScrollPane());
        splitPane.setLeftComponent(createNetworkScrollPane());
        splitPane.setDividerLocation(200);
        return splitPane;
    }

    /**
     * This method initializes networkTree.
     *
     * @return javax.swing.JTree
     */
    private JTree getNetworkTree() {
        if (tree == null) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeData("Network", "View description of the network"));
            node.add(getJunctionNode());
            node.add(getDetectorNode());
            node.add(getSectionNode());
            node.add(getCentroidNode());

            tree = new JTree(node, true);
            tree.addTreeSelectionListener(this);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }
        return tree;
    }

    private DefaultMutableTreeNode getSectionNode() {
        if (sectionNode == null) {
            sectionNode = new DefaultMutableTreeNode(new NodeData("Sections", "View all sections"));
        }
        return sectionNode;
    }

    private void initialize() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;

        setLayout(new GridBagLayout());
        add(createNetworkSplitPane(), gridBagConstraints);

        htmlStartString = "<html><head><style type=\"text/css\">" + "body { font-family:Dialog,Helvetica,Arial; }"
                + "</style></head><body>";
    }

    final void setDetectorNodeDescription(final String description) {
        final NodeData data = (NodeData) getDetectorNode().getUserObject();
        data.description = description;
    }

    @Override
    public final void valueChanged(final TreeSelectionEvent event) {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) getNetworkTree().getLastSelectedPathComponent();
        final NodeData data = (NodeData) node.getUserObject();
        displayDescription(data.description);
    }

    private static final class NodeData {
        private String description = "";
        private String name = "";

        private NodeData(final String name, final String description) {
            this.name = name;
            this.description = description;
        }

        public String toString() {
            return this.name;
        }
    }

    private static class MyJEditorPane extends JEditorPane {
        private final ImageIcon image = new ImageIcon("icons/oc.png");

        public void paint(final Graphics g) {
            g.drawImage(image.getImage(), getSize().width - image.getIconWidth() - 10, 10, null, null);
            super.paint(g);
        }
    }
}
