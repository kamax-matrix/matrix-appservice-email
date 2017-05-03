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

import io.kamax.matrix.client._MatrixClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubscriptionManager implements _SubscriptionManager {

    private Map<String, _BridgeSubscription> subsEmailKey = new HashMap<>();
    private Map<String, _BridgeSubscription> subsMatrixKey = new HashMap<>();

    private String getMatrixKey(String hsDomain, String roomId, String email) {
        return hsDomain + "|" + roomId + "|" + email;
    }

    private String getMatrixKey(_MatrixClient mxUser, _EmailClient emUser, String mxRoomId) {
        return getMatrixKey(mxUser.getHomeserver().getDomain(), mxRoomId, emUser.getEmail());
    }

    private Optional<_BridgeSubscription> findById(String email, String roomId, String hsDomain) {
        return Optional.ofNullable(subsMatrixKey.get(getMatrixKey(hsDomain, roomId, email)));
    }

    @Override
    public _BridgeSubscription getOrCreate(String roomId, _MatrixClient mxUser, _EmailClient emUser) {
        return findById(emUser.getEmail(), roomId, mxUser.getHomeserver().getDomain()).orElseGet(() -> {
            String emailKey;
            do {
                emailKey = UUID.randomUUID().toString().replace("-", "");
            } while (subsEmailKey.containsKey(emailKey));

            String matrixKey = getMatrixKey(mxUser, emUser, roomId);
            _BridgeSubscription sub = new BridgeSubscription(roomId, emailKey, matrixKey, mxUser, emUser);
            subsEmailKey.put(emailKey, sub);
            subsMatrixKey.put(matrixKey, sub);

            return sub;
        });
    }

    @Override
    public Optional<_BridgeSubscription> get(String roomId, _MatrixBridgeUser mxUser) {
        return findById(mxUser.getEmail(), roomId, mxUser.getClient().getHomeserver().getDomain());
    }

    @Override
    public Optional<_BridgeSubscription> get(String emailKey) {
        return Optional.ofNullable(subsEmailKey.get(emailKey));
    }

    @Override
    public Optional<_BridgeSubscription> remove(String emailKey) {
        _BridgeSubscription sub = subsEmailKey.remove(emailKey);
        if (sub == null) {
            return Optional.empty();
        }

        subsMatrixKey.remove(sub.getMatrixKey());
        return Optional.of(sub);
    }

    @Override
    public Optional<_BridgeSubscription> remove(String roomId, _MatrixClient mxUser, _EmailClient emClient) {
        _BridgeSubscription sub = subsMatrixKey.remove(getMatrixKey(mxUser, emClient, roomId));
        if (sub == null) {
            return Optional.empty();
        }

        subsEmailKey.remove(sub.getEmailKey());
        return Optional.of(sub);
    }

}
