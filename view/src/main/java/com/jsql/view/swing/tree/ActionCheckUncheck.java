package com.jsql.view.swing.tree;

import com.jsql.view.swing.tree.model.AbstractNodeModel;
import com.jsql.view.swing.util.MediatorHelper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Check and uncheck column as checkbox.
 */
public class ActionCheckUncheck implements ActionListener {

    private final AbstractNodeModel nodeModel;
    private final DefaultMutableTreeNode currentTableNode;

    public ActionCheckUncheck(AbstractNodeModel nodeModel, DefaultMutableTreeNode currentTableNode) {
        
        this.nodeModel = nodeModel;
        this.currentTableNode = currentTableNode;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        
        Object source = actionEvent.getSource();
        JCheckBox columnCheckBox = (JCheckBox) source;
        this.nodeModel.setSelected(columnCheckBox.isSelected());

        DefaultTreeModel treeModel = (DefaultTreeModel) MediatorHelper.treeDatabase().getModel();
        DefaultMutableTreeNode tableNode = (DefaultMutableTreeNode) this.currentTableNode.getParent();

        int tableChildCount = treeModel.getChildCount(tableNode);
        var isOneChildSelected = false;
        
        for (var i = 0 ; i < tableChildCount ; i++) {
            
            DefaultMutableTreeNode currentChild = (DefaultMutableTreeNode) treeModel.getChild(tableNode, i);
            
            if (currentChild.getUserObject() instanceof AbstractNodeModel) {
                
                AbstractNodeModel columnTreeNodeModel = (AbstractNodeModel) currentChild.getUserObject();
                
                if (columnTreeNodeModel.isSelected()) {
                    
                    isOneChildSelected = true;
                    break;
                }
            }
        }

        AbstractNodeModel nodeUserObject = (AbstractNodeModel) tableNode.getUserObject();
        nodeUserObject.setContainingSelection(isOneChildSelected);
    }
}