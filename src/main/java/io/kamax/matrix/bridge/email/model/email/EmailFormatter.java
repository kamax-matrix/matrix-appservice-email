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

import io.kamax.matrix.bridge.email.config.ServerConfig;
import io.kamax.matrix.bridge.email.config.email.EmailSenderConfig;
import io.kamax.matrix.bridge.email.model._BridgeMessageContent;
import io.kamax.matrix.bridge.email.model.matrix._MatrixBridgeMessage;
import io.kamax.matrix.bridge.email.model.subscription.SubscriptionEvents;
import io.kamax.matrix.bridge.email.model.subscription._SubscriptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
public class EmailFormatter implements InitializingBean, _EmailFormatter {

    private Logger log = LoggerFactory.getLogger(EmailFormatter.class);

    @Autowired
    private ServerConfig srvCfg;

    @Autowired
    private EmailSenderConfig sendCfg;

    @Autowired
    private _EmailTemplateManager templateMgr;

    private Session session = Session.getInstance(System.getProperties());

    @Override
    public void afterPropertiesSet() throws Exception {
        if (templateMgr.get(SubscriptionEvents.OnMessage).isEmpty()) {
            log.error("Configuration error: template list for onMessage notification event cannot be empty");
            System.exit(1);
        }
    }

    private String getSubscriptionManageLink(String token) {
        return srvCfg.getHost() + "/subscription?token=" + token;
    }

    private String getHtml(String text) {
        return "<div>" + text + "</div>";
    }

    private MimeBodyPart makeEventBodyPart(_EmailEndPoint ep, _EmailTemplate template, String contentBody) throws IOException, MessagingException {
        StringBuilder partRaw = new StringBuilder();

        String header = template.getHeader();
        String footer = template.getFooter();
        String content = template.getContent().replace("%MANAGE_URL%", getSubscriptionManageLink(ep.getChannelId()));

        content = content.replace("%BODY%", contentBody);

        partRaw.append(header).append(content).append(footer);

        MimeBodyPart part = new MimeBodyPart();
        part.setText(partRaw.toString(), StandardCharsets.UTF_8.name(), template.getType());

        log.info("Created body part of type {}", template.getType());

        return part;
    }

    private MimeMessage makeEmail(_EmailEndPoint ep, String subject, MimeMultipart body, boolean allowReply) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        if (allowReply) {
            msg.setReplyTo(InternetAddress.parse(sendCfg.getTemplate().replace("%KEY%", ep.getChannelId())));
        }
        msg.setSubject("Matrix E-mail bridge - " + subject);
        msg.setContent(body);
        return msg;
    }

    private MimeMessage makeEmail(_EmailEndPoint ep, List<_EmailTemplate> templates, String title, String txtMsg, String htmlMsg, boolean allowReply) throws IOException, MessagingException {
        MimeMultipart body = new MimeMultipart();
        body.setSubType("alternative");

        for (_EmailTemplate template : templates) {
            if ("plain".contentEquals(template.getType()) && txtMsg != null) {
                log.info("Got plain template, producing part");
                body.addBodyPart(makeEventBodyPart(ep, template, txtMsg));
            }

            if ("html".contentEquals(template.getType()) && htmlMsg != null) {
                log.info("Got html template, producing part");
                body.addBodyPart(makeEventBodyPart(ep, template, htmlMsg));
            }
        }

        return makeEmail(ep, title, body, allowReply);
    }

    private MimeMessage makeEmail(_EmailEndPoint ep, List<_EmailTemplate> templates, String title, String msg, boolean allowReply) throws IOException, MessagingException {
        return makeEmail(ep, templates, title, msg, getHtml(msg), allowReply);
    }

    private MimeMessage makeCreateEvent(_EmailEndPoint ep, List<_EmailTemplate> templates) throws IOException, MessagingException {
        return makeEmail(ep, templates, "New conversation", "You have been invited to a Matrix conversation", true);
    }

    private MimeMessage makeDestroyEvent(_EmailEndPoint ep, List<_EmailTemplate> templates) throws IOException, MessagingException {
        return makeEmail(ep, templates, "Conversation terminated", "You have been removed from your Matrix conversation", false);
    }

    @Override
    public MimeMessage get(_MatrixBridgeMessage msg, _EmailEndPoint ep) throws IOException, MessagingException {
        Optional<_BridgeMessageContent> html = msg.getContent(MimeTypeUtils.TEXT_HTML_VALUE);
        Optional<_BridgeMessageContent> txt = msg.getContent(MimeTypeUtils.TEXT_PLAIN_VALUE);
        if (!html.isPresent() && !txt.isPresent()) {
            log.warn("Ignoring Matrix message {} to {}, no valid content", msg.getKey(), ep.getIdentity());
        }

        List<_EmailTemplate> templates = templateMgr.get(SubscriptionEvents.OnMessage);
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

        return makeEmail(ep, templates, "New Message", txtContent, htmlContent, true);
    }

    @Override
    public Optional<MimeMessage> get(_SubscriptionEvent ev, _EmailEndPoint ep) throws IOException, MessagingException {
        List<_EmailTemplate> templates = templateMgr.get(ev.getType());
        if (templates.isEmpty()) {
            log.info("No template configured for subscription event {}, skipping");
            return Optional.empty();
        }

        switch (ev.getType()) {
            case OnCreate:
                return Optional.of(makeCreateEvent(ep, templates));
            case OnDestroy:
                return Optional.of(makeDestroyEvent(ep, templates));
            default:
                log.warn("Unknown subscription event type {}", ev.getType().getId());
                return Optional.empty();
        }
    }

}
