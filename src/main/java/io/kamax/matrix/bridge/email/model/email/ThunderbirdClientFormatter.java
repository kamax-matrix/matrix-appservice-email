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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ThunderbirdClientFormatter implements _EmailClientFormatter {

    @Override
    public String getId() {
        return "thunderbird";
    }

    @Override
    public boolean matches(Message m) throws MessagingException {
        String[] headers = m.getHeader("User-Agent");
        if (headers != null) {
            for (String header : headers) {
                if (StringUtils.containsIgnoreCase(header, getId())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<Pattern> getContentPatterns(String subType) {
        return Collections.emptyList();
    }

    String formatPlain(String content) {
        // TODO do a proper algorithm

        try {
            int maxLine = 0;

            List<String> linesIn = IOUtils.readLines(new StringReader(content));
            for (int i = 0; i < linesIn.size(); i++) {
                if (linesIn.get(i).startsWith(">")) {
                    maxLine = i - 1;
                    break;
                }
            }

            List<String> linesOut = new ArrayList<>();
            boolean prevLineBlank = false;
            for (int i = 0; i < maxLine; i++) {
                String line = StringUtils.trimToEmpty(linesIn.get(i));
                if (StringUtils.isBlank(line)) {
                    if (prevLineBlank) {
                        continue;
                    }

                    prevLineBlank = true;
                } else {
                    prevLineBlank = false;
                }
                linesOut.add(line);
            }

            if (prevLineBlank) {
                linesOut.remove(linesOut.size() - 1);
            }

            return StringUtils.join(linesOut, System.lineSeparator());
        } catch (IOException e) {
            // This should never happen, we can't deal with it here
            throw new RuntimeException(e);
        }
    }

    String formatHtml(String content) {
        Element body = Jsoup.parse(content).body();
        body.select("blockquote[cite]").remove();
        body.select("div.moz-cite-prefix").remove();

        while (body.children().size() > 0 && body.children().last().is("br")) {
            body.children().last().remove();
        }

        return Jsoup.clean(body.html(), Whitelist.basic());
    }

    @Override
    public _BridgeMessageContent format(_BridgeMessageContent content) {
        if ("text/plain".contentEquals(content.getMime())) {
            return new BridgeMessageTextContent(formatPlain(content.getContent()));
        } else if ("text/html".contentEquals(content.getMime())) {
            return new BridgeMessageHtmlContent(formatHtml(content.getContent()));
        } else {
            throw new IllegalArgumentException(content.getMime() + " is not supported");
        }
    }

}
