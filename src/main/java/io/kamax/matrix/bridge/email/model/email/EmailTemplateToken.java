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

public enum EmailTemplateToken {

    MsgContent("%MSG_CONTENT%"),
    MsgTimeHour("%MSG_TIME_HOUR%"),
    MsgTimeMin("%MSG_TIME_MIN%"),
    MsgTimeSec("%MSG_TIME_SEC%"),
    Sender("%SENDER%"),
    SenderName("%SENDER_NAME%"),
    SenderAddress("%SENDER_ADDRESS%"),
    SenderAvatar("%SENDER_AVATAR%"),
    ReceiverAddress("%RECEIVER_ADDRESS%"),
    Room("%ROOM%"),
    RoomName("%ROOM_NAME%"),
    RoomAddress("%ROOM_ADDRESS%"),

    ManageUrl("%MANAGE_URL%");

    private String token;

    EmailTemplateToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

}
