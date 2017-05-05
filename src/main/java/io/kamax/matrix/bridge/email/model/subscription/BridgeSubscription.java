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

import io.kamax.matrix.bridge.email.model.email._EmailEndPoint;
import io.kamax.matrix.bridge.email.model.matrix._MatrixEndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BridgeSubscription implements _BridgeSubscription {

    private Logger log = LoggerFactory.getLogger(BridgeSubscription.class);

    private String id;
    private String emKey;
    private String mxKey;
    private _MatrixEndPoint mxEp;
    private _EmailEndPoint emEp;

    private List<_BridgeSubscriptionListener> listeners = new ArrayList<>();

    public BridgeSubscription(String id, String emKey, _EmailEndPoint emEp, String mxKey, _MatrixEndPoint mxEp) {
        this.id = id;
        this.emKey = emKey;
        this.mxKey = mxKey;
        this.mxEp = mxEp;
        this.emEp = emEp;

        mxEp.addListener(emEp::sendMessage);
        emEp.addListener(mxEp::sendMessage);
    }

    @Override
    public String getId() {
        return id;
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
    public void terminate() {
        log.info("Canceling subscription {} | Matrix - ID: {} - Identity: {} | Email - ID: {} - Identity: {}",
                id,
                mxKey,
                mxEp.getIdentity(),
                emKey,
                emEp.getIdentity());

        log.info("Closing Matrix endpoint");
        mxEp.close();

        log.info("Closing Email endpoint");
        emEp.close();

        for (_BridgeSubscriptionListener listener : listeners) {
            listener.terminated(this);
        }
    }

    @Override
    public void addListener(_BridgeSubscriptionListener listener) {
        listeners.add(listener);
    }

}
