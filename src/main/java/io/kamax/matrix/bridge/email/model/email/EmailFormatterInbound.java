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

import io.kamax.matrix.bridge.email.model.BridgeMessageHtmlContent;
import io.kamax.matrix.bridge.email.model.BridgeMessageTextContent;
import io.kamax.matrix.bridge.email.model._BridgeMessageContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class EmailFormatterInbound implements _EmailFormatterInbound {

    private Logger log = LoggerFactory.getLogger(EmailFormatterInbound.class);

    @Autowired
    private List<_EmailClientFormatter> clientFormatters;

    protected List<_BridgeMessageContent> extractContent(Part p) throws MessagingException, IOException {
        if (p.isMimeType("multipart/*")) {
            log.info("Found multipart content, extracting");

            List<_BridgeMessageContent> contents = new ArrayList<>();
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                contents.addAll(extractContent(mp.getBodyPart(i)));
            }
            return contents;
        }

        if (p.isMimeType("message/rfc822")) {
            log.info("Found nested content, extracting");
            return extractContent((Part) p.getContent());
        }

        List<_BridgeMessageContent> contents = new ArrayList<>();
        if (p.isMimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)) {
            log.info("Found plain text content");
            return Collections.singletonList(new BridgeMessageTextContent((String) p.getContent()));
        }

        if (p.isMimeType(MimeTypeUtils.TEXT_HTML_VALUE)) {
            log.info("Found HTML content");
            return Collections.singletonList(new BridgeMessageHtmlContent((String) p.getContent()));
        }

        return Collections.emptyList();
    }

    @Override
    public Optional<_EmailBridgeMessage> get(String key, Message msg) {
        try {
            String sender = ((InternetAddress) msg.getFrom()[0]).getAddress(); // TODO sanitize properly
            log.info("Email is from {}", sender);
            List<_BridgeMessageContent> contents = extractContent(msg);
            if (contents.isEmpty()) {
                log.warn("Found no valid content, skipping");
                return Optional.empty();
            }

            for (_EmailClientFormatter f : clientFormatters) {
                if (f.matches(msg)) {
                    List<_BridgeMessageContent> contentFormatted = new ArrayList<>();
                    for (_BridgeMessageContent content : contents) {
                        contentFormatted.add(f.format(content));
                    }
                    contents = contentFormatted;
                    break;
                }
            }

            return Optional.of(new EmailBridgeMessage(key, sender, contents));
        } catch (IOException | MessagingException e) {
            throw new RuntimeException(e);
        }
    }

}
