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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public abstract class AEndPoint<K, V extends _BridgeMessage, S extends _BridgeMessage> implements _EndPoint<K, V, S> {

    private Logger log = LoggerFactory.getLogger(AEndPoint.class);

    private String id;
    private K identity;
    private String channel;

    private boolean isClosed;
    private List<_EndPointMessageListener<S>> msgListeners = new ArrayList<>();
    private List<_EndPointStateListener> stateListeners = new ArrayList<>();

    public AEndPoint(String id, K identity, String channel) {
        this.id = id;
        this.identity = identity;
        this.channel = channel;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getChannelId() {
        return channel;
    }

    @Override
    public K getIdentity() {
        return identity;
    }

    protected boolean isClosed() {
        return isClosed;
    }

    protected abstract void sendEventImpl(_SubscriptionEvent ev);

    @Override
    public void sendEvent(_SubscriptionEvent ev) {
        if (isClosed()) {
            log.info("Ignoring subscription event {} notification, endpoint {} is closed", ev.getType(), getId());
            return;
        }

        sendEventImpl(ev);
    }

    protected abstract void sendMessageImpl(_BridgeSubscription sub, V msg);

    @Override
    public void sendMessage(_BridgeSubscription sub, V msg) {
        if (isClosed()) {
            log.info("Ignoring message {}, endpoint {} is closed", msg.getKey(), getId());
            return;
        }

        sendMessageImpl(sub, msg);
    }

    protected abstract void closeImpl();

    @Override
    public void close() {
        log.info("Closing endpoint for user {} in channel {}", getIdentity(), getChannelId());

        closeImpl();
        isClosed = true;

        fireClosedEvent();
    }

    @Override
    public void addMessageListener(_EndPointMessageListener<S> listener) {
        log.info("Adding message listener to endpoint {}", id);

        msgListeners.add(listener);
    }

    public void addStateListener(_EndPointStateListener listener) {
        log.info("Adding state listener to endpoint {}", id);

        stateListeners.add(listener);
    }

    protected void fireMessageEvent(S msg) {
        log.info("Sending message event to {} listeners", msgListeners.size());

        for (_EndPointMessageListener<S> listener : msgListeners) {
            listener.push(msg);
        }
    }

    protected void fireClosedEvent() {
        log.info("Sending close event to {} listeners", msgListeners.size());

        for (_EndPointStateListener listener : stateListeners) {
            listener.closed(this);
        }
    }

}
