package com.jsql.view.swing.tab;

import com.jsql.util.I18nUtil;
import com.jsql.util.LogLevelUtil;
import com.jsql.view.swing.util.MediatorHelper;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Display popupmenu on right click.
 * Used on manager tabs.
 */
public class TabMouseAdapter extends MouseAdapter {
    
    /**
     * Log4j logger sent to view.
     */
    private static final Logger LOGGER = LogManager.getRootLogger();
    
    private final TabbedPaneWheeled tabbedPaneWheeled;
    
    public TabMouseAdapter(TabbedPaneWheeled tabbedPaneWheeled) {
        this.tabbedPaneWheeled = tabbedPaneWheeled;
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        
        if (!SwingUtilities.isRightMouseButton(event)) {
            return;
        }
            
        var componentSource = (Component) event.getSource();
        var menu = new JPopupMenu();

        // Copy menu items from menubar
        for (
            var position = 0
            ; position < MediatorHelper.menubar().getMenuView().getMenuComponentCount()
            ; position++
        ) {
            
            // Fix #35348: SerializationException on clone()
            try {
                JMenuItem itemMenu = (JMenuItem) SerializationUtils.clone(MediatorHelper.menubar().getMenuView().getMenuComponent(position));
                menu.add(itemMenu);
                
                final int positionFinal = position;
                itemMenu.addActionListener(actionEvent -> this.tabbedPaneWheeled.setSelectedIndex(positionFinal));
                
            } catch (SerializationException e) {
                LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
            }
        }

        menu.show(componentSource, event.getX(), event.getY());
        
        menu.setLocation(
            ComponentOrientation.RIGHT_TO_LEFT.equals(ComponentOrientation.getOrientation(I18nUtil.getLocaleDefault()))
            ? event.getXOnScreen() - menu.getWidth()
            : event.getXOnScreen(),
            event.getYOnScreen()
        );
    }
}