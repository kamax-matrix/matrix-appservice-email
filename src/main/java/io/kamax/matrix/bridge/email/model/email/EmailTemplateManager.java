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

package io.kamax.matrix.bridge.email.model.email;

import io.kamax.matrix.bridge.email.config.subscription.EmailNotificationConfig;
import io.kamax.matrix.bridge.email.config.subscription.EmailTemplateConfig;
import io.kamax.matrix.bridge.email.model.subscription.SubscriptionEvents;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EmailTemplateManager implements InitializingBean, _EmailTemplateManager {

    @Autowired
    private ApplicationContext app;

    @Autowired
    private EmailNotificationConfig notifCfg;

    private Map<SubscriptionEvents, List<_EmailTemplate>> templates;

    @Override
    public void afterPropertiesSet() throws Exception {
        templates = new HashMap<>();

        for (SubscriptionEvents ev : SubscriptionEvents.values()) {
            List<_EmailTemplate> t = new ArrayList<>();
            for (EmailTemplateConfig cfg : notifCfg.get(ev)) {
                t.add(app.getBean(EmailTemplate.class, cfg));
            }
            templates.put(ev, t);
        }
    }

    @Override
    public List<_EmailTemplate> get(SubscriptionEvents event) {
        return templates.getOrDefault(event, Collections.emptyList());
    }

}
