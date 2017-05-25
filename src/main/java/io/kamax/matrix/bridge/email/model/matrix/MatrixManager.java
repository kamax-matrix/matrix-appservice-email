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

import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.bridge.email.config.matrix.EntityTemplateConfig;
import io.kamax.matrix.bridge.email.config.matrix.HomeserverConfig;
import io.kamax.matrix.bridge.email.config.subscription.MatrixNotificationConfig;
import io.kamax.matrix.bridge.email.model.BridgeEmailCodec;
import io.kamax.matrix.bridge.email.model._EndPoint;
import io.kamax.matrix.client._MatrixClient;
import io.kamax.matrix.client.as.MatrixApplicationServiceClient;
import io.kamax.matrix.client.as._MatrixApplicationServiceClient;
import io.kamax.matrix.hs.MatrixHomeserver;
import io.kamax.matrix.hs._MatrixHomeserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MatrixManager implements _MatrixManager, InitializingBean {

    private Logger log = LoggerFactory.getLogger(MatrixManager.class);

    @Autowired
    private HomeserverConfig hsCfg;

    @Autowired
    private MatrixNotificationConfig notifCfg;

    @Autowired
    private BridgeEmailCodec emailCodec;

    private _MatrixApplicationServiceClient mgr;

    private List<Pattern> patterns;
    private Map<String, _MatrixBridgeUser> vMxUsers = new HashMap<>();
    private Map<String, MatrixEndPoint> endpoints = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        patterns = new ArrayList<>();
        for (EntityTemplateConfig entityTemplate : hsCfg.getUsers()) {
            patterns.add(Pattern.compile(entityTemplate.getTemplate().replace("%EMAIL%", "(?<email>.*)")));
        }
        if (patterns.size() < 1) {
            log.error("At least one user template must be configured");
            System.exit(1);
        }

        _MatrixHomeserver hs = new MatrixHomeserver(hsCfg.getDomain(), hsCfg.getHost());
        mgr = new MatrixApplicationServiceClient(hs, hsCfg.getAsToken(), hsCfg.getLocalpart());
    }

    private Optional<Matcher> findMatcherForUser(_MatrixID mxId) {
        for (Pattern p : patterns) {
            Matcher m = p.matcher(mxId.getLocalPart());
            if (m.matches()) {
                return Optional.of(m);
            }
        }

        return Optional.empty();
    }

    public Optional<_MatrixBridgeUser> findClientForUser(_MatrixID mxId) {
        return Optional.ofNullable(vMxUsers.computeIfAbsent(mxId.getId(), id -> {
            Optional<Matcher> mOpt = findMatcherForUser(mxId);
            if (!mOpt.isPresent()) {
                return null;
            }

            String email = emailCodec.decode(mOpt.get().group("email"));

            log.info("Creating new Matrix client for {} as {}", email, mxId);
            return new MatrixBridgeUser(mgr.getUser(mxId.getLocalPart()), email);
        }));
    }

    public boolean isOurUser(_MatrixID mxId) {
        return vMxUsers.containsKey(mxId.getId()) || findMatcherForUser(mxId).isPresent();
    }

    @Override
    public String getKey(String mxId, String roomId) {
        return mxId + "|" + roomId;
    }

    private MatrixEndPoint createEndpoint(_MatrixClient client, String roomId) {
        String id = getKey(client.getUser().getId(), roomId);
        MatrixEndPoint ep = new MatrixEndPoint(id, client, roomId, notifCfg);
        ep.addStateListener(this::destroyEndpoint);
        endpoints.put(id, ep);
        return ep;
    }

    private void destroyEndpoint(_EndPoint ep) {
        endpoints.remove(ep.getId());
    }

    @Override
    public synchronized MatrixEndPoint getEndpoint(String mxId, String roomId) {
        MatrixEndPoint ep = endpoints.get(getKey(mxId, roomId));
        if (ep != null) {
            return ep;
        }

        Optional<_MatrixBridgeUser> client = findClientForUser(new MatrixID(mxId));
        if (!client.isPresent()) {
            throw new IllegalArgumentException(mxId + " is not a Matrix bridge user");
        }

        return createEndpoint(client.get().getClient(), roomId);
    }

    public _MatrixApplicationServiceClient getClient() {
        return mgr;
    }

}
