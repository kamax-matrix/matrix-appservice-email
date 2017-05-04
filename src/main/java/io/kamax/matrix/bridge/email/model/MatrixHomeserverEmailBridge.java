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

import io.kamax.matrix._MatrixID;
import io.kamax.matrix._MatrixUser;
import io.kamax.matrix.bridge.email.config.matrix.EntityTemplate;
import io.kamax.matrix.bridge.email.config.matrix.HomeserverConfig;
import io.kamax.matrix.bridge.email.exception.InvalidMatrixIdException;
import io.kamax.matrix.bridge.email.exception.RoomNotFoundException;
import io.kamax.matrix.bridge.email.exception.UserNotFoundException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MatrixHomeserverEmailBridge implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(MatrixHomeserverEmailBridge.class);

    @Autowired
    private _SubscriptionManager subMgr;

    @Autowired
    private BridgeEmailCodec emailCodec;

    private List<Pattern> patterns;

    private _MatrixHomeserver hs;
    private _MatrixApplicationServiceClient mxMgr;
    private Map<String, _MatrixBridgeUser> vMxUsers = new HashMap<>();

    @Autowired
    private _EmailManager emMgr;
    private Map<String, _EmailClient> vEmUsers = new HashMap<>();

    public MatrixHomeserverEmailBridge(HomeserverConfig cfg) throws URISyntaxException {
        hs = new MatrixHomeserver(cfg.getDomain(), cfg.getHost());
        mxMgr = new MatrixApplicationServiceClient(hs, cfg.getAsToken(), cfg.getLocalpart());

        patterns = new ArrayList<>();
        for (EntityTemplate entityTemplate : cfg.getUsers()) {
            patterns.add(Pattern.compile(entityTemplate.getTemplate().replace("%EMAIL%", "(?<email>.*)")));
        }
        if (patterns.size() < 1) {
            log.error("At least one user template must be configured");
            System.exit(1);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        subMgr.addListener(new _SubscriptionListener() {

            @Override
            public void created(_BridgeSubscription sub) {
                log.info("Subscription created for {} in room {}", sub.getEmailEndpoint().getIdentity(), sub.getRoomId());

            }

            @Override
            public void destroyed(_BridgeSubscription sub) {
                log.info("Subscription destroyed for {} in room {}", sub.getEmailEndpoint().getIdentity(), sub.getRoomId());
                sub.getMatrixUser().getRoom(sub.getRoomId()).leave();
            }

        });

        emMgr.addListener(msg -> {
            Optional<_BridgeSubscription> subOpt = subMgr.get(msg.getKey());
            if (!subOpt.isPresent()) {
                // TODO send error email
                log.warn("Got e-mail message with invalid key {}", msg.getKey());
                return;
            }

            subOpt.get().forward(msg);
            log.info("E-mail with key {} was forwarded", msg.getKey());
        });

        emMgr.connect();
    }

    protected Optional<Matcher> findMatcherForUser(_MatrixID mxId) {
        for (Pattern p : patterns) {
            Matcher m = p.matcher(mxId.getLocalPart());
            if (m.matches()) {
                return Optional.of(m);
            }
        }

        return Optional.empty();
    }

    protected boolean isOurUser(_MatrixID mxId) {
        return vMxUsers.containsKey(mxId.getId()) || findMatcherForUser(mxId).isPresent();
    }

    protected Optional<_MatrixBridgeUser> findClientForUser(_MatrixID mxId) {
        return Optional.ofNullable(vMxUsers.computeIfAbsent(mxId.getId(), id -> {
            Optional<Matcher> mOpt = findMatcherForUser(mxId);
            if (!mOpt.isPresent()) {
                return null;
            }

            String email = emailCodec.decode(mOpt.get().group("email"));

            log.info("Creating new Matrix client for {} as {}", email, mxId);
            _MatrixClient client = new MatrixClient(mxMgr.getHomeserver(), mxMgr.getAccessToken(), mxId);
            return new BridgeUser(client, email);
        }));
    }

    protected _EmailClient findClientForUser(String email) {
        return vEmUsers.computeIfAbsent(email, id -> emMgr.getClient(email));
    }

    public void queryUser(UserQuery query) throws UserNotFoundException {
        Optional<_MatrixBridgeUser> mOpt = findClientForUser(query.getId());
        if (!mOpt.isPresent()) {
            throw new InvalidMatrixIdException(query.getId().getId());
        }

        _MatrixClient user = mxMgr.createUser(query.getId().getLocalPart());
        user.setDisplayName(mOpt.get().getEmail() + " (Bridge)");
    }

    public void queryRoom(RoomQuery query) throws RoomNotFoundException {
        log.error("Room {} was requested, but we don't handle rooms", query.getAlias());

        throw new RoomNotFoundException();
    }

    public void push(MatrixTransactionPush transaction) {
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

    private void pushMessageEvent(_RoomMessageEvent ev) {
        log.info("We got message event {} in {}", ev.getType(), ev.getRoomId());

        log.info("Computing forward list");
        log.info("Listing users in the room {}", ev.getRoomId());
        List<_MatrixID> users = mxMgr.getRoom(ev.getRoomId()).getJoinedUsers();
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

            Optional<_BridgeSubscription> subOpt = subMgr.get(ev.getRoomId(), userOpt.get());
            if (!subOpt.isPresent()) {
                log.warn("No subscription for MXID {} while present in the room", user);
                continue;
            }

            log.info("Forwarding message {} from room {} to {}", ev.getId(), ev.getRoomId(), user);
            subOpt.get().forward(new MatrixMessage(subOpt.get().getMatrixKey(), mxMgr.getUser(ev.getSender()), ev.getBody()));
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

            if (!user.is(mxMgr)) {
                log.info("We are a bridge user, registering subscription");

                _BridgeSubscription sub = subMgr.getOrCreate(ev.getRoomId(), user.getClient(), findClientForUser(user.getEmail()));
                log.info("Subscription | Matrix key: {} | Email key: {}", sub.getMatrixKey(), sub.getEmailKey());
            }
        }

        if (RoomMembership.Leave.is(ev.getMembership())) {
            log.info("Left room {} on {} as {}", ev.getRoomId(), hs.getDomain(), ev.getInvitee());

            if (!user.is(mxMgr)) {
                log.info("We are a bridge user, removing subscription");

                Optional<_BridgeSubscription> subOpt = subMgr.remove(ev.getRoomId(), user.getClient(), findClientForUser(user.getEmail()));
                if (subOpt.isPresent()) {
                    log.info("Subscription is still active, canceling");
                    subOpt.get().cancelFromMatrix();
                } else {
                    log.info("Subscription was already removed, skipping");
                }
            }
        }
    }

    private class MatrixMessage extends ABridgeMessage<_MatrixUser> implements _MatrixBridgeMessage {

        MatrixMessage(String key, _MatrixUser sender, String txtContent) {
            super(key, sender, Collections.singletonList(new BridgeMessageTextContent(txtContent)));
        }

    }

}
