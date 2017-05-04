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
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.Optional;

@Component
@Scope("prototype")
@Lazy
public class BridgeSubscription implements _BridgeSubscription {

    private Logger log = LoggerFactory.getLogger(BridgeSubscription.class);

    private String roomId;
    private String matrixKey;
    private _MatrixClient mxUser;
    private _EmailEndPoint emEp;

    public BridgeSubscription(String roomId, String matrixKey, _MatrixClient mxUser, _EmailEndPoint emEp) {
        this.roomId = roomId;
        this.matrixKey = matrixKey;
        this.mxUser = mxUser;
        this.emEp = emEp;
    }

    @Override
    public String getEmailKey() {
        return emEp.getId();
    }

    @Override
    public String getMatrixKey() {
        return matrixKey;
    }

    @Override
    public _MatrixClient getMatrixUser() {
        return mxUser;
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public _EmailEndPoint getEmailEndpoint() {
        return emEp;
    }

    @Override
    public void forward(_EmailBridgeMessage msg) {
        // TODO move into Matrix end point
        Optional<_BridgeMessageContent> html = msg.getContent(MimeTypeUtils.TEXT_HTML_VALUE);
        Optional<_BridgeMessageContent> txt = msg.getContent(MimeTypeUtils.TEXT_PLAIN_VALUE);
        if (!html.isPresent() && !txt.isPresent()) {
            log.warn("Ignoring E-mail message {} to {}, no valid content", msg.getKey(), emEp.getIdentity());
        }

        if (html.isPresent() && txt.isPresent()) {
            log.info("Forwarding formatted e-mail message {} to Matrix for {}", msg.getKey(), emEp.getIdentity());
            mxUser.getRoom(roomId).sendFormattedText(html.get().getContent(), txt.get().getContent());
        } else {
            log.info("Forwarding plain e-mail message {} to Matrix for {}", msg.getKey(), emEp.getIdentity());
            mxUser.getRoom(roomId).sendText(txt.get().getContent());
        }
    }

    @Override
    public void forward(_MatrixBridgeMessage msg) {
        emEp.sendMessage(msg);
    }

    @Override
    public void cancelFromMatrix() {
        log.info("Matrix: Canceling subscription for {} with key {}", emEp.getIdentity(), matrixKey);

        emEp.close();
    }

    @Override
    public void cancelFromEmail() {
        log.info("E-mail: Canceling subscription for {} with key {}", emEp.getIdentity(), emEp.getId());

        mxUser.getRoom(roomId).leave();
    }

}
