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
import io.kamax.matrix.bridge.email.model.subscription._BridgeSubscription;
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
    private _EmailFormatterOutbound formatter;

    private Session session;

    public EmailEndPoint(String id, String email, String emailKey, EmailSenderConfig cfg, _EmailFormatterOutbound formatter) {
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
        msg.setHeader("X-Mailer", "matrix-appservice-email");
        msg.setSentDate(new Date());

        SMTPTransport transport = (SMTPTransport) session.getTransport("smtp");
        transport.setStartTLS(cfg.getTls() > 0);
        transport.setRequireStartTLS(cfg.getTls() > 1);
        transport.connect(cfg.getHost(), cfg.getPort(), cfg.getLogin(), cfg.getPassword());
        log.info("Sending email via SMTP using {}:{}", cfg.getHost(), cfg.getPort());

        try {
            transport.sendMessage(msg, InternetAddress.parse(getIdentity()));
        } catch (MessagingException e) {
            log.error("mmm", e);
        } finally {
            transport.close();
        }
    }

    @Override
    protected void sendEventImpl(_SubscriptionEvent ev) {
        try {
            Optional<MimeMessage> msg = formatter.get(ev);
            if (!msg.isPresent()) {
                log.info("Formatter did not return message, ignoring notification");
                return;
            }

            send(msg.get());
            log.info("Email bridge: sending event {} to {} - success", ev.getType(), getIdentity());
        } catch (IOException | MessagingException e) {
            log.error("Could not send notification to {} about event {}", getIdentity(), ev.getType(), e);
        }
    }

    @Override
    protected void sendMessageImpl(_BridgeSubscription sub, _MatrixBridgeMessage msg) {
        log.info("Email bridge: sending message from {} to {} - start", msg.getSender(), getIdentity());

        try {
            Optional<MimeMessage> mimeMsg = formatter.get(sub, msg);
            if (!mimeMsg.isPresent()) {
                log.info("Email bridge: formatter did not return any content for matrix message, ignoring");
            } else {
                send(mimeMsg.get());
                log.info("Email bridge: sending message from {} to {} - success", msg.getSender(), getIdentity());
            }
        } catch (Exception e) {
            log.error("Email bridge: sending message from {} to {} - failure", msg.getSender(), getIdentity());
            throw new RuntimeException(e);
        }

        log.info("Email bridge: sending message from {} to {} - end", msg.getSender(), getIdentity());
    }

    void inject(_EmailBridgeMessage msg) {
        log.info("Email message was injected into end point {} - {} - {}", getId(), getIdentity(), getChannelId());

        fireMessageEvent(msg);
    }

}
