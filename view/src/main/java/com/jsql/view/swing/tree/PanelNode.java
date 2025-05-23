/*******************************************************************************
 * Copyhacked (H) 2012-2025.
 * This program and the accompanying materials
 * are made available under no term at all, use it like
 * you want, but share and discuss it
 * every time possible with every body.
 *
 * Contributors:
 *      ron190 at ymail dot com - initial implementation
 ******************************************************************************/
package com.jsql.view.swing.tree;

import com.jsql.util.I18nUtil;
import com.jsql.view.swing.tree.model.AbstractNodeModel;
import com.jsql.view.swing.util.UiStringUtil;
import com.jsql.view.swing.util.UiUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

/**
 * A tree Node composed of an icon, a GIF loader, a progress bar, a label.
 */
public class PanelNode extends JPanel {
    
    /**
     * Default icon of the node (database or table).
     */
    private final JLabel iconNode = new JLabel();

    /**
     * A GIF loader, displayed if progress track is unknown (like columns).
     */
    private final JLabel loaderWait = new JLabel();

    /**
     * Progress bar displayed during injection, with pause icon displayed if user paused the process.
     */
    private final ProgressBarPausable progressBar = new ProgressBarPausable();

    /**
     * Text of the node.
     */
    private final JLabel nodeLabel = new JLabel();
    private final JTextField textFieldEditable = new JTextField(15);
    
    /**
     * Create Panel for tree nodes.
     * @param tree JTree to populate
     * @param currentNode Node to draw in the tree
     */
    public PanelNode(final JTree tree, final DefaultMutableTreeNode currentNode) {
        this.loaderWait.setIcon(UiUtil.HOURGLASS.getIcon());
        this.loaderWait.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        this.progressBar.setPreferredSize(new Dimension(20, 20));
        this.progressBar.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));

        this.nodeLabel.setOpaque(true);

        this.iconNode.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        
        Stream.of(
            this.iconNode,
            this.loaderWait,
            this.progressBar,
            this.nodeLabel,
            this.textFieldEditable
        )
        .forEach(component -> {
            this.add(component);
            component.setVisible(false);
        });
        
        this.setComponentOrientation(ComponentOrientation.getOrientation(I18nUtil.getCurrentLocale()));
        
        this.initTextFieldEditable(tree, currentNode);
    }

    private void initTextFieldEditable(final JTree tree, final DefaultMutableTreeNode currentNode) {
        this.textFieldEditable.addActionListener(e -> {
            AbstractNodeModel nodeModel = (AbstractNodeModel) currentNode.getUserObject();
            nodeModel.setIsEdited(false);
            
            this.nodeLabel.setVisible(true);
            this.textFieldEditable.setVisible(false);
            tree.requestFocusInWindow();

            nodeModel.getElementDatabase().setElementValue(new String(
                this.textFieldEditable.getText().getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
            ));
            this.nodeLabel.setText(UiStringUtil.detectUtf8Html(nodeModel.getElementDatabase().getLabelWithCount()));
            
            tree.revalidate();
            tree.repaint();
        });
        
        this.textFieldEditable.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                AbstractNodeModel nodeModel = (AbstractNodeModel) currentNode.getUserObject();
                nodeModel.setIsEdited(false);
                tree.revalidate();
                tree.repaint();
            }
        });
        
        KeyAdapter keyAdapterF2 = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                AbstractNodeModel nodeModel = (AbstractNodeModel) currentNode.getUserObject();
                
                if (e.getKeyCode() == KeyEvent.VK_F2 && !nodeModel.isRunning()) {
                    nodeModel.setIsEdited(true);
                    
                    PanelNode.this.nodeLabel.setVisible(false);
                    PanelNode.this.textFieldEditable.setVisible(true);
                    PanelNode.this.textFieldEditable.requestFocusInWindow();
                    
                    tree.revalidate();
                    tree.repaint();
                }
            }
        };
        
        this.addKeyListener(keyAdapterF2);
        this.textFieldEditable.addKeyListener(keyAdapterF2);
    }

    /**
     * Change the text icon.
     * @param newIcon An icon to display next to the text.
     */
    public void setIconNode(Icon newIcon) {
        this.iconNode.setIcon(newIcon);
    }
    
    /**
     * Display text icon to the left.
     */
    public void showIcon() {
        this.iconNode.setVisible(true);
    }
    
    /**
     * Mask the node icon for example when the loader component is displayed.
     */
    public void hideIcon() {
        this.iconNode.setVisible(false);
    }
    
    /**
     * Change the loader icon.
     * @param newIcon An icon to display for the loader.
     */
    public void setLoaderIcon(Icon newIcon) {
        this.loaderWait.setIcon(newIcon);
    }

    /**
     * Display the animated gif loader.
     */
    public void showLoader() {
        this.loaderWait.setVisible(true);
    }
    
    
    // Getter and setter

    public ProgressBarPausable getProgressBar() {
        return this.progressBar;
    }

    public JLabel getNodeLabel() {
        return this.nodeLabel;
    }

    public JTextField getTextFieldEditable() {
        return this.textFieldEditable;
    }
}
