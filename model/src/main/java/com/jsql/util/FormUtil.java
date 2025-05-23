package com.jsql.util;

import com.jsql.model.InjectionModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;

public class FormUtil {
    
    private static final Logger LOGGER = LogManager.getRootLogger();
    
    private static final String INPUT_ATTR_VALUE = "value";
    private static final String FORM_ATTR_VALUE = "method";
    
    private final InjectionModel injectionModel;
    
    public FormUtil(InjectionModel injectionModel) {
        this.injectionModel = injectionModel;
    }

    public void parseForms(int statusCode, String pageSource) {
        var elementsForm = Jsoup.parse(pageSource).select("form");
        if (elementsForm.isEmpty()) {
            return;
        }
        
        var result = new StringBuilder();
        Map<Element, List<Element>> mapForms = new HashMap<>();
        
        for (Element form: elementsForm) {
            mapForms.put(form, new ArrayList<>());
            result.append(
                String.format(
                    "%n<form action=\"%s\" method=\"%s\" />",
                    form.attr("action"),
                    form.attr(FormUtil.FORM_ATTR_VALUE)
                )
            );
            for (Element input: form.select("input")) {
                result.append(
                    String.format(
                        "%n    <input name=\"%s\" value=\"%s\" />",
                        input.attr("name"),
                        input.attr(FormUtil.INPUT_ATTR_VALUE)
                    )
                );
                mapForms.get(form).add(input);
            }
            Collections.reverse(mapForms.get(form));
        }
            
        if (!this.injectionModel.getMediatorUtils().getPreferencesUtil().isParsingForm()) {
            this.logForms(statusCode, elementsForm, result);
        } else {
            this.addForms(elementsForm, result, mapForms);
        }
    }

    private void addForms(Elements elementsForm, StringBuilder result, Map<Element, List<Element>> mapForms) {
        LOGGER.log(
            LogLevelUtil.CONSOLE_SUCCESS,
            "Found {} <form> in HTML body, adding input(s) to requests: {}",
            elementsForm::size,
            () -> result
        );
        
        for (Entry<Element, List<Element>> form: mapForms.entrySet()) {
            for (Element input: form.getValue()) {
                if (StringUtil.GET.equalsIgnoreCase(form.getKey().attr(FormUtil.FORM_ATTR_VALUE))) {
                    this.injectionModel.getMediatorUtils().getParameterUtil().getListQueryString().add(
                        0,
                        new SimpleEntry<>(
                            input.attr("name"),
                            input.attr(FormUtil.INPUT_ATTR_VALUE)
                        )
                    );
                } else if (StringUtil.POST.equalsIgnoreCase(form.getKey().attr(FormUtil.FORM_ATTR_VALUE))) {
                    this.injectionModel.getMediatorUtils().getParameterUtil().getListRequest().add(
                        0,
                        new SimpleEntry<>(
                            input.attr("name"),
                            input.attr(FormUtil.INPUT_ATTR_VALUE)
                        )
                    );
                }
            }
        }
    }

    private void logForms(int statusCode, Elements elementsForm, StringBuilder result) {
        LOGGER.log(
            LogLevelUtil.CONSOLE_DEFAULT,
            "Found {} ignored <form> in HTML body: {}",
            elementsForm::size,
            () -> result
        );
        if (statusCode != 200) {
            LOGGER.log(LogLevelUtil.CONSOLE_INFORM, "WAF can detect missing form parameters, you may enable 'Add <input/> parameters' in Preferences and retry");
        }
    }
}
