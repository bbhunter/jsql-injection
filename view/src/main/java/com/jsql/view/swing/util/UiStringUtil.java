package com.jsql.view.swing.util;

import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UniversalDetector;

import java.nio.charset.StandardCharsets;

public class UiStringUtil {

    private UiStringUtil() {
        // Utility class
    }
    
    public static String detectUtf8Html(String text) {
        return UiStringUtil.detectUtf8Html(text, false);
    }
    
    public static String detectUtf8HtmlNoWrap(String text) {
        return UiStringUtil.detectUtf8Html(text, true);
    }
    
    public static String detectUtf8Html(String text, boolean nowrap) {
        
        // Fix #35217: NullPointerException on getBytes()
        if (text == null) {
            return StringUtils.EMPTY;
        }
        
        var detector = new UniversalDetector(null);
        
        // Decode bytes for potentially UTF8 chars
        // Required by asian and hindi chars, otherwise wrong display in database tree
        detector.handleData(
            text.getBytes(StandardCharsets.UTF_8),
            0,
            text.length() - 1
        );
        
        detector.dataEnd();
        String result = text;

        // Confirm UTF8
        String encoding = detector.getDetectedCharset();
        if (encoding != null) {
            result = I18nViewUtil.formatNonLatin(text, nowrap ? "white-space:nowrap;" : StringUtils.EMPTY);
        }
        
        return result;
    }
}
