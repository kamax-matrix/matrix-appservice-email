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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SubscriptionManager implements _SubscriptionManager {

    @Autowired
    private ApplicationContext app;

    private Map<String, _BridgeSubscription> subsEmailKey = new HashMap<>();
    private Map<String, _BridgeSubscription> subsMatrixKey = new HashMap<>();

    private List<_SubscriptionListener> listeners = new ArrayList<>();

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
            _BridgeSubscription sub = app.getBean(BridgeSubscription.class, roomId, matrixKey, mxUser, emUser.getEndpoint(emailKey));
            subsEmailKey.put(emailKey, sub);
            subsMatrixKey.put(matrixKey, sub);

            for (_SubscriptionListener listener : listeners) {
                listener.created(sub);
            }

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

    private _BridgeSubscription remove(_BridgeSubscription sub) {
        if (sub == null) {
            return null;
        }

        subsEmailKey.remove(sub.getEmailKey());
        subsMatrixKey.remove(sub.getMatrixKey());

        for (_SubscriptionListener listener : listeners) {
            listener.destroyed(sub);
        }

        return sub;
    }

    @Override
    public Optional<_BridgeSubscription> remove(String emailKey) {
        return Optional.ofNullable(remove(subsEmailKey.get(emailKey)));
    }

    @Override
    public Optional<_BridgeSubscription> remove(String roomId, _MatrixClient mxUser, _EmailClient emClient) {
        return Optional.ofNullable(remove(subsMatrixKey.get(getMatrixKey(mxUser, emClient, roomId))));
    }

    @Override
    public void addListener(_SubscriptionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

}
