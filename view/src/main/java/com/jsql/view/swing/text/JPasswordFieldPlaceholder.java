package com.jsql.view.swing.text;

import com.jsql.util.LogLevelUtil;
import com.jsql.view.swing.util.UiUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * Textfield with information text displayed when empty.
 */
public class JPasswordFieldPlaceholder extends JPasswordField implements JPlaceholder {

    private static final Logger LOGGER = LogManager.getRootLogger();

    /**
     * Text to display when empty.
     */
    private String placeholderText;

    /**
     * Create a textfield with hint.
     * @param placeholder Text displayed when empty
     */
    public JPasswordFieldPlaceholder(String placeholder) {
        this.placeholderText = placeholder;
    }

    @Override
    public void paint(Graphics g) {
        try {
            super.paint(g);
        } catch (ClassCastException e) {  // Fix #4301, ClassCastException: sun.awt.image.BufImgSurfaceData cannot be cast to sun.java2d.xr.XRSurfaceData
            LOGGER.log(LogLevelUtil.CONSOLE_JAVA, e, e);
        }
        if (new String(this.getPassword()).isEmpty()) {
            int h = this.getHeight();
            var fm = g.getFontMetrics();
            UiUtil.drawPlaceholder(this, g, this.placeholderText, 0, h / 2 + fm.getAscent() / 2 - 1);
        }
    }

    @Override
    public void setPlaceholderText(String placeholderText) {
        this.placeholderText = placeholderText;
    }
}