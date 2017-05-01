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
import io.kamax.matrix.bridge.email.config.EntityTemplate;
import io.kamax.matrix.bridge.email.config.HomeserverConfig;
import io.kamax.matrix.bridge.email.exception.InvalidMatrixIdException;
import io.kamax.matrix.bridge.email.exception.RoomNotFoundException;
import io.kamax.matrix.bridge.email.exception.UserNotFoundException;
import io.kamax.matrix.client._MatrixClient;
import io.kamax.matrix.client.as.MatrixApplicationServiceClient;
import io.kamax.matrix.client.as._MatrixApplicationServiceClient;
import io.kamax.matrix.client.regular.MatrixClient;
import io.kamax.matrix.hs.MatrixHomeserver;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.matrix.hs._MatrixHomeserver;
import io.kamax.matrix.hs._MatrixRoom;
import io.kamax.matrix.hs.event._MatrixEvent;
import io.kamax.matrix.hs.event._RoomMembershipEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MatrixHomeserverEmailBridge {

    private Logger log = LoggerFactory.getLogger(MatrixHomeserverEmailBridge.class);

    @Autowired
    private _SubscriptionManager subMgr;

    @Autowired
    private BridgeEmailCodec emailCodec;

    private HomeserverConfig cfg;

    private List<Pattern> patterns;

    private _MatrixHomeserver hs;
    private _MatrixApplicationServiceClient globalUser;
    private Map<String, _MatrixBridgeUser> vUsers = new HashMap<>();

    public MatrixHomeserverEmailBridge(HomeserverConfig cfg) throws URISyntaxException {
        this.cfg = cfg;

        hs = new MatrixHomeserver(cfg.getDomain(), cfg.getHost());
        globalUser = new MatrixApplicationServiceClient(hs, cfg.getAsToken(), cfg.getLocalpart());

        patterns = new ArrayList<>();
        for (EntityTemplate entityTemplate : cfg.getUsers()) {
            patterns.add(Pattern.compile(entityTemplate.getTemplate().replace("%EMAIL%", "(?<email>.*)")));
        }
        if (patterns.size() < 1) {
            log.error("At least one user template must be configured");
            System.exit(1);
        }
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
        return vUsers.containsKey(mxId.getId()) || findMatcherForUser(mxId).isPresent();
    }

    protected Optional<_MatrixBridgeUser> findClientForUser(_MatrixID mxId) {
        return Optional.ofNullable(vUsers.computeIfAbsent(mxId.getId(), id -> {
            Optional<Matcher> mOpt = findMatcherForUser(mxId);
            if (!mOpt.isPresent()) {
                return null;
            }

            String email = emailCodec.decode(mOpt.get().group("email"));
            _MatrixClient client = new MatrixClient(globalUser.getHomeserver(), globalUser.getAccessToken(), mxId);
            _MatrixBridgeUser user = new BridgeUser(client, email);
            return user;
        }));
    }

    public void queryUser(UserQuery query) throws UserNotFoundException {
        Optional<_MatrixBridgeUser> mOpt = findClientForUser(query.getId());
        if (!mOpt.isPresent()) {
            throw new InvalidMatrixIdException(query.getId().getId());
        }

        _MatrixClient user = globalUser.createUser(query.getId().getLocalPart());
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
            } else {
                log.info("Unknown event type {} from {}", event.getType(), event.getSender());
            }
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

            if (!user.is(globalUser)) {
                log.info("We are a bridge user, registering subscription");

                _BridgeSubscription sub = subMgr.getOrCreate(ev.getRoomId(), user);
                log.info("Subscription ID: {}", sub.getId());
            }
        }

        if (RoomMembership.Leave.is(ev.getMembership())) {
            log.info("Left room {} on {} as {}", ev.getRoomId(), hs.getDomain(), ev.getInvitee());

            if (!user.is(globalUser)) {
                log.info("We are a bridge user, removing subscription");

                subMgr.remove(user.getEmail(), ev.getRoomId(), hs.getDomain());
            }
        }
    }

}
