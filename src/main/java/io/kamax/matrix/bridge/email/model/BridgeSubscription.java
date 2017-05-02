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

import io.kamax.matrix.client._MatrixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeSubscription implements _BridgeSubscription {

    private Logger log = LoggerFactory.getLogger(BridgeSubscription.class);

    private String roomId;
    private String emailKey;
    private String matrixKey;
    private _MatrixClient mxUser;
    private _EmailClient emUser;

    public BridgeSubscription(String roomId, String emailKey, String matrixKey, _MatrixClient mxUser, _EmailClient emUser) {
        this.roomId = roomId;
        this.matrixKey = matrixKey;
        this.emailKey = emailKey;
        this.mxUser = mxUser;
        this.emUser = emUser;
    }

    @Override
    public String getEmailKey() {
        return emailKey;
    }

    @Override
    public String getMatrixKey() {
        return matrixKey;
    }

    @Override
    public _MatrixClient getMatrixUser() {
        return null;
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public _EmailClient getEmailClient() {
        return null;
    }

    @Override
    public void forward(_EmailBridgeMessage msg) {
        mxUser.getRoom(roomId).send(msg.getContent());
    }

    @Override
    public void forward(_MatrixBridgeMessage msg) {
        emUser.getChannel(emailKey).send(msg);
    }

    @Override
    public void cancelFromMatrix() {
        log.info("Matrix: Canceling subscription for {} with key {}", emUser.getEmail(), matrixKey);

        emUser.getChannel(emailKey).leave();
    }

    @Override
    public void cancelFromEmail() {
        log.info("E-mail: Canceling subscription for {} with key {}", emUser.getEmail(), emailKey);

        mxUser.getRoom(roomId).leave();
    }

}
