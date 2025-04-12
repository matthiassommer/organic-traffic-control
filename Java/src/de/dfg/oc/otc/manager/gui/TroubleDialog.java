package de.dfg.oc.otc.manager.gui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

/**
 * @author rochner
 *
 */
class TroubleDialog extends JDialog {
    TroubleDialog(final String text) {
        initialize(text);
    }

    private JPanel createContentPane(final String text) {
        final JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.add(createOkButton(), BorderLayout.SOUTH);
        content.add(createNoticeDescription(text), BorderLayout.CENTER);
        return content;
    }

    private JTextArea createNoticeDescription(final String text) {
        final JTextArea noticeDescription = new JTextArea(text);
        noticeDescription.setEditable(false);
        noticeDescription.setRows(30);
        noticeDescription.setWrapStyleWord(true);
        noticeDescription.setLineWrap(true);
        noticeDescription.setBackground(Color.lightGray);
        return noticeDescription;
    }

    private JButton createOkButton() {
        final JButton okButton = new JButton("Ok");
        okButton.addActionListener(e -> dispose());
        return okButton;
    }

    private void initialize(final String text) {
        this.setSize(300, 200);
        this.setTitle("Notice");
        this.setModal(true);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setContentPane(createContentPane(text));

        this.setVisible(true);
    }
}