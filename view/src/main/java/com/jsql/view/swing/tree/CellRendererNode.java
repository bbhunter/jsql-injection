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

import com.jsql.view.swing.tree.model.AbstractNodeModel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Render a tree node based on node model.
 * Can render default tree node, or node for database, table or column.
 */
public class CellRendererNode extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(
        JTree tree, Object nodeRenderer, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus
    ) {
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) nodeRenderer;
        var userObject = currentNode.getUserObject();
        AbstractNodeModel dataModel = (AbstractNodeModel) userObject;
        
        return dataModel.getComponent(tree, nodeRenderer, selected, leaf, hasFocus);
    }
}
