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

import com.sun.mail.smtp.SMTPTransport;
import io.kamax.matrix.bridge.email.config.email.EmailSenderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeTypeUtils;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

public class EmailClient implements _EmailClient {

    private Logger log = LoggerFactory.getLogger(EmailClient.class);

    private String email;
    private EmailSenderConfig cfg;

    public EmailClient(String email, EmailSenderConfig cfg) {
        this.email = email;
        this.cfg = cfg;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public _EmailEndPoint getEndpoint(String channelId) {
        return new EmailEndPoint(channelId);
    }

    private class EmailEndPoint implements _EmailEndPoint {

        private String emailKey;

        EmailEndPoint(String emailKey) {
            this.emailKey = emailKey;
        }

        @Override
        public String getId() {
            return emailKey;
        }

        @Override
        public String getIdentity() {
            return email;
        }

        @Override
        public void open() {
            // TODO implement me
            log.warn("Email endpoint open: stub");
        }

        @Override
        public void close() {
            // TODO implement me
            log.warn("Email endpoint close: stub");
        }

        @Override
        public void sendMessage(_MatrixBridgeMessage mxMsg) {
            log.info("Email bridge: sending message from {} to {} - start", mxMsg.getSender(), email);

            Optional<_BridgeMessageContent> html = mxMsg.getContent(MimeTypeUtils.TEXT_HTML_VALUE);
            Optional<_BridgeMessageContent> txt = mxMsg.getContent(MimeTypeUtils.TEXT_PLAIN_VALUE);
            if (!html.isPresent() && !txt.isPresent()) {
                log.warn("Ignoring Matrix message {} to {}, no valid content", mxMsg.getKey(), email);
            }

            try {
                MimeMultipart body = new MimeMultipart();
                if (html.isPresent()) {
                    log.info("Matrix message contains HTML, including");

                    _BridgeMessageContent htmlContent = html.get();
                    MimeBodyPart part = new MimeBodyPart();
                    part.setText(htmlContent.getContent(), StandardCharsets.UTF_8.name(), "html");
                    body.addBodyPart(part);
                }

                if (txt.isPresent()) {
                    log.info("Matrix message contains Plain text, including");

                    _BridgeMessageContent txtContent = txt.get();
                    MimeBodyPart part = new MimeBodyPart();
                    part.setText(txtContent.getContent(), StandardCharsets.UTF_8.name(), "plain");
                    body.addBodyPart(part);
                }


                Session session = Session.getInstance(System.getProperties());
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(cfg.getEmail());
                msg.setReplyTo(InternetAddress.parse(cfg.getTemplate().replace("%KEY%", emailKey)));
                msg.setSubject("Matrix E-mail bridge - New message");
                msg.setSentDate(new Date());
                msg.setHeader("X-Mailer", "matrix-appservice-email");
                msg.setContent(body);
                SMTPTransport transport = (SMTPTransport) session.getTransport("smtp");
                transport.setStartTLS(cfg.getTls() > 0);
                transport.setRequireStartTLS(cfg.getTls() > 1);
                transport.connect(cfg.getHost(), cfg.getPort(), cfg.getLogin(), cfg.getPassword());

                try {
                    transport.sendMessage(msg, InternetAddress.parse(email));
                    log.info("Email bridge: sending message from {} to {} - success", mxMsg.getSender(), email);
                } finally {
                    transport.close();
                }
            } catch (Exception e) {
                log.error("Email bridge: sending message from {} to {} - failure", mxMsg.getSender(), email);
                throw new RuntimeException(e);
            }
            log.info("Email bridge: sending message from {} to {} - end", mxMsg.getSender(), email);
        }

        @Override
        public void sendNotification(_BridgeEvent ev) {
            // TODO implement me
            log.warn("Email endpoint send bridge event: stub");
        }

    }

}
