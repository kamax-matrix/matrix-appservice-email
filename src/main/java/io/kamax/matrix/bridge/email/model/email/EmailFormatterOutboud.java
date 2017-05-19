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
import io.kamax.matrix.bridge.email.model.BridgeMessageHtmlContent;
import io.kamax.matrix.bridge.email.model.BridgeMessageTextContent;
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
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class EmailFormatterOutboud implements InitializingBean, _EmailFormatterOutbound {

    private Logger log = LoggerFactory.getLogger(EmailFormatterOutboud.class);

    @Autowired
    private ServerConfig srvCfg;

    @Autowired
    private EmailSenderConfig sendCfg;

    @Autowired
    private _EmailTemplateManager templateMgr;

    private Session session = Session.getInstance(System.getProperties());

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!templateMgr.get(SubscriptionEvents.OnMessage).isPresent()) {
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

    private String processToken(TokenData data, String template) {
        return template
                .replace(EmailTemplateToken.ManageUrl.getToken(), data.getManageUrl());
    }

    private String processToken(TokenData data, String template, String content) {
        return processToken(data, template)
                .replace(EmailTemplateToken.MsgContent.getToken(), content);
    }

    private MimeBodyPart makeBodyPart(TokenData data, _EmailTemplateContent template, _BridgeMessageContent content) throws IOException, MessagingException {
        StringBuilder partRaw = new StringBuilder();

        String header = processToken(data, template.getHeader());
        String footer = processToken(data, template.getFooter());
        String contentString = processToken(data, template.getContent(), content.getContentAsString());

        partRaw.append(header).append(contentString).append(footer);

        MimeBodyPart part = new MimeBodyPart();
        part.setText(partRaw.toString(), StandardCharsets.UTF_8.name(), template.getType().replace("text/", ""));

        log.info("Created body part of type {}", template.getType());

        return part;
    }

    private MimeMessage makeEmail(TokenData data, _EmailTemplate template, MimeMultipart body, boolean allowReply) throws MessagingException, UnsupportedEncodingException {
        MimeMessage msg = new MimeMessage(session);
        if (allowReply) {
            msg.setReplyTo(InternetAddress.parse(sendCfg.getTemplate().replace("%KEY%", data.getKey())));
        }

        msg.setSender(new InternetAddress(sendCfg.getEmail()));
        msg.setSubject(processToken(data, template.getSubject()));
        msg.setContent(body);
        return msg;
    }

    private MimeMessage makeEmail(TokenData data, _EmailTemplate template, List<_BridgeMessageContent> contents, boolean allowReply) throws MessagingException, IOException {
        MimeMultipart body = new MimeMultipart();
        body.setSubType("alternative");

        for (_BridgeMessageContent content : contents) {
            Optional<_EmailTemplateContent> contentTemplateOpt = template.getContent(content.getMime());
            if (!contentTemplateOpt.isPresent()) {
                continue;
            }

            body.addBodyPart(makeBodyPart(data, contentTemplateOpt.get(), content));
        }

        return makeEmail(data, template, body, allowReply);
    }

    private MimeMessage makeEmail(TokenData data, _EmailTemplate template, boolean allowReply) throws IOException, MessagingException {
        List<_BridgeMessageContent> contents = Arrays.asList(
                new BridgeMessageTextContent(MimeTypeUtils.TEXT_PLAIN_VALUE),
                new BridgeMessageTextContent(MimeTypeUtils.TEXT_HTML_VALUE)
        );

        return makeEmail(data, template, contents, allowReply);
    }

    @Override
    public Optional<MimeMessage> get(_MatrixBridgeMessage msg, _EmailEndPoint ep) throws IOException, MessagingException {
        Optional<_EmailTemplate> templateOpt = templateMgr.get(SubscriptionEvents.OnMessage);
        if (!templateOpt.isPresent()) {
            log.info("Ignoring message event {} to {}, no notification set", msg.getKey(), ep.getIdentity());
            return Optional.empty();
        }

        _EmailTemplate template = templateOpt.get();
        List<_EmailTemplateContent> templates = template.listContents();
        if (templates.isEmpty()) {
            log.info("No template configured for subscription event {}, skipping");
            return Optional.empty();
        }

        Optional<_BridgeMessageContent> txtOpt = msg.getContent(MimeTypeUtils.TEXT_PLAIN_VALUE);
        Optional<_BridgeMessageContent> htmlOpt = msg.getContent(MimeTypeUtils.TEXT_HTML_VALUE);

        List<_BridgeMessageContent> contents = new ArrayList<>();
        if (!txtOpt.isPresent()) {
            if (!htmlOpt.isPresent()) {
                log.warn("Ignoring Matrix message {} to {}, no valid content", msg.getKey(), ep.getIdentity());
                return Optional.empty();
            }

            contents.add(htmlOpt.get());
        } else {
            contents.add(txtOpt.get());
            if (htmlOpt.isPresent()) {
                contents.add(htmlOpt.get());
            } else {
                contents.add(new BridgeMessageHtmlContent(getHtml(txtOpt.get().getContentAsString())));
            }
        }

        TokenData tokenData = new TokenData(ep.getChannelId());
        tokenData.setManageUrl(getSubscriptionManageLink(ep.getChannelId()));

        return Optional.of(makeEmail(tokenData, template, contents, true));
    }

    @Override
    public Optional<MimeMessage> get(_SubscriptionEvent ev) throws IOException, MessagingException {
        Optional<_EmailTemplate> templateOpt = templateMgr.get(ev.getType());
        if (!templateOpt.isPresent()) {
            log.info("Ignoring subscription event {} to {}, no notification set", ev.getType(), ev.getSubscription().getEmailEndpoint().getIdentity());
            return Optional.empty();
        }

        _EmailTemplate template = templateOpt.get();
        List<_EmailTemplateContent> templates = template.listContents();
        if (templates.isEmpty()) {
            log.info("No template configured for subscription event {}, skipping");
            return Optional.empty();
        }

        TokenData tokenData = new TokenData(ev.getSubscription().getEmailEndpoint().getChannelId());
        tokenData.setManageUrl(getSubscriptionManageLink(ev.getSubscription().getEmailEndpoint().getChannelId()));

        switch (ev.getType()) {
            case OnCreate:
                return Optional.of(makeEmail(tokenData, template, true));
            default:
                log.warn("Unknown subscription event type {}", ev.getType().getId());
                return Optional.of(makeEmail(tokenData, template, false));
        }
    }

    private class TokenData {

        private String key;
        private String msgTimeHour;
        private String msgTimeMin;
        private String msgTimeSec;
        private String senderName;
        private String senderAddress;
        private String senderAvatar;
        private String receiverAddress;
        private String room;
        private String roomName;
        private String roomAddress;
        private String manageUrl;

        TokenData(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public String getMsgTimeHour() {
            return msgTimeHour;
        }

        public void setMsgTimeHour(String msgTimeHour) {
            this.msgTimeHour = msgTimeHour;
        }

        public String getMsgTimeMin() {
            return msgTimeMin;
        }

        public void setMsgTimeMin(String msgTimeMin) {
            this.msgTimeMin = msgTimeMin;
        }

        public String getMsgTimeSec() {
            return msgTimeSec;
        }

        public void setMsgTimeSec(String msgTimeSec) {
            this.msgTimeSec = msgTimeSec;
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }

        public String getSenderAddress() {
            return senderAddress;
        }

        public void setSenderAddress(String senderAddress) {
            this.senderAddress = senderAddress;
        }

        public String getSenderAvatar() {
            return senderAvatar;
        }

        public void setSenderAvatar(String senderAvatar) {
            this.senderAvatar = senderAvatar;
        }

        public String getReceiverAddress() {
            return receiverAddress;
        }

        public void setReceiverAddress(String receiverAddress) {
            this.receiverAddress = receiverAddress;
        }

        public String getRoom() {
            return room;
        }

        public void setRoom(String room) {
            this.room = room;
        }

        public String getRoomName() {
            return roomName;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public String getRoomAddress() {
            return roomAddress;
        }

        public void setRoomAddress(String roomAddress) {
            this.roomAddress = roomAddress;
        }

        public String getManageUrl() {
            return manageUrl;
        }

        public void setManageUrl(String manageUrl) {
            this.manageUrl = manageUrl;
        }

    }
}
