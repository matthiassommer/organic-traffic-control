package de.dfg.oc.otc.manager.gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Shows textual output messages.
 *
 * @author rochner
 */
class TextConsole extends JTextPane {
    TextConsole() {
        super();
        setEditable(false);
    }

    /**
     * Prints the stacktrace of an exception to the console. Text color is set to
     * red.
     *
     * @param exc the exception that will be printed
     */
    final void exceptionToConsole(final Exception exc) {
        try {
            // Set text color to red
            final StyledDocument document = getStyledDocument();
            final int offset = document.getLength();
            final MutableAttributeSet sas = new SimpleAttributeSet();
            sas.addAttribute(StyleConstants.Foreground, Color.RED);

            document.insertString(offset, "\n", sas);

            // Print stacktrace to console
            final StackTraceElement[] trace = exc.getStackTrace();
            for (int i = trace.length - 1; i >= 0; i--) {
                document.insertString(offset, trace[i] + "\n", sas);
            }
            document.insertString(offset, "Message: " + exc.getMessage() + "\n", sas);
            document.insertString(offset, exc.getClass().getName() + "\n", sas);

            setCaretPosition(document.getLength());

            try {
                Container parent = getParent();
                while (!(parent instanceof JTabbedPane) && parent != null) {
                    parent = parent.getParent();
                }

                final JTabbedPane tabs = (JTabbedPane) parent;
                for (int i = 0; i < tabs.getComponentCount(); i++) {
                    if (tabs.getTitleAt(i).equals("Console")) {
                        tabs.setSelectedIndex(i);
                        break;
                    }
                }
            } catch (RuntimeException e) {
                e.getStackTrace();
            }
        } catch (BadLocationException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Prints a message to the console.
     *
     * @param textToAdd the text that will be printed
     */
    final void infoToConsole(final String textToAdd) {
        try {
            final StyledDocument document = getStyledDocument();
            final int offset = document.getLength();
            document.insertString(offset, textToAdd + "\n", new SimpleAttributeSet());
            setCaretPosition(document.getLength());
        } catch (BadLocationException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Prints a message to the console. Textcolor is set to red.
     *
     * @param textToAdd the text that will be printed
     */
    final void warnToConsole(final String textToAdd) {
        try {
            final StyledDocument document = getStyledDocument();
            final int offset = document.getLength();

            final MutableAttributeSet sas = new SimpleAttributeSet();
            sas.addAttribute(StyleConstants.Foreground, Color.RED);

            document.insertString(offset, textToAdd + "\n", sas);

            // Scrolling
            setCaretPosition(document.getLength());
        } catch (BadLocationException e) {
            System.err.println(e.getMessage());
        }
    }
}
