package com.liferay.demo.auto.translate.service;

import com.amazonaws.auth.*;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;
import com.liferay.demo.auto.translate.api.TranslateService;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import org.osgi.service.component.annotations.Component;

import java.util.Locale;

/**
 * @author jverweij
 */
@Component(
	immediate = true,
	property = {
		// TODO enter required service properties
	},
	service = TranslateService.class
)
public class TranslateServiceImpl implements TranslateService {
	private static final String REGION = "eu-west-1";

	@Override
	public String doTranslateWithLocale(Locale locale, String value) {
		return this.doTranslate(locale.getLanguage(),value);
	}

	@Override
	public String doTranslate(String language, String value) {
		try {
			_log.debug("Translate from [en] to [" + language + "]: " + value);

			// Create credentials using a provider chain. For more information, see
			// https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
			_log.debug("key: " + PortalUtil.getPortalProperties().getProperty("aws.accessKeyId"));
			BasicAWSCredentials awsCreds = new BasicAWSCredentials(PortalUtil.getPortalProperties().getProperty("aws.accessKeyId"), PortalUtil.getPortalProperties().getProperty("aws.secretKey"));

			AmazonTranslate translate = AmazonTranslateClient.builder()
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
					.withRegion(REGION)
					.build();

			TranslateTextRequest request = new TranslateTextRequest()
					.withText(value)
					.withSourceLanguageCode("en")
					.withTargetLanguageCode(language);
			TranslateTextResult result  = translate.translateText(request);
			_log.debug(result.getTranslatedText());
			return result.getTranslatedText();

		} catch (Exception ex) {
			_log.error("Error: " + ex.getMessage());
			return "";
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(TranslateServiceImpl.class);
}