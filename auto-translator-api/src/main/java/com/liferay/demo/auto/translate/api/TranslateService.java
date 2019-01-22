package com.liferay.demo.auto.translate.api;

import java.util.Locale;

/**
 * @author jverweij
 */
public interface TranslateService {

    public String doTranslateWithLocale(Locale locale, String value);
    public String doTranslate(String language, String value);
}