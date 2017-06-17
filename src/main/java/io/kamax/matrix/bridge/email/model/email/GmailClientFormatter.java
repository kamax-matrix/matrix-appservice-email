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

import io.kamax.matrix.bridge.email.model._BridgeMessageContent;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GmailClientFormatter extends AEmailClientFormatter {

    private Logger log = LoggerFactory.getLogger(GmailClientFormatter.class);

    private Pattern pattern = Pattern.compile("<div class=(3D)?\"gmail_extra\">");

    @Override
    public String getId() {
        return "gmail";
    }

    @Override
    public boolean matches(Message m, List<_BridgeMessageContent> contents) throws MessagingException {
        for (_BridgeMessageContent content : contents) {
            Matcher matcher = pattern.matcher(content.getContentAsString());
            if (matcher.find()) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected String formatHtml(String content) {
        try {
            // TODO let's not assume the encoding
            content = new String(QuotedPrintableCodec.decodeQuotedPrintable(content.getBytes()), StandardCharsets.UTF_8);
        } catch (DecoderException e) {
            log.warn("E-mail from Gmail was not quoted-printable codec, using raw HTML instead");
        }

        Element body = Jsoup.parse(content).body();
        Element contentDiv = body.select("div[dir='ltr']").first();
        if (contentDiv == null) {
            log.warn("Found no valid content in e-mail from Gmail, returning empty");
            return "";
        }

        while (contentDiv.children().size() > 0 && contentDiv.children().last().is("br")) {
            contentDiv.children().last().remove();
        }

        return Jsoup.clean(contentDiv.html(), Whitelist.basic());
    }

}
