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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public abstract class AEmailClientFormatter implements _EmailClientFormatter {

    protected String formatPlain(String content) {
        // TODO do a proper algorithm

        try {
            int maxLine = 0;


            List<String> linesIn = IOUtils.readLines(new StringReader(content));
            for (int i = 0; i < linesIn.size(); i++) {
                if (linesIn.get(i).startsWith(">")) {
                    maxLine = i - (StringUtils.isBlank(linesIn.get(i - 1)) ? 2 : 1);
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

    protected abstract String formatHtml(String content);

    @Override
    public _BridgeMessageContent format(_BridgeMessageContent content) {
        if ("text/plain".contentEquals(content.getMime())) {
            return new BridgeMessageTextContent(formatPlain(content.getContentAsString()));
        } else if ("text/html".contentEquals(content.getMime())) {
            return new BridgeMessageHtmlContent(formatHtml(content.getContentAsString()));
        } else {
            throw new IllegalArgumentException(content.getMime() + " is not supported");
        }
    }

}
