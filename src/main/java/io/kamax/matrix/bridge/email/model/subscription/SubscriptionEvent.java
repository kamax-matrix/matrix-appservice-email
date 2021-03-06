/*
 * matrix-appservice-email - Matrix Bridge to E-mail
 * Copyright (C) 2017 Kamax Sarl
 *
 * https://www.kamax.io/
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

package io.kamax.matrix.bridge.email.model.subscription;

import java.time.Instant;

public class SubscriptionEvent implements _SubscriptionEvent {

    private SubscriptionEvents type;
    private _BridgeSubscription sub;
    private Instant time;
    private String initiator;

    public SubscriptionEvent(SubscriptionEvents type, _BridgeSubscription sub, Instant time, String initiator) {
        this.type = type;
        this.sub = sub;
        this.time = time;
        this.initiator = initiator;
    }

    @Override
    public SubscriptionEvents getType() {
        return type;
    }

    @Override
    public Instant getTime() {
        return time;
    }

    @Override
    public String getInitiator() {
        return initiator;
    }

    @Override
    public _BridgeSubscription getSubscription() {
        return sub;
    }

}
