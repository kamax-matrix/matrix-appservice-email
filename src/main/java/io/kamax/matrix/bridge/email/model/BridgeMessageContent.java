/*
 * matrix-appservice-email - Matrix Bridge to E-mail
 * Copyright (C) 2017 Kamax Sarl
 *
 * https://www.kamax.io/
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

import org.apache.commons.lang.StringUtils;

import java.nio.charset.StandardCharsets;

public class BridgeMessageContent implements _BridgeMessageContent {

    private String mime;
    private String encoding;
    private byte[] content;

    public BridgeMessageContent(String mime) {
        this(mime, new byte[0]);
    }

    public BridgeMessageContent(String mime, byte[] content) {
        this(mime, null, content);
    }

    public BridgeMessageContent(String mime, String encoding, byte[] content) {
        this.mime = mime;
        this.encoding = StringUtils.defaultIfBlank(encoding, "");
        this.content = content;
    }

    @Override
    public String getMime() {
        return mime;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public String getContentAsString() {
        return new String(getContent(), StandardCharsets.UTF_8);
    }

}
