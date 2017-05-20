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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class ABridgeMessage<T> implements _BridgeMessage<T> {

    private String key;
    private Instant time;
    private T sender;
    private Map<String, _BridgeMessageContent> parts;

    public ABridgeMessage(String key, Instant time, T sender, List<_BridgeMessageContent> partsList) {
        this.key = key;
        this.time = time;
        this.sender = sender;

        Map<String, _BridgeMessageContent> partsMap = new HashMap<>();
        for (_BridgeMessageContent part : partsList) {
            partsMap.put(part.getMime(), part);
        }
        this.parts = partsMap;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public T getSender() {
        return sender;
    }

    @Override
    public Instant getTime() {
        return time;
    }

    @Override
    public Optional<_BridgeMessageContent> getContent(String mime) {
        return Optional.ofNullable(parts.get(mime));
    }

}
