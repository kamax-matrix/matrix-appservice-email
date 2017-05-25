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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("subscription.notification.matrix")
public class MatrixNotificationConfig {

    private boolean onCreate = false;
    private boolean onDestroy = false;
    private boolean onMessage = false;

    public boolean getOnCreate() {
        return onCreate;
    }

    public void setOnCreate(boolean onCreate) {
        this.onCreate = onCreate;
    }

    public boolean getOnDestroy() {
        return onDestroy;
    }

    public void setOnDestroy(boolean onDestroy) {
        this.onDestroy = onDestroy;
    }

    public boolean getOnMessage() {
        return onMessage;
    }

    public void setOnMessage(boolean onMessage) {
        this.onMessage = onMessage;
    }

    public boolean get(SubscriptionEvents event) {
        switch (event) {
            case OnCreate:
                return getOnCreate();
            case OnDestroy:
                return getOnDestroy();
            case OnMessage:
                return getOnMessage();
            default:
                return false;
        }
    }

}
