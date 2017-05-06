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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@ConfigurationProperties("subscription.notification.email")
public class EmailNotificationConfig implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(EmailNotificationConfig.class);

    private List<EmailTemplateConfig> onCreate = new ArrayList<>();
    private List<EmailTemplateConfig> onDestroy = new ArrayList<>();
    private List<EmailTemplateConfig> onMessage = new ArrayList<>();
    private List<EmailTemplateConfig> onMute = new ArrayList<>();
    private List<EmailTemplateConfig> onUnmute = new ArrayList<>();

    public List<EmailTemplateConfig> getOnCreate() {
        return onCreate;
    }

    public void setOnCreate(List<EmailTemplateConfig> onCreate) {
        this.onCreate = onCreate;
    }

    public List<EmailTemplateConfig> getOnDestroy() {
        return onDestroy;
    }

    public void setOnDestroy(List<EmailTemplateConfig> onDestroy) {
        this.onDestroy = onDestroy;
    }

    public List<EmailTemplateConfig> getOnMessage() {
        return onMessage;
    }

    public void setOnMessage(List<EmailTemplateConfig> onMessage) {
        this.onMessage = onMessage;
    }

    public List<EmailTemplateConfig> getOnMute() {
        return onMute;
    }

    public void setOnMute(List<EmailTemplateConfig> onMute) {
        this.onMute = onMute;
    }

    public List<EmailTemplateConfig> getOnUnmute() {
        return onUnmute;
    }

    public void setOnUnmute(List<EmailTemplateConfig> onUnmute) {
        this.onUnmute = onUnmute;
    }

    public List<EmailTemplateConfig> get(SubscriptionEvents event) {
        switch (event) {
            case OnCreate:
                return getOnCreate();
            case OnDestroy:
                return getOnDestroy();
            case OnMessage:
                return getOnMessage();
            case OnMute:
                return getOnMute();
            case OnUnmute:
                return getOnUnmute();
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (SubscriptionEvents eventId : SubscriptionEvents.values()) {
            log.info("Templates for {}:", eventId);
            for (EmailTemplateConfig template : get(eventId)) {
                log.info("- Type: {}", template.getType());
                log.info("  Header: {}", template.getHeader());
                log.info("  Footer: {}", template.getFooter());
                log.info("  Content: {}", template.getContent());
                log.info("  Message: {}", template.getMessage());
            }
        }
    }

}
