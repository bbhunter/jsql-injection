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

import com.jsql.util.I18nUtil;
import com.jsql.util.LogLevelUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A list supporting drag and drop.
 */
public class DnDList extends JList<ItemList> {
    
    private static final Logger LOGGER = LogManager.getRootLogger();
    
    /**
     * Model for the JList.
     */
    protected final DefaultListModel<ItemList> listModel;
    
    /**
     * List of default items.
     */
    private final transient List<ItemList> defaultList;
    
    /**
     * Create a JList decorated with drag/drop features.
     * @param newList List to decorate
     */
    public DnDList(List<ItemList> newList) {
        this.defaultList = newList;
        this.listModel = new DefaultListModel<>();

        for (ItemList path: newList) {
            this.listModel.addElement(path);
        }

        this.setModel(this.listModel);
        this.initActionMap();
        this.initListener();
        this.setDragEnabled(true);
        this.setDropMode(DropMode.INSERT);
        this.setTransferHandler(new ListTransfertHandler());  // Set Drag and Drop
    }

    private void initListener() {
        this.addMouseListener(new MouseAdapterMenuAction(this));
        this.addFocusListener(new FocusListener() {  // Allows color change when list loses/gains focus
            @Override
            public void focusLost(FocusEvent focusEvent) {
                DnDList.this.repaint();
            }
            @Override
            public void focusGained(FocusEvent focusEvent) {
                DnDList.this.repaint();
            }
        });
        this.addKeyListener(new KeyAdapter() {  // Allows deleting values
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_DELETE) {
                    DnDList.this.removeSelectedItem();
                }
            }
        });
    }

    private void initActionMap() {
        var listActionMap = this.getActionMap();  // Transform Cut, selects next value
        listActionMap.put(TransferHandler.getCutAction().getValue(Action.NAME), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (DnDList.this.getSelectedValuesList().isEmpty()) {
                    return;
                }
                
                List<ItemList> selectedValues = DnDList.this.getSelectedValuesList();
                List<ItemList> siblings = new ArrayList<>();
                for (ItemList value: selectedValues) {
                    int valueIndex = DnDList.this.listModel.indexOf(value);
                    if (valueIndex < DnDList.this.listModel.size() - 1) {
                        siblings.add(DnDList.this.listModel.get(valueIndex + 1));
                    } else if (valueIndex > 0) {
                        siblings.add(DnDList.this.listModel.get(valueIndex - 1));
                    }
                }

                TransferHandler.getCutAction().actionPerformed(e);
                
                for (ItemList sibling: siblings) {
                    DnDList.this.setSelectedValue(sibling, true);
                }
            }
        });

        listActionMap.put(
            TransferHandler.getCopyAction().getValue(Action.NAME),
            TransferHandler.getCopyAction()
        );
        listActionMap.put(
            TransferHandler.getPasteAction().getValue(Action.NAME),
            TransferHandler.getPasteAction()
        );
    }

    /**
     * Delete selected items from the list.
     */
    public void removeSelectedItem() {
        if (this.getSelectedValuesList().isEmpty()) {
            return;
        }

        List<ItemList> selectedValues = this.getSelectedValuesList();
        for (ItemList itemSelected: selectedValues) {
            int indexOfItemSelected = this.listModel.indexOf(itemSelected);
            this.listModel.removeElement(itemSelected);
            if (indexOfItemSelected == this.listModel.getSize()) {
                this.setSelectedIndex(indexOfItemSelected - 1);
            } else {
                this.setSelectedIndex(indexOfItemSelected);
            }
        }
        
        try {
            var rectangle = this.getCellBounds(
                this.getMinSelectionIndex(),
                this.getMaxSelectionIndex()
            );
            if (rectangle != null) {
                this.scrollRectToVisible(
                    this.getCellBounds(
                        this.getMinSelectionIndex(),
                        this.getMaxSelectionIndex()
                    )
                );
            }
        } catch (NullPointerException e) {
            // Report NullPointerException #1571 : manual scroll elsewhere then run action
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
        }
    }

    /**
     * Load a file into the list (drag/drop or copy/paste).
     */
    public void dropPasteFile(final List<File> filesToImport, int position) {
        if (filesToImport.isEmpty()) {
            return;
        }
        
        for (File fileToImport : filesToImport) {
            // Report NoSuchMethodError #1617
            if (
                !FilenameUtils
                .getExtension(fileToImport.getPath())
                .matches("txt|csv|ini")
            ) {
                // Fix #42832: ClassCastException on showMessageDialog()
                try {
                    JOptionPane.showMessageDialog(
                        this.getTopLevelAncestor(),
                        I18nUtil.valueByKey("LIST_IMPORT_ERROR_LABEL"),
                        I18nUtil.valueByKey("LIST_IMPORT_ERROR_TITLE"),
                        JOptionPane.ERROR_MESSAGE
                    );
                } catch (ClassCastException e) {
                    LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e.getMessage(), e);
                }
                return;
            }
        }

        var options = new String[] {
            I18nUtil.valueByKey("LIST_IMPORT_CONFIRM_REPLACE"),
            I18nUtil.valueByKey("LIST_IMPORT_CONFIRM_ADD"),
            I18nUtil.valueByKey("LIST_ADD_VALUE_CANCEL")
        };
        int answer = JOptionPane.showOptionDialog(
            this.getTopLevelAncestor(),
            I18nUtil.valueByKey("LIST_IMPORT_CONFIRM_LABEL"),
            I18nUtil.valueByKey("LIST_IMPORT_CONFIRM_TITLE"),
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[2]
        );
        if (answer != JOptionPane.YES_OPTION && answer != JOptionPane.NO_OPTION) {
            return;
        }

        int startPosition = position;
        if (answer == JOptionPane.YES_OPTION) {
            this.listModel.clear();
            startPosition = 0;
        }
        int startPositionFinal = startPosition;
        SwingUtilities.invokeLater(() -> this.addItems(filesToImport, startPositionFinal));
    }

    private void addItems(final List<File> filesToImport, int startPosition) {
        int endPosition = startPosition;
        for (File file: filesToImport) {
            endPosition = this.initItems(endPosition, file);
        }
        if (!this.listModel.isEmpty()) {
            this.setSelectionInterval(startPosition, endPosition - 1);
        }
        
        try {
            this.scrollRectToVisible(
                this.getCellBounds(
                    this.getMinSelectionIndex(),
                    this.getMaxSelectionIndex()
                )
            );
        } catch (NullPointerException e) {
            // Report NullPointerException #1571 : manual scroll elsewhere then run action
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e.getMessage(), e);
        }
    }

    private int initItems(int startPosition, File file) {
        int endPosition = startPosition;
        
        try (
            var fileReader = new FileReader(file, StandardCharsets.UTF_8);
            var bufferedReader = new BufferedReader(fileReader)
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (
                    StringUtils.isNotEmpty(line)
                    // Fix Report #60
                    && 0 <= endPosition
                    && endPosition <= this.listModel.size()
                ) {
                    this.addItem(endPosition++, line);
                }
            }
        } catch (IOException e) {
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e.getMessage(), e);
        }
        
        return endPosition;
    }
    
    public void restore() {
        this.listModel.clear();
        for (ItemList path: this.defaultList) {
            this.listModel.addElement(path);
        }
    }
    
    public void addItem(int endPosition, String line) {
        this.listModel.add(endPosition, new ItemList(line.replace("\\", "/")));
    }
}
