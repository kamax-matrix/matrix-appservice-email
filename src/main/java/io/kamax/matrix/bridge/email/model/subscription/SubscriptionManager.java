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

package io.kamax.matrix.bridge.email.model.subscription;

import io.kamax.matrix.bridge.email.model.email._EmailEndPoint;
import io.kamax.matrix.bridge.email.model.email._EmailManager;
import io.kamax.matrix.bridge.email.model.matrix._MatrixEndPoint;
import io.kamax.matrix.bridge.email.model.matrix._MatrixManager;
import io.kamax.matrix.client._MatrixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubscriptionManager implements _SubscriptionManager {

    private Logger log = LoggerFactory.getLogger(SubscriptionManager.class);

    @Autowired
    private _EmailManager emMgr;

    @Autowired
    private _MatrixManager mxMgr;

    private Map<String, _BridgeSubscription> subs = new HashMap<>();
    private Map<String, WeakReference<_BridgeSubscription>> subsEmailKey = new HashMap<>();
    private Map<String, WeakReference<_BridgeSubscription>> subsMatrixKey = new HashMap<>();

    private synchronized _BridgeSubscription create(String subId, String email, String threadId, String mxId, String roomId) {
        log.info("Creating new subscription {} for email {} with threadId {} and matrix id {} in room {}",
                subId,
                email,
                threadId,
                mxId,
                roomId);

        _EmailEndPoint emEp = emMgr.getEndpoint(email, threadId);
        _MatrixEndPoint mxEp = mxMgr.getEndpoint(mxId, roomId);
        String eKey = emMgr.getKey(email, threadId);
        String mKey = mxMgr.getKey(mxId, roomId);

        _BridgeSubscription sub = new BridgeSubscription(subId, eKey, emEp, mKey, mxEp);
        sub.addListener(this::remove);

        subs.put(subId, sub);
        subsEmailKey.put(eKey, new WeakReference<>(sub));
        subsMatrixKey.put(mKey, new WeakReference<>(sub));

        return sub;
    }

    private synchronized _BridgeSubscription remove(_BridgeSubscription sub) {
        if (sub == null) {
            return null;
        }

        log.info("Removing subscription {}", sub.getId());
        subsEmailKey.remove(sub.getEmailKey());
        subsMatrixKey.remove(sub.getMatrixKey());
        subs.remove(sub.getId());

        return sub;
    }

    @Override
    public _BridgeSubscription create(String email, String mxId, String roomId) {
        String subId;
        do {
            subId = UUID.randomUUID().toString();
        } while (subs.containsKey(subId));

        String threadId = subId.replace("-", "");

        return create(subId, email, threadId, mxId, roomId);
    }

    @Override
    public _BridgeSubscription getOrCreate(String email, _MatrixClient mxUser, String roomId) {
        String mxId = mxUser.getUserId().getId();

        return getWithMatrixKey(mxMgr.getKey(mxId, roomId)).orElseGet(() -> create(email, mxId, roomId));
    }

    private Optional<_BridgeSubscription> validateExisting(String key, Map<String, WeakReference<_BridgeSubscription>> subs) {
        WeakReference<_BridgeSubscription> ref = subs.get(key);
        if (ref != null) {
            _BridgeSubscription sub = ref.get();
            if (sub != null) {
                return Optional.of(sub);
            } else {
                log.warn("Found existing mapping for obsolete key {}", key);
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<_BridgeSubscription> getWithEmailKey(String emailKey) {
        return validateExisting(emailKey, subsEmailKey);
    }

    @Override
    public Optional<_BridgeSubscription> getWithMatrixKey(String matrixKey) {
        return validateExisting(matrixKey, subsMatrixKey);
    }

}
