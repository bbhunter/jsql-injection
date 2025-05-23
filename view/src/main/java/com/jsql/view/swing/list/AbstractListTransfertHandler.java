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
package com.jsql.view.swing.list;

import com.jsql.util.LogLevelUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Handler for processing cut/copy/paste/drag/drop action on a JList items.
 */
public abstract class AbstractListTransfertHandler extends TransferHandler {
    
    private static final Logger LOGGER = LogManager.getRootLogger();

    /**
     * List of cut/copy/paste/drag/drop items.
     */
    protected transient List<ItemList> dragPaths = null;
    
    protected abstract String initTransferable();
    
    protected abstract void parseStringDrop(TransferSupport support, DnDList list, DefaultListModel<ItemList> listModel);
    
    protected abstract List<Integer> initStringPaste(String clipboardText, int selectedIndex, DefaultListModel<ItemList> listModel);
    
    @Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY_OR_MOVE;
    }
    
    @Override
    protected Transferable createTransferable(JComponent component) {
        DnDList list = (DnDList) component;
        this.dragPaths = list.getSelectedValuesList();
        var stringTransferable = this.initTransferable();
        return new StringSelection(stringTransferable.trim());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        if (action == TransferHandler.MOVE) {
            JList<ItemList> list = (JList<ItemList>) c;
            DefaultListModel<ItemList> model = (DefaultListModel<ItemList>) list.getModel();
            
            for (ItemList itemPath: this.dragPaths) {
                // Unhandled ArrayIndexOutOfBoundsException #56115 on remove()
                try {
                    model.remove(model.indexOf(itemPath));
                } catch (ArrayIndexOutOfBoundsException e) {
                    LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e.getMessage(), e);
                }
            }
            
            this.dragPaths = null;
        }
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.stringFlavor)
            || support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!this.canImport(support)) {
            return false;
        }

        DnDList list = (DnDList) support.getComponent();
        DefaultListModel<ItemList> listModel = (DefaultListModel<ItemList>) list.getModel();
        
        if (support.isDrop()) {  // drop
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                this.parseStringDrop(support, list, listModel);
            } else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                this.parseFileDrop(support, list);
            }
        } else {
            var transferableFromClipboard = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferableFromClipboard != null) {  // paste
                if (transferableFromClipboard.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    this.parseStringPaste(list, listModel, transferableFromClipboard);
                } else if (transferableFromClipboard.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    this.parseFilePaste(list, transferableFromClipboard);
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void parseFileDrop(TransferSupport support, DnDList list) {
        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        int childIndex = dropLocation.getIndex();
        try {
            list.dropPasteFile(
                (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor),
                childIndex
            );
        } catch (UnsupportedFlavorException | IOException e) {
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
        }
    }

    private void parseStringPaste(DnDList list, DefaultListModel<ItemList> listModel, Transferable transferableFromClipboard) {
        try {
            String clipboardText = (String) transferableFromClipboard.getTransferData(DataFlavor.stringFlavor);
            var selectedIndexPaste = Math.max(list.getSelectedIndex(), 0);
            list.clearSelection();
            List<Integer> selectedIndexes = this.initStringPaste(clipboardText, selectedIndexPaste, listModel);
            var selectedIndexesPasted = new int[selectedIndexes.size()];
            var i = 0;
            
            for (Integer selectedIndex: selectedIndexes) {
                selectedIndexesPasted[i] = selectedIndex;
                i++;
            }
            
            list.setSelectedIndices(selectedIndexesPasted);
            list.scrollRectToVisible(
                list.getCellBounds(
                    list.getMinSelectionIndex(),
                    list.getMaxSelectionIndex()
                )
            );
        } catch (NullPointerException | UnsupportedFlavorException | IOException e) {
            // Fix #8831: Multiple Exception on scrollRectToVisible()
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseFilePaste(DnDList list, Transferable transferableFromClipboard) {
        try {
            var selectedIndex = Math.max(list.getSelectedIndex(), 0);
            list.clearSelection();
            list.dropPasteFile(
                (List<File>) transferableFromClipboard.getTransferData(DataFlavor.javaFileListFlavor),
                selectedIndex
            );
        } catch (UnsupportedFlavorException | IOException e) {
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
        }
    }
}
