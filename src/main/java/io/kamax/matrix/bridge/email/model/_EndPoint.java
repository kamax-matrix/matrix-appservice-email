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

package io.kamax.matrix.bridge.email.model;

import io.kamax.matrix.bridge.email.model.subscription._BridgeSubscription;
import io.kamax.matrix.bridge.email.model.subscription._SubscriptionEvent;

public interface _EndPoint<K, V extends _BridgeMessage, S extends _BridgeMessage> {

    String getId();

    String getChannelId();

    K getIdentity();

    void close();

    void sendEvent(_SubscriptionEvent ev);

    void sendMessage(_BridgeSubscription sub, V msg);

    void addMessageListener(_EndPointMessageListener<S> listener);

    void addStateListener(_EndPointStateListener listener);

    interface _EndPointMessageListener<S> {

        void push(S msg);

    }

    interface _EndPointStateListener {

        void closed(_EndPoint ep);

    }

}
