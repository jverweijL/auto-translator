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
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

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
            article = _JournalArticleLocalService.getArticle((long)message.get("groupId"),(String)message.get("articleId"));
            entry = _AssetEntryLocalService.getEntry(JournalArticle.class.getName(),article.getResourcePrimKey());

            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setCompanyId(entry.getCompanyId());
            serviceContext.setScopeGroupId(article.getGroupId());

            if (article != null) {

                if (mustbeTranslated(entry)) {
                    // fields we will translate
                    // for now just title and summary/description
                    Map<Locale, String> titleMap = article.getTitleMap();

                    for (Locale locale : LanguageUtil.getAvailableLocales(article.getGroupId())) {
                        // TODO lookup default locale and skip this.
                        if (!locale.equals(Locale.US)) {
                            String result = _TranslateService.doTranslate(locale.getLanguage(), article.getTitle());
                            if (!result.isEmpty()) {
                                _log.debug("translation for " + locale.getLanguage() + ":  " + result);
                                if (titleMap.containsKey(locale)) {
                                    titleMap.replace(locale, result);
                                } else {
                                    titleMap.put(locale,result);
                                }
                            }
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
        } catch (PortalException e) {
            e.printStackTrace();
        }
    }

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
