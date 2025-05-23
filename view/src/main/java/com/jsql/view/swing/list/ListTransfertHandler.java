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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for processing cut/copy/paste/drag/drop action on a JList items.
 */
public class ListTransfertHandler extends AbstractListTransfertHandler {
    
    private static final Logger LOGGER = LogManager.getRootLogger();

    @Override
    protected String initTransferable() {
        var stringTransferable = new StringBuilder();
        for (ItemList itemPath: this.dragPaths) {
            stringTransferable.append(itemPath).append("\n");
        }
        return stringTransferable.toString();
    }

    @Override
    protected void parseStringDrop(TransferSupport support, DnDList list, DefaultListModel<ItemList> listModel) {
        var dropLocation = (JList.DropLocation) support.getDropLocation();
        int childIndex = dropLocation.getIndex();
        List<Integer> listSelectedIndices = new ArrayList<>();

        // DnD from list
        if (this.dragPaths != null && !this.dragPaths.isEmpty()) {
            this.addFromList(listModel, childIndex, listSelectedIndices);
        } else {
            this.addFromOutside(support, listModel, childIndex, listSelectedIndices);
        }

        var selectedIndices = new int[listSelectedIndices.size()];
        var i = 0;
        for (Integer integer: listSelectedIndices) {
            selectedIndices[i] = integer;
            i++;
        }
        list.setSelectedIndices(selectedIndices);
    }

    private void addFromOutside(TransferSupport support, DefaultListModel<ItemList> listModel, int childIndexFrom, List<Integer> listSelectedIndices) {
        try {
            int childIndexTo = childIndexFrom;
            var importString = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            for (String value: importString.split("\\n")) {
                if (StringUtils.isNotEmpty(value)) {
                    listSelectedIndices.add(childIndexTo);
                    listModel.add(childIndexTo++, new ItemList(value.replace("\\", "/")));
                }
            }
        } catch (UnsupportedFlavorException | IOException e) {
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
        }
    }

    private void addFromList(DefaultListModel<ItemList> listModel, int childIndexFrom, List<Integer> listSelectedIndices) {
        int childIndexTo = childIndexFrom;
        for (ItemList value: this.dragPaths) {
            if (StringUtils.isNotEmpty(value.toString())) {
                var newValue = new ItemList(value.toString().replace("\\", "/"));  //! FUUuu
                listSelectedIndices.add(childIndexTo);
                listModel.add(childIndexTo++, newValue);
            }
        }
    }

    @Override
    protected List<Integer> initStringPaste(String clipboardText, int selectedIndexFrom, DefaultListModel<ItemList> listModel) {
        int selectedIndexTo = selectedIndexFrom;
        List<Integer> selectedIndexes = new ArrayList<>();
        for (String line: clipboardText.split("\\n")) {
            if (StringUtils.isNotEmpty(line)) {
                String newLine = line.replace("\\", "/");
                var newItem = new ItemList(newLine);
                selectedIndexes.add(selectedIndexTo);
                listModel.add(selectedIndexTo++, newItem);
            }
        }
        return selectedIndexes;
    }
}
