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

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class GmailClientFormatter implements _EmailClientFormatter {

    @Override
    public String getId() {
        return "gmail";
    }

    @Override
    public boolean matches(Message m) throws MessagingException {
        return false;
    }

    @Override
    public List<Pattern> getContentPatterns(String subType) {
        return Collections.emptyList();
    }

    @Override
    public _BridgeMessageContent format(_BridgeMessageContent content) {
        return null;
    }

}
