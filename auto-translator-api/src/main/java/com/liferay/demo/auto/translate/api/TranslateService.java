package com.liferay.demo.auto.translate.api;

import java.util.Locale;

/**
 * @author jverweij
 */
public interface TranslateService {

    public String doTranslateWithLocale(Locale from, Locale to, String value);
    public String doTranslate(String fromLanguage, String toLanguage, String value);
}