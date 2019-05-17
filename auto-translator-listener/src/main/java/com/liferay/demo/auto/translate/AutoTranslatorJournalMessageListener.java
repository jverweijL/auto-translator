package com.liferay.demo.auto.translate;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalServiceUtil;
import com.liferay.demo.auto.translate.api.TranslateService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.journal.service.JournalArticleService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.messaging.MessageListenerException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.xml.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component(
        immediate=true,property=("destination.name=" + AutoTranslatorConfigurator.DESTINATION),
        service = MessageListener.class
)
public class AutoTranslatorJournalMessageListener implements MessageListener {

    private long WAITTIME = 2000;


    @Override
    public void receive(Message message) throws MessageListenerException {

        try {
            _log.debug("Let's wait " + WAITTIME + " milliseconds..");
            Thread.sleep(WAITTIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        JournalArticle article = null;
        AssetEntry entry = null;
        try {
            long groupid = (long)message.get("groupId");
            Locale defaultLocale = PortalUtil.getSiteDefaultLocale(groupid);
            article = _JournalArticleLocalService.getArticle(groupid,(String)message.get("articleId"));
            entry = _AssetEntryLocalService.getEntry(JournalArticle.class.getName(),article.getResourcePrimKey());
            final Set<Locale> availableLocales = LanguageUtil.getAvailableLocales(groupid);


            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setCompanyId(entry.getCompanyId());
            serviceContext.setScopeGroupId(article.getGroupId());

            if (article != null) {

                if (mustbeTranslated(entry)) {
                    // fields we will translate
                    // for now just title and summary/description
                    Map<Locale, String> titleMap = article.getTitleMap();

                    for (Locale locale : LanguageUtil.getAvailableLocales(article.getGroupId())) {
                        if (!locale.equals(defaultLocale)) {
                            // translate title
                            String result = _TranslateService.doTranslate(defaultLocale.getLanguage(), locale.getLanguage(), article.getTitle());
                            if (!result.isEmpty()) {
                                _log.debug("translation for " + locale.getLanguage() + ":  " + result);
                                if (titleMap.containsKey(locale)) {
                                    titleMap.replace(locale, result);
                                } else {
                                    titleMap.put(locale,result);
                                }
                            }

                            // translate content
                            Boolean overwriteTranslation = Boolean.getBoolean(PortalUtil.getPortalProperties().getProperty("aws.translate.override","false"));
                            SAXReader reader = SAXReaderUtil.getSAXReader();
                            Document document;
                            if (overwriteTranslation) {
                                document = reader.read(article.getContentByLocale(defaultLocale.getLanguage()));
                            } else {
                                document = reader.read(article.getContent());
                            }

                            // TODO process document element by element
                            ///liferay/development/sources/liferay71/liferay-portal/modules/apps/adaptive-media/adaptive-media-web/src/main/java/com/liferay/adaptive/media/web/internal/upgrade/v1_0_0/UpgradeJournalArticleDataFileEntryId.java
                            XPath xPath = SAXReaderUtil.createXPath(
                                    "//dynamic-element[@type='text_area']");
                            List<Node> nodes = xPath.selectNodes(document);

                            for (Node node : nodes) {

                            }

                            article.setContent(document.compactString());
                        }
                    }

                    _JournalArticleLocalService.updateArticle(entry.getUserId(),
                                                              entry.getGroupId(),
                                                              article.getFolderId(),
                                                              article.getArticleId(),
                                                              article.getVersion(),
                                                              titleMap,article.getDescriptionMap(),
                                                              article.getContent(),
                                                              article.getLayoutUuid(),
                                                              serviceContext);
                }
            }
        } catch (PortalException | DocumentException | IOException e) {
            e.printStackTrace();
        }
    }

    /*public static void translateElement(Element ele, AmazonTranslate translate) {

        // Check if the element has any text
        if (!ele.ownText().isEmpty()) {

            // Retrieve the text of the HTML element
            String text = ele.ownText();

            // Now translate the element's text
            try {

                // Translate from English to Spanish
                TranslateTextRequest request = new TranslateTextRequest()
                        .withText(text)
                        .withSourceLanguageCode("en")
                        .withTargetLanguageCode("es");

                // Retrieve the result
                TranslateTextResult result  = translate.translateText(request);

                // Record the original and translated text
                System.out.println("Original text: " + text + " - Translated text: "+ result.getTranslatedText());

                // Update the HTML element with the translated text
                ele.text(result.getTranslatedText());

                // Catch any translation errors
            } catch (AmazonServiceException e) {
                System.err.println(e.getErrorMessage());
                System.exit(1);
            }
        } else {
            // We have found a non-text HTML element. No action required.
        }
    }*/

    public boolean mustbeTranslated(AssetEntry entry) throws PortalException {
        //only autotag if there's an autotag tag or if it's empty
        String triggerTagName = "autotranslate";
        if (triggerTagName.isEmpty()) {
            _log.debug("No trigger tagname found");
            return true;
        } else {
            _log.debug("Checking entry for tag " + triggerTagName);
            AssetTag triggerTag = AssetTagLocalServiceUtil.getTag(entry.getGroupId(), triggerTagName);
            _log.debug("Entry has triggertag: " + entry.getTags().contains(triggerTag));
            return entry.getTags().contains(triggerTag);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected JournalArticleLocalService _JournalArticleLocalService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected AssetEntryLocalService _AssetEntryLocalService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TranslateService _TranslateService;




    private static final Log _log = LogFactoryUtil.getLog(AutoTranslatorJournalMessageListener.class);
}
