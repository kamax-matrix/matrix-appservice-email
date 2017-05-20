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

package io.kamax.matrix.bridge.email.model.subscription;

import io.kamax.matrix.bridge.email.model._MessageFormatter;
import io.kamax.matrix.bridge.email.model.email._EmailEndPoint;
import io.kamax.matrix.bridge.email.model.matrix._MatrixEndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BridgeSubscription implements _BridgeSubscription {

    private Logger log = LoggerFactory.getLogger(BridgeSubscription.class);

    private String sourceMxId;
    private Instant timestamp;
    private String id;
    private String emKey;
    private String mxKey;
    private _MatrixEndPoint mxEp;
    private _EmailEndPoint emEp;

    private boolean isClosed;
    private List<_BridgeSubscriptionListener> listeners = new ArrayList<>();

    public BridgeSubscription(String id, String sourceMxId, Instant timestamp, _MessageFormatter formatter, String emKey, _EmailEndPoint emEp, String mxKey, _MatrixEndPoint mxEp) {
        this.id = id;
        this.sourceMxId = sourceMxId;
        this.timestamp = timestamp;
        this.emKey = emKey;
        this.mxKey = mxKey;
        this.mxEp = mxEp;
        this.emEp = emEp;

        mxEp.addMessageListener(msg -> emEp.sendMessage(this, formatter.format(msg)));
        emEp.addMessageListener(msg -> mxEp.sendMessage(this, formatter.format(msg)));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getInitiator() {
        return sourceMxId;
    }

    @Override
    public String getEmailKey() {
        return emKey;
    }

    @Override
    public String getMatrixKey() {
        return mxKey;
    }

    @Override
    public _MatrixEndPoint getMatrixEndpoint() {
        return mxEp;
    }

    @Override
    public _EmailEndPoint getEmailEndpoint() {
        return emEp;
    }

    @Override
    public void commence() {
        if (isClosed) {
            throw new IllegalStateException();
        }

        log.info("Commencing subscription {} | Matrix - ID: {} - Identity: {} | Email - ID: {} - Identity: {}",
                id,
                mxKey,
                mxEp.getIdentity(),
                emKey,
                emEp.getIdentity());

        SubscriptionEvent ev = new SubscriptionEvent(SubscriptionEvents.OnCreate, this, timestamp, sourceMxId);
        emEp.sendEvent(ev);
        mxEp.sendEvent(ev);
    }

    @Override
    public void terminate() {

    }

    @Override
    public void termine(String byUserId, String reason) {
        synchronized (this) {
            if (isClosed) {
                return;
            }

            isClosed = true;
        }

        log.info("Terminating subscription {} | Matrix - ID: {} - Identity: {} | Email - ID: {} - Identity: {}",
                id,
                mxKey,
                mxEp.getIdentity(),
                emKey,
                emEp.getIdentity());

        SubscriptionEvent ev = new SubscriptionEvent(SubscriptionEvents.OnDestroy, this, Instant.now(), byUserId);

        log.info("Closing Matrix endpoint");
        mxEp.sendEvent(ev);
        mxEp.close();

        log.info("Closing Email endpoint");
        emEp.sendEvent(ev);
        emEp.close();

        for (_BridgeSubscriptionListener listener : listeners) {
            listener.onTerminate(this);
        }
    }

    @Override
    public void addListener(_BridgeSubscriptionListener listener) {
        listeners.add(listener);
    }

}
