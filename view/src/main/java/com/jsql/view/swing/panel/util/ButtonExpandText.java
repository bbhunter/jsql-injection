/*******************************************************************************
 * Copyhacked (H) 2012-2025.
 * This program and the accompanying materials
 * are made available under no term at all, use it like
 * you want, but share and discuss it
 * every time possible with every body.
 *
 * Contributors:
 *      ron190 at ymail dot com - initial implementation
 *******************************************************************************/
package com.jsql.view.swing.panel.util;

import com.jsql.view.swing.text.JPopupTextArea;
import com.jsql.view.swing.text.JTextAreaPlaceholder;
import com.jsql.view.swing.util.MediatorHelper;
import com.jsql.view.swing.util.UiUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A button displayed in address.
 */
public class ButtonExpandText extends JButton {

    /**
     * Create a button in address bar.
     */
    public ButtonExpandText(String titleFrame, JTextField sourceTextField) {
        this.setPreferredSize(new Dimension(16, 16));
        this.setContentAreaFilled(false);

        this.setIcon(UiUtil.EXPAND.getIcon());
        this.setRolloverIcon(UiUtil.EXPAND_HOVER.getIcon());
        this.setPressedIcon(UiUtil.EXPAND_PRESSED.getIcon());

        JTextArea textAreaInDialog = new JPopupTextArea(new JTextAreaPlaceholder("Multiline text")).getProxy();
        textAreaInDialog.getCaret().setBlinkRate(500);

        final JDialog dialogWithTextarea = new JDialog(MediatorHelper.frame(), titleFrame, true);
        dialogWithTextarea.getContentPane().add(new JScrollPane(textAreaInDialog));
        dialogWithTextarea.pack();
        dialogWithTextarea.setSize(400, 300);
        dialogWithTextarea.setLocationRelativeTo(null);
        dialogWithTextarea.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sourceTextField.setText(textAreaInDialog.getText().replace("\n", "\\n").replace("\r", "\\r"));
                super.windowClosing(e);
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                textAreaInDialog.setText(sourceTextField.getText().replace("\\n", "\n").replace("\\r", "\r"));
                dialogWithTextarea.setVisible(!dialogWithTextarea.isVisible());
            }
        });

        dialogWithTextarea.getRootPane()
            .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "Cancel"
            );
        dialogWithTextarea.getRootPane()
            .getActionMap()
            .put("Cancel", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    dialogWithTextarea.dispose();
                }
            });
    }
}
