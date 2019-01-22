package com.liferay.demo.auto.translate;

import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBus;
import com.liferay.portal.kernel.model.BaseModelListener;

import com.liferay.portal.kernel.model.ModelListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author jverweij
 */
@Component(
		immediate = true,
		name = "AutoTranslatorBaseModelListener",
		property = {
				// TODO enter required service properties
		},
		service = ModelListener.class
)
public class AutoTranslatorBaseModelListener extends BaseModelListener<JournalArticle> {

	private static Log _log = LogFactoryUtil.getLog(AutoTranslatorBaseModelListener.class);

	@Override
	public void onAfterCreate(JournalArticle article) throws ModelListenerException {
		super.onAfterCreate(article);

		//put it on the message bus since OnAfterCreate is a bit misleading... it's not all materialized in the DB yet.
		//Fire and forget principle, listeners should handle this
        _log.debug("Sending translator message to messagebus");
		Message message = new Message();
		message.put("articleId", article.getArticleId());
		message.put("groupId", article.getGroupId());
		_MessageBus.sendMessage(AutoTranslatorConfigurator.DESTINATION, message);
	}

	@Reference
	MessageBus _MessageBus;

}