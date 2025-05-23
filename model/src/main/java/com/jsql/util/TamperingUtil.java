package com.jsql.util;

import com.jsql.util.tampering.TamperingType;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.regex.Pattern;

public class TamperingUtil {

    private static final Logger LOGGER = LogManager.getRootLogger();

    public static final String TAG_OPENED = "<tampering>";
    public static final String TAG_CLOSED = "</tampering>";

    private boolean isBase64 = false;
    private boolean isVersionComment = false;
    private boolean isFunctionComment = false;
    private boolean isEqualToLike = false;
    private boolean isRandomCase = false;
    private boolean isHexToChar = false;
    private boolean isStringToChar = false;
    private boolean isQuoteToUtf8 = false;
    private boolean isEval = false;
    private boolean isSpaceToMultilineComment = false;
    private boolean isSpaceToDashComment = false;
    private boolean isSpaceToSharpComment = false;

    private String customTamper = null;

    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();

    private static String eval(String sqlQuery, String jsTampering) {
        Object resultSqlTampered;
        try {
            if (StringUtils.isEmpty(jsTampering)) {
                throw new ScriptException("Tampering context is empty");
            }

            ScriptEngine nashornEngine = TamperingUtil.SCRIPT_ENGINE_MANAGER.getEngineByName("nashorn");
            nashornEngine.eval(jsTampering);

            var nashornInvocable = (Invocable) nashornEngine;
            resultSqlTampered = nashornInvocable.invokeFunction("tampering", sqlQuery);

        } catch (ScriptException e) {
            LOGGER.log(
                LogLevelUtil.CONSOLE_ERROR,
                String.format("Tampering context contains errors: %s", e.getMessage()),
                e
            );
            resultSqlTampered = sqlQuery;
        } catch (NoSuchMethodException e) {
            LOGGER.log(
                LogLevelUtil.CONSOLE_ERROR,
                String.format("Tampering context is not properly defined: %s", e.getMessage()),
                e
            );
            LOGGER.log(LogLevelUtil.CONSOLE_ERROR, "Minimal tampering context is: var tampering = function(sql) {return sql}");
            resultSqlTampered = sqlQuery;
        }
        return resultSqlTampered.toString();
    }

    public String tamper(String sqlQueryDefault) {
        String lead;
        String sqlQuery;
        String trail;

        // Transform only SQL query without HTTP parameters and syntax changed, like p=1'+[sql]
        String regexToMatchTamperTags = String.format("(?s)(.*%s)(.*)(%s.*)", TamperingUtil.TAG_OPENED, TamperingUtil.TAG_CLOSED);
        var matcherSql = Pattern.compile(regexToMatchTamperTags).matcher(sqlQueryDefault);

        if (matcherSql.find()) {
            lead = matcherSql.group(1);
            sqlQuery = matcherSql.group(2);
            trail = matcherSql.group(3);
        } else {
            return sqlQueryDefault;
        }

        if (this.isEval) {
            sqlQuery = TamperingUtil.eval(sqlQuery, this.customTamper);
        }

        sqlQuery = this.transform(sqlQuery, this.isHexToChar, TamperingType.HEX_TO_CHAR);
        sqlQuery = this.transform(sqlQuery, this.isStringToChar, TamperingType.STRING_TO_CHAR);
        sqlQuery = this.transform(sqlQuery, this.isFunctionComment, TamperingType.COMMENT_TO_METHOD_SIGNATURE);
        sqlQuery = this.transform(sqlQuery, this.isVersionComment, TamperingType.VERSIONED_COMMENT_TO_METHOD_SIGNATURE);
        sqlQuery = this.transform(sqlQuery, this.isRandomCase, TamperingType.RANDOM_CASE);
        sqlQuery = this.transform(sqlQuery, this.isEqualToLike, TamperingType.EQUAL_TO_LIKE);

        sqlQuery = lead + sqlQuery + trail;

        String regexToremoveTamperTags = String.format("(?i)%s|%s", TamperingUtil.TAG_OPENED, TamperingUtil.TAG_CLOSED);
        sqlQuery = sqlQuery.replaceAll(regexToremoveTamperTags, StringUtils.EMPTY);

        // Empty when checking character insertion
        if (StringUtils.isEmpty(sqlQuery)) {
            return StringUtils.EMPTY;
        }

        // Transform all query, SQL and HTTP

        // Dependency to: EQUAL_TO_LIKE
        if (this.isSpaceToDashComment) {
            sqlQuery = TamperingUtil.eval(sqlQuery, TamperingType.SPACE_TO_DASH_COMMENT.instance().getJavascript());
        } else if (this.isSpaceToMultilineComment) {
            sqlQuery = TamperingUtil.eval(sqlQuery, TamperingType.SPACE_TO_MULTILINE_COMMENT.instance().getJavascript());
        } else if (this.isSpaceToSharpComment) {
            sqlQuery = TamperingUtil.eval(sqlQuery, TamperingType.SPACE_TO_SHARP_COMMENT.instance().getJavascript());
        }

        sqlQuery = this.transform(sqlQuery, this.isBase64, TamperingType.BASE64);
        sqlQuery = this.transform(sqlQuery, this.isQuoteToUtf8, TamperingType.QUOTE_TO_UTF8);  // char insertion included
        return sqlQuery;
    }

    private String transform(String sqlQuery, boolean shouldApply, TamperingType tamperingType) {
        if (shouldApply) {
            return TamperingUtil.eval(sqlQuery, tamperingType.instance().getJavascript());
        }
        return sqlQuery;
    }


    // Builder

    public TamperingUtil withBase64() {
        this.isBase64 = true;
        return this;
    }

    public TamperingUtil withVersionComment() {
        this.isVersionComment = true;
        return this;
    }

    public TamperingUtil withFunctionComment() {
        this.isFunctionComment = true;
        return this;
    }

    public TamperingUtil withEqualToLike() {
        this.isEqualToLike = true;
        return this;
    }

    public TamperingUtil withRandomCase() {
        this.isRandomCase = true;
        return this;
    }

    public TamperingUtil withHexToChar() {
        this.isHexToChar = true;
        return this;
    }

    public TamperingUtil withStringToChar() {
        this.isStringToChar = true;
        return this;
    }

    public TamperingUtil withQuoteToUtf8() {
        this.isQuoteToUtf8 = true;
        return this;
    }

    public TamperingUtil withEval() {
        this.isEval = true;
        return this;
    }

    public TamperingUtil withSpaceToMultilineComment() {
        this.isSpaceToMultilineComment = true;
        return this;
    }

    public TamperingUtil withSpaceToDashComment() {
        this.isSpaceToDashComment = true;
        return this;
    }

    public TamperingUtil withSpaceToSharpComment() {
        this.isSpaceToSharpComment = true;
        return this;
    }

    
    // Getter and setter

    public String getCustomTamper() {
        return this.customTamper;
    }

    public void setCustomTamper(String customTamper) {
        this.customTamper = customTamper;
    }

    public TamperingUtil withBase64(boolean selected) {
        this.isBase64 = selected;
        return this;
    }

    public TamperingUtil withEqualToLike(boolean selected) {
        this.isEqualToLike = selected;
        return this;
    }

    public TamperingUtil withEval(boolean selected) {
        this.isEval = selected;
        return this;
    }

    public TamperingUtil withFunctionComment(boolean selected) {
        this.isFunctionComment = selected;
        return this;
    }

    public TamperingUtil withHexToChar(boolean selected) {
        this.isHexToChar = selected;
        return this;
    }

    public TamperingUtil withQuoteToUtf8(boolean selected) {
        this.isQuoteToUtf8 = selected;
        return this;
    }

    public TamperingUtil withRandomCase(boolean selected) {
        this.isRandomCase = selected;
        return this;
    }

    public TamperingUtil withSpaceToDashComment(boolean selected) {
        this.isSpaceToDashComment = selected;
        return this;
    }

    public TamperingUtil withSpaceToMultilineComment(boolean selected) {
        this.isSpaceToMultilineComment = selected;
        return this;
    }

    public TamperingUtil withSpaceToSharpComment(boolean selected) {
        this.isSpaceToSharpComment = selected;
        return this;
    }

    public TamperingUtil withStringToChar(boolean selected) {
        this.isStringToChar = selected;
        return this;
    }

    public TamperingUtil withVersionComment(boolean selected) {
        this.isVersionComment = selected;
        return this;
    }
}
