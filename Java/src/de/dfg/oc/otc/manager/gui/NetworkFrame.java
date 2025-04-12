package de.dfg.oc.otc.manager.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Frame zum Anzeigen der Daten des Netzwerks.
 */
class NetworkFrame extends JFrame {
    private static final long serialVersionUID = -6129574895223785102L;
    private final JTextArea networkData = new JTextArea();

    NetworkFrame() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setState(Frame.NORMAL);
        setTitle("Organic Traffic Control network data");
        final ImageIcon img = new ImageIcon("icons/network.png");
        setIconImage(img.getImage());

        JScrollPane pane = new JScrollPane(this.networkData);
        add(pane);

        pack();

        final int width = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
        final int height = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
        setSize(width / 3, height);
    }

    final void setText(final String theText) {
        this.networkData.setText(theText);
    }
}

