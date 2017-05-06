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

package io.kamax.matrix.bridge.email.model.email;

import com.sun.mail.smtp.SMTPTransport;
import io.kamax.matrix.bridge.email.config.email.EmailSenderConfig;
import io.kamax.matrix.bridge.email.model.AEndPoint;
import io.kamax.matrix.bridge.email.model.matrix._MatrixBridgeMessage;
import io.kamax.matrix.bridge.email.model.subscription._SubscriptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

public class EmailEndPoint extends AEndPoint<String, _MatrixBridgeMessage, _EmailBridgeMessage> implements _EmailEndPoint {

    private Logger log = LoggerFactory.getLogger(EmailEndPoint.class);

    private EmailSenderConfig cfg;
    private _EmailFormatter formatter;

    private Session session;

    public EmailEndPoint(String id, String email, String emailKey, EmailSenderConfig cfg, _EmailFormatter formatter) {
        super(id, email, emailKey);
        this.cfg = cfg;
        this.formatter = formatter;

        session = Session.getInstance(System.getProperties());
    }

    @Override
    protected void closeImpl() {
        // TODO implement me
        log.warn("Email endpoint close: stub");
    }

    private void send(MimeMessage msg) throws MessagingException {
        msg.setFrom(cfg.getName() + "<" + cfg.getEmail() + ">");
        msg.setHeader("X-Mailer", "matrix-appservice-email");
        msg.setSentDate(new Date());

        SMTPTransport transport = (SMTPTransport) session.getTransport("smtp");
        transport.setStartTLS(cfg.getTls() > 0);
        transport.setRequireStartTLS(cfg.getTls() > 1);
        transport.connect(cfg.getHost(), cfg.getPort(), cfg.getLogin(), cfg.getPassword());

        try {
            transport.sendMessage(msg, InternetAddress.parse(getIdentity()));
        } finally {
            transport.close();
        }
    }

    @Override
    protected void sendMessageImpl(_MatrixBridgeMessage mxMsg) {
        log.info("Email bridge: sending message from {} to {} - start", mxMsg.getSender(), getIdentity());

        try {
            send(formatter.get(mxMsg, this));
            log.info("Email bridge: sending message from {} to {} - success", mxMsg.getSender(), getIdentity());
        } catch (Exception e) {
            log.error("Email bridge: sending message from {} to {} - failure", mxMsg.getSender(), getIdentity());
            throw new RuntimeException(e);
        }

        log.info("Email bridge: sending message from {} to {} - end", mxMsg.getSender(), getIdentity());
    }

    @Override
    protected void sendNotificationImpl(_SubscriptionEvent ev) {
        try {
            Optional<MimeMessage> msg = formatter.get(ev, this);
            if (!msg.isPresent()) {
                log.info("Formatter did not return message, ignoring notification");
                return;
            }

            send(msg.get());
        } catch (IOException | MessagingException e) {
            log.error("Could not send notification to {} about event {}", getIdentity(), ev.getType(), e);
        }
    }

    void inject(_EmailBridgeMessage msg) {
        log.info("Email message was injected into end point {} - {} - {}", getId(), getIdentity(), getChannelId());

        fireMessageEvent(msg);
    }

}
