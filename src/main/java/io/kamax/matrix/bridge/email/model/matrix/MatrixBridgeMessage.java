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

package io.kamax.matrix.bridge.email.model.matrix;

import io.kamax.matrix._MatrixUser;
import io.kamax.matrix.bridge.email.model.ABridgeMessage;
import io.kamax.matrix.bridge.email.model.BridgeMessageTextContent;

import java.time.Instant;
import java.util.Collections;

public class MatrixBridgeMessage extends ABridgeMessage<_MatrixUser> implements _MatrixBridgeMessage {

    public MatrixBridgeMessage(String key, Instant time, _MatrixUser sender, String txtContent) {
        super(key, time, sender, Collections.singletonList(new BridgeMessageTextContent(txtContent)));
    }

    public MatrixBridgeMessage(String key, Instant time, _MatrixUser sender) {
        super(key, time, sender, Collections.emptyList());
    }

}