package de.dfg.oc.otc.manager.gui;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Main class, starts gui for OTC Manager.
 */
abstract class Main {
    public static void main(final String[] args) {
        try {
            System.setErr(new PrintStream(new FileOutputStream("logs/javaerrors.txt")));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        final MainFrame mainframe = new MainFrame();
        mainframe.setTitle("Organic Traffic Control Manager");

        final ImageIcon icon = new ImageIcon("icons/ente.png");
        mainframe.setIconImage(icon.getImage());

        mainframe.setVisible(true);
    }
}