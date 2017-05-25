/*
 * matrix-appservice-email - Matrix Bridge to E-mail
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.matrix.bridge.email.config.subscription;

import io.kamax.matrix.bridge.email.model.subscription.SubscriptionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("subscription.notification.email")
public class EmailNotificationConfig implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(EmailNotificationConfig.class);

    private EmailTemplateConfig onCreate;
    private EmailTemplateConfig onDestroy;
    private EmailTemplateConfig onMessage;

    public EmailTemplateConfig getOnCreate() {
        return onCreate;
    }

    public void setOnCreate(EmailTemplateConfig onCreate) {
        this.onCreate = onCreate;
    }

    public EmailTemplateConfig getOnDestroy() {
        return onDestroy;
    }

    public void setOnDestroy(EmailTemplateConfig onDestroy) {
        this.onDestroy = onDestroy;
    }

    public EmailTemplateConfig getOnMessage() {
        return onMessage;
    }

    public void setOnMessage(EmailTemplateConfig onMessage) {
        this.onMessage = onMessage;
    }

    public EmailTemplateConfig get(SubscriptionEvents event) {
        switch (event) {
            case OnCreate:
                return getOnCreate();
            case OnDestroy:
                return getOnDestroy();
            case OnMessage:
                return getOnMessage();
            default:
                return new EmailTemplateConfig();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (SubscriptionEvents eventId : SubscriptionEvents.values()) {
            log.info("Template for {}:", eventId);
            EmailTemplateConfig cfg = get(eventId);
            log.info("Subject: {}", cfg.getSubject());
            for (EmailTemplateContentConfig template : cfg.getContent()) {
                log.info("- Type: {}", template.getType());
                log.info("  Header: {}", template.getHeader());
                log.info("  Footer: {}", template.getFooter());
                log.info("  Content: {}", template.getContent());
                log.info("  Message: {}", template.getMessage());
            }
            log.info("--");
        }
    }

}
