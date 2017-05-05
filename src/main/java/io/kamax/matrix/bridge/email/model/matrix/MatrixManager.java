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

import io.kamax.matrix.*;
import io.kamax.matrix.bridge.email.config.matrix.EntityTemplate;
import io.kamax.matrix.bridge.email.config.matrix.HomeserverConfig;
import io.kamax.matrix.bridge.email.config.matrix.IdentityConfig;
import io.kamax.matrix.bridge.email.exception.InvalidHomeserverTokenException;
import io.kamax.matrix.bridge.email.exception.InvalidMatrixIdException;
import io.kamax.matrix.bridge.email.exception.NoHomeserverTokenException;
import io.kamax.matrix.bridge.email.exception.RoomNotFoundException;
import io.kamax.matrix.bridge.email.model.ABridgeMessage;
import io.kamax.matrix.bridge.email.model.BridgeEmailCodec;
import io.kamax.matrix.bridge.email.model.BridgeMessageTextContent;
import io.kamax.matrix.bridge.email.model.subscription._BridgeSubscription;
import io.kamax.matrix.bridge.email.model.subscription._SubscriptionManager;
import io.kamax.matrix.client._MatrixClient;
import io.kamax.matrix.client.as.MatrixApplicationServiceClient;
import io.kamax.matrix.client.as._MatrixApplicationServiceClient;
import io.kamax.matrix.client.regular.MatrixClient;
import io.kamax.matrix.event._MatrixEvent;
import io.kamax.matrix.event._RoomMembershipEvent;
import io.kamax.matrix.event._RoomMessageEvent;
import io.kamax.matrix.hs.MatrixHomeserver;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.matrix.hs._MatrixHomeserver;
import io.kamax.matrix.hs._MatrixRoom;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MatrixManager implements _MatrixApplicationService, _MatrixManager, InitializingBean {

    private Logger log = LoggerFactory.getLogger(MatrixManager.class);

    @Autowired
    private HomeserverConfig hsCfg;

    @Autowired
    private IdentityConfig isCfg;

    @Autowired
    private BridgeEmailCodec emailCodec;

    @Autowired
    private _SubscriptionManager subMgr;

    private _MatrixHomeserver hs;
    private _MatrixApplicationServiceClient mgr;

    private List<Pattern> patterns;
    private Map<String, _MatrixBridgeUser> vMxUsers = new HashMap<>();
    private Map<String, MatrixEndPoint> endpoints = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        patterns = new ArrayList<>();
        for (EntityTemplate entityTemplate : hsCfg.getUsers()) {
            patterns.add(Pattern.compile(entityTemplate.getTemplate().replace("%EMAIL%", "(?<email>.*)")));
        }
        if (patterns.size() < 1) {
            log.error("At least one user template must be configured");
            System.exit(1);
        }

        hs = new MatrixHomeserver(hsCfg.getDomain(), hsCfg.getHost());
        mgr = new MatrixApplicationServiceClient(hs, hsCfg.getAsToken(), hsCfg.getLocalpart());
    }

    private _MatrixApplicationServiceClient validateCredentials(AHomeserverCall call) {
        if (StringUtils.isEmpty(call.getCredentials())) {
            log.warn("No credentials supplied");

            throw new NoHomeserverTokenException();
        }

        if (!hsCfg.getHsToken().contentEquals(call.getCredentials())) {
            log.warn("Invalid credentials");

            throw new InvalidHomeserverTokenException();
        }

        log.info("HS provided valid credentials"); // TODO switch to debug later

        return mgr;
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

    private Optional<_MatrixBridgeUser> findClientForUser(_MatrixID mxId) {
        return Optional.ofNullable(vMxUsers.computeIfAbsent(mxId.getId(), id -> {
            Optional<Matcher> mOpt = findMatcherForUser(mxId);
            if (!mOpt.isPresent()) {
                return null;
            }

            String email = emailCodec.decode(mOpt.get().group("email"));

            log.info("Creating new Matrix client for {} as {}", email, mxId);
            _MatrixClient client = new MatrixClient(mgr.getHomeserver(), mgr.getAccessToken(), mxId);
            return new MatrixBridgeUser(client, email);
        }));
    }

    private boolean isOurUser(_MatrixID mxId) {
        return vMxUsers.containsKey(mxId.getId()) || findMatcherForUser(mxId).isPresent();
    }

    private void pushMessageEvent(_RoomMessageEvent ev) {
        log.info("We got message event {} in {}", ev.getType(), ev.getRoomId());

        log.info("Computing forward list");
        log.info("Listing users in the room {}", ev.getRoomId());
        List<_MatrixID> users = mgr.getRoom(ev.getRoomId()).getJoinedUsers();
        for (_MatrixID user : users) {
            if (!isOurUser(user)) {
                log.info("{} is not a bridged user, skipping", user);
                continue;
            }

            if (user.equals(ev.getSender())) {
                log.info("{} is the original sender of the event, skipping", user);
                continue;
            }

            log.info("{} is a valid potential bridge user", user);
            Optional<_MatrixBridgeUser> userOpt = findClientForUser(user);
            if (!userOpt.isPresent()) {
                log.warn("No Matrix client for MXID {} while present in the room", user);
                continue;
            }

            MatrixEndPoint ep = getEndpoint(user.getId(), ev.getRoomId());

            log.info("Injecting message {} from room {} to {}", ev.getId(), ev.getRoomId(), user);
            ep.inject(new MatrixMessage(ev.getId(), mgr.getUser(ev.getSender()), ev.getBody()));
        }
    }

    private void pushMembershipEvent(_RoomMembershipEvent ev) {
        log.info("We got membership event {} for {}", ev.getMembership(), ev.getInvitee());

        Optional<_MatrixBridgeUser> clientOpt = findClientForUser(ev.getInvitee());
        if (!clientOpt.isPresent()) {
            log.info("Event is not for us, skipping");
            return;
        }

        handleMembershipEvent(clientOpt.get(), ev);
    }

    private void handleMembershipEvent(_MatrixBridgeUser user, _RoomMembershipEvent ev) {
        _MatrixRoom room = user.getClient().getRoom(ev.getRoomId());

        if (RoomMembership.Invite.is(ev.getMembership())) {
            log.info("Joining room {} on {} as {}", ev.getRoomId(), hs.getDomain(), ev.getInvitee());

            room.join();
        }

        if (RoomMembership.Join.is(ev.getMembership())) {
            log.info("Joined room {} on {} as {}", ev.getRoomId(), hs.getDomain(), ev.getInvitee());

            if (!user.is(mgr)) {
                log.info("We are a bridge user, registering subscription");

                _BridgeSubscription sub = subMgr.getOrCreate(user.getEmail(), user.getClient(), ev.getRoomId());
                sub.addListener(sub1 -> destroyEndpoint(sub1.getMatrixEndpoint()));

                log.info("Subscription | Matrix key: {} | Email key: {}", sub.getMatrixKey(), sub.getEmailKey());
            }
        }

        if (RoomMembership.Leave.is(ev.getMembership())) {
            log.info("Left room {} on {} as {}", ev.getRoomId(), hs.getDomain(), ev.getInvitee());

            if (!user.is(mgr)) {
                log.info("We are a bridge user, removing subscription");

                Optional<_BridgeSubscription> subOpt = subMgr.getWithEmailKey(getKey(user.getClient().getUserId().getId(), ev.getRoomId()));
                if (subOpt.isPresent()) {
                    log.info("Subscription is still active, canceling");
                    subOpt.get().terminate();
                } else {
                    log.info("Subscription was already removed, skipping");
                }
            }
        }
    }

    private String encode(String template, String email) {
        return template.replace("%EMAIL%", emailCodec.encode(email));
    }

    @Override
    public _MatrixID getId(String mxId) throws InvalidMatrixIdException {
        try {
            return new MatrixID(mxId);
        } catch (IllegalArgumentException e) {
            throw new InvalidMatrixIdException(e);
        }
    }

    @Override
    public Optional<ThreePidMapping> getMatrixId(ThreePid threePid) {
        if (!ThreePidMedium.Email.is(threePid.getMedium())) {
            return Optional.empty();
        }

        String localpart = encode(isCfg.getTemplate(), threePid.getAddress());
        return Optional.of(new ThreePidMapping(threePid, new MatrixID(localpart, isCfg.getDomain())));
    }

    @Override
    public void queryUser(UserQuery query) {
        validateCredentials(query);

        Optional<_MatrixBridgeUser> mOpt = findClientForUser(query.getId());
        if (!mOpt.isPresent()) {
            throw new InvalidMatrixIdException(query.getId().getId());
        }

        _MatrixClient user = mgr.createUser(query.getId().getLocalPart());
        user.setDisplayName(mOpt.get().getEmail() + " (Bridge)");
    }

    @Override
    public void queryRoom(RoomQuery query) {
        validateCredentials(query);

        log.error("Room {} was requested, but we don't handle rooms", query.getAlias());
        throw new RoomNotFoundException();
    }

    @Override
    public void push(MatrixTransactionPush transaction) {
        validateCredentials(transaction);

        for (_MatrixEvent event : transaction.getEvents()) {
            if (event instanceof _RoomMembershipEvent) {
                pushMembershipEvent((_RoomMembershipEvent) event);
            } else if (event instanceof _RoomMessageEvent) {
                pushMessageEvent((_RoomMessageEvent) event);
            } else {
                log.info("Unknown event type {} from {}", event.getType(), event.getSender());
            }
        }
    }

    @Override
    public String getKey(String mxId, String roomId) {
        return mxId + "|" + roomId;
    }

    private MatrixEndPoint createEndpoint(_MatrixClient client, String roomId) {
        String id = getKey(client.getUserId().getId(), roomId);
        MatrixEndPoint ep = new MatrixEndPoint(id, client, roomId);
        endpoints.put(id, ep);
        return ep;
    }

    private void destroyEndpoint(_MatrixEndPoint ep) {
        endpoints.remove(getKey(ep.getIdentity().getId(), ep.getChannelId()));
    }

    @Override
    public synchronized MatrixEndPoint getEndpoint(String mxId, String roomId) {
        MatrixEndPoint ep = endpoints.get(getKey(mxId, roomId));
        if (ep != null) {
            return ep;
        }

        Optional<_MatrixBridgeUser> client = findClientForUser(getId(mxId));
        if (!client.isPresent()) {
            throw new IllegalArgumentException(mxId + " is not a Matrix bridge user");
        }

        return createEndpoint(client.get().getClient(), roomId);
    }

    private class MatrixMessage extends ABridgeMessage<_MatrixUser> implements _MatrixBridgeMessage {

        MatrixMessage(String key, _MatrixUser sender, String txtContent) {
            super(key, sender, Collections.singletonList(new BridgeMessageTextContent(txtContent)));
        }

    }

}
