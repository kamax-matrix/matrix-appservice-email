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

package io.kamax.matrix.bridge.email.model.matrix;

import io.kamax.matrix._MatrixID;
import io.kamax.matrix.bridge.email.config.subscription.MatrixNotificationConfig;
import io.kamax.matrix.bridge.email.model.AEndPoint;
import io.kamax.matrix.bridge.email.model._BridgeMessageContent;
import io.kamax.matrix.bridge.email.model.email._EmailBridgeMessage;
import io.kamax.matrix.bridge.email.model.subscription._SubscriptionEvent;
import io.kamax.matrix.client._MatrixClient;
import io.kamax.matrix.hs._MatrixRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeTypeUtils;

import java.util.Optional;

public class MatrixEndPoint extends AEndPoint<_MatrixID, _EmailBridgeMessage, _MatrixBridgeMessage> implements _MatrixEndPoint {

    private Logger log = LoggerFactory.getLogger(MatrixEndPoint.class);

    private _MatrixClient client;
    private MatrixNotificationConfig notifCfg;

    public MatrixEndPoint(String id, _MatrixClient client, String roomId, MatrixNotificationConfig notifCfg) {
        super(id, client.getUserId(), roomId);
        this.client = client;
        this.notifCfg = notifCfg;
    }

    @Override
    protected void closeImpl() {
        client.getRoom(getChannelId()).leave();
    }

    protected void sendMessageImpl(_EmailBridgeMessage msg) {
        Optional<_BridgeMessageContent> html = msg.getContent(MimeTypeUtils.TEXT_HTML_VALUE);
        Optional<_BridgeMessageContent> txt = msg.getContent(MimeTypeUtils.TEXT_PLAIN_VALUE);
        if (!html.isPresent() && !txt.isPresent()) {
            log.warn("Ignoring E-mail message {} to {}, no valid content", msg.getKey(), msg.getSender());
        }

        if (html.isPresent() && txt.isPresent()) {
            log.info("Forwarding e-mail {} to Matrix from {} with formatted content", msg.getKey(), msg.getSender());
            client.getRoom(getChannelId()).sendFormattedText(html.get().getContent(), txt.get().getContent());
        } else {
            log.info("Forwarding e-mail {} to Matrix from {} with plain content", msg.getKey(), msg.getSender());
            client.getRoom(getChannelId()).sendText(txt.get().getContent());
        }
    }

    @Override
    protected void sendNotificationImpl(_SubscriptionEvent ev) {
        if (!notifCfg.get(ev.getType())) {
            log.info("Subscription event {} has Matrix notification disabled, ignoring", ev.getType());
            return;
        }

        _MatrixRoom room = client.getRoom(getChannelId());
        switch (ev.getType()) {
            case OnDestroy:
                room.sendNotice("This user unsubscribed from this room.");
                room.leave();
                break;
            default:
                log.warn("Unknown subscription event type {}", ev.getType().getId());
        }
    }

    void inject(_MatrixBridgeMessage msg) {
        log.info("Matrix message was injected into end point {} - {} - {}", getId(), getIdentity(), getChannelId());

        fireMessageEvent(msg);
    }

}
