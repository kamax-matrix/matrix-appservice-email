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

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubscriptionManager implements _SubscriptionManager {

    private Map<String, _BridgeSubscription> subsUuid = new HashMap<>();
    private Map<SubscriptionId, _BridgeSubscription> subsIds = new HashMap<>();

    @Override
    public _BridgeSubscription getOrCreate(String roomId, _MatrixBridgeUser user) {
        SubscriptionId subId = new SubscriptionId(user.getEmail(), roomId, user.getClient().getHomeserver().getDomain());

        _BridgeSubscription sub = subsIds.get(subId);
        if (sub != null) {
            return sub;
        }

        String uuid;
        do {
            uuid = UUID.randomUUID().toString();
        } while (subsUuid.containsKey(uuid));

        sub = new BridgeSubscription(uuid, roomId, user);
        subsIds.put(subId, sub);
        subsUuid.put(uuid, sub);

        return sub;
    }

    @Override
    public _BridgeSubscription get(String id) {
        return subsUuid.get(id);
    }

    @Override
    public Optional<_BridgeSubscription> remove(String id) {
        return null;
    }

    @Override
    public Optional<_BridgeSubscription> remove(String email, String roomId, String hsDomain) {
        return null;
    }

    private class SubscriptionId {

        private String email;
        private String roomId;
        private String hs;

        SubscriptionId(String email, String roomId, String hs) {
            this.email = email;
            this.roomId = roomId;
            this.hs = hs;
        }

        public String getEmail() {
            return email;
        }

        public String getRoomId() {
            return roomId;
        }

        public String getHs() {
            return hs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SubscriptionId that = (SubscriptionId) o;

            if (!email.equals(that.email)) return false;
            if (!roomId.equals(that.roomId)) return false;
            return hs.equals(that.hs);
        }

        @Override
        public int hashCode() {
            int result = email.hashCode();
            result = 31 * result + roomId.hashCode();
            result = 31 * result + hs.hashCode();
            return result;
        }
    }

}
