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
import io.kamax.matrix.bridge.email.model._BridgeMessageContent;
import io.kamax.matrix.bridge.email.model.matrix._MatrixBridgeMessage;
import io.kamax.matrix.bridge.email.model.subscription.SubscriptionEvents;
import io.kamax.matrix.bridge.email.model.subscription._SubscriptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeTypeUtils;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class EmailEndPoint extends AEndPoint<String, _MatrixBridgeMessage, _EmailBridgeMessage> implements _EmailEndPoint {

    private Logger log = LoggerFactory.getLogger(EmailEndPoint.class);

    private EmailSenderConfig cfg;
    private _EmailTemplateManager templateMgr;

    private Session session;

    public EmailEndPoint(String id, String email, String emailKey, EmailSenderConfig cfg, _EmailTemplateManager templateMgr) {
        super(id, email, emailKey);
        this.cfg = cfg;
        this.templateMgr = templateMgr;

        session = Session.getInstance(System.getProperties());
    }

    @Override
    protected void closeImpl() {
        // TODO implement me
        log.warn("Email endpoint close: stub");
    }

    private String getHtml(String text) {
        return "<div>" + text + "</div>";
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

    private MimeBodyPart makeEventBodyPart(_EmailTemplate template, String contentBody) throws IOException, MessagingException {
        StringBuilder partRaw = new StringBuilder();

        String header = template.getHeader();
        String footer = template.getFooter();
        String content = template.getContent();

        content = content.replace("%BODY%", contentBody);

        partRaw.append(header).append(content).append(footer);

        MimeBodyPart part = new MimeBodyPart();
        part.setText(partRaw.toString(), StandardCharsets.UTF_8.name(), template.getType());

        log.info("Created body part of type {}", template.getType());

        return part;
    }

    private MimeMessage makeEmail(String subject, MimeMultipart body, boolean allowReply) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        if (allowReply) {
            msg.setReplyTo(InternetAddress.parse(cfg.getTemplate().replace("%KEY%", getChannelId())));
        }
        msg.setSubject("Matrix E-mail bridge - " + subject);
        msg.setContent(body);
        return msg;
    }

    private void sendEmail(List<_EmailTemplate> templates, String title, String txtMsg, String htmlMsg, boolean allowReply) throws IOException, MessagingException {
        MimeMultipart body = new MimeMultipart();
        body.setSubType("alternative");

        for (_EmailTemplate template : templates) {
            if ("plain".contentEquals(template.getType()) && txtMsg != null) {
                log.info("Got plain template, producing part");
                body.addBodyPart(makeEventBodyPart(template, txtMsg));
            }

            if ("html".contentEquals(template.getType()) && htmlMsg != null) {
                log.info("Got html template, producing part");
                body.addBodyPart(makeEventBodyPart(template, htmlMsg));
            }
        }

        send(makeEmail(title, body, allowReply));
    }

    private void sendEmail(List<_EmailTemplate> templates, String title, String msg, boolean allowReply) throws IOException, MessagingException {
        sendEmail(templates, title, msg, getHtml(msg), allowReply);
    }

    private void sendCreateEvent(List<_EmailTemplate> templates) throws IOException, MessagingException {
        sendEmail(templates, "New conversation", "You have been invited to a Matrix conversation", true);
    }

    private void sendMuteEvent(List<_EmailTemplate> templates) throws IOException, MessagingException {
        sendEmail(templates, "Notifications toggle", "Notifications have been muted for your Matrix conversation", false);
    }

    private void sendUnmuteEvent(List<_EmailTemplate> templates) throws MessagingException, IOException {
        sendEmail(templates, "Notifications toggle", "Notifications have been unmuted for your Matrix conversation", false);
    }

    private void sendDestroyEvent(List<_EmailTemplate> templates) throws IOException, MessagingException {
        sendEmail(templates, "Conversation terminated", "You have been removed from your Matrix conversation", false);
    }

    @Override
    protected void sendMessageImpl(_MatrixBridgeMessage mxMsg) {
        log.info("Email bridge: sending message from {} to {} - start", mxMsg.getSender(), getIdentity());

        Optional<_BridgeMessageContent> html = mxMsg.getContent(MimeTypeUtils.TEXT_HTML_VALUE);
        Optional<_BridgeMessageContent> txt = mxMsg.getContent(MimeTypeUtils.TEXT_PLAIN_VALUE);
        if (!html.isPresent() && !txt.isPresent()) {
            log.warn("Ignoring Matrix message {} to {}, no valid content", mxMsg.getKey(), getIdentity());
        }

        List<_EmailTemplate> templates = templateMgr.get(SubscriptionEvents.OnMessage);
        if (templates.isEmpty()) {
            log.error("No template configured for message event {}, skipping");
            return;
        }

        try {
            String txtContent = null;
            String htmlContent = null;
            if (html.isPresent()) {
                log.info("Matrix message contains HTML, including");

                htmlContent = html.get().getContent();
            }

            if (txt.isPresent()) {
                log.info("Matrix message contains Plain text, including");

                txtContent = txt.get().getContent();

                if (!html.isPresent()) {
                    log.info("Matrix message does not contain HTML, creating from text");
                    htmlContent = getHtml(txtContent);
                }
            }

            sendEmail(templates, "New Message", txtContent, htmlContent, true);
            log.info("Email bridge: sending message from {} to {} - success", mxMsg.getSender(), getIdentity());
        } catch (Exception e) {
            log.error("Email bridge: sending message from {} to {} - failure", mxMsg.getSender(), getIdentity());
            throw new RuntimeException(e);
        }

        log.info("Email bridge: sending message from {} to {} - end", mxMsg.getSender(), getIdentity());
    }

    @Override
    protected void sendNotificationImpl(_SubscriptionEvent ev) {
        List<_EmailTemplate> templates = templateMgr.get(ev.getType());
        if (templates.isEmpty()) {
            log.info("No template configured for subscription event {}, skipping");
            return;
        }

        try {
            switch (ev.getType()) {
                case OnCreate:
                    sendCreateEvent(templates);
                    break;
                case OnMute:
                    sendMuteEvent(templates);
                    break;
                case OnUnmute:
                    sendUnmuteEvent(templates);
                    break;
                case OnDestroy:
                    sendDestroyEvent(templates);
                    break;
                default:
                    log.warn("Unknown subscription event type {}", ev.getType().getId());
            }
        } catch (IOException | MessagingException e) {
            log.error("Could not send notification to {} about event {}", getIdentity(), ev.getType(), e);
        }
    }

    void inject(_EmailBridgeMessage msg) {
        log.info("Email message was injected into end point {} - {} - {}", getId(), getIdentity(), getChannelId());

        fireMessageEvent(msg);
    }

}
