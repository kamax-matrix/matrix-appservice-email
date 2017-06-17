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
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.List;

@Component
public class ThunderbirdClientFormatter extends AEmailClientFormatter {

    @Override
    public String getId() {
        return "thunderbird";
    }

    @Override
    public boolean matches(Message m, List<_BridgeMessageContent> contents) throws MessagingException {
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
    protected String formatHtml(String content) {
        Element body = Jsoup.parse(content).body();
        body.select("blockquote[cite]").remove();
        body.select("div.moz-cite-prefix").remove();

        while (body.children().size() > 0 && body.children().last().is("br")) {
            body.children().last().remove();
        }

        return Jsoup.clean(body.html(), Whitelist.basic());
    }

}
