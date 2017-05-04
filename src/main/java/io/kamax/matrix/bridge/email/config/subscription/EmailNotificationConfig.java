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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

@Configuration
@ConfigurationProperties("subscription.notification.email")
public class EmailNotificationConfig {

    private Map<String, EmailTemplate> events;

    public Map<String, EmailTemplate> getEvents() {
        return events;
    }

    public void setEvents(Map<String, EmailTemplate> events) {
        this.events = events;
    }

    public Optional<EmailTemplate> get(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }

}
