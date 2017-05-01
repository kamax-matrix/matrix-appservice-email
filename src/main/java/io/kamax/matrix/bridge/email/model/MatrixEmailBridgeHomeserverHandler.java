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
import io.kamax.matrix.hs._Room;
import io.kamax.matrix.hs.event._MatrixEvent;
import io.kamax.matrix.hs.event._RoomMembershipEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatrixEmailBridgeHomeserverHandler {

    private Logger log = LoggerFactory.getLogger(MatrixEmailBridgeHomeserverHandler.class);

    private HomeserverConfig cfg;
    private BridgeEmailCodec emailCodec;

    private List<Pattern> patterns;

    private _MatrixApplicationServiceClient globalUser;
    private Map<String, _MatrixClient> vClients = new HashMap<>();

    public MatrixEmailBridgeHomeserverHandler(HomeserverConfig cfg) throws URISyntaxException {
        this.cfg = cfg;

        _MatrixHomeserver hs = new MatrixHomeserver(cfg.getDomain(), cfg.getHost());
        globalUser = new MatrixApplicationServiceClient(hs, cfg.getAsToken(), cfg.getLocalpart());

        patterns = new ArrayList<>();
        for (EntityTemplate entityTemplate : cfg.getUsers()) {
            patterns.add(Pattern.compile(entityTemplate.getTemplate().replace("%EMAIL%", "(?<email>.*)")));
        }
        if (patterns.size() < 1) {
            log.error("At least one user template must be configured");
            System.exit(1);
        }

        emailCodec = new BridgeEmailCodec();
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
        return vClients.containsKey(mxId.getId()) || findMatcherForUser(mxId).isPresent();
    }

    protected Optional<_MatrixClient> findClientForUser(_MatrixID mxId) {
        return Optional.ofNullable(vClients.computeIfAbsent(mxId.getId(), id -> {
            if (!isOurUser(mxId)) {
                return null;
            }

            return new MatrixClient(globalUser.getHomeserver(), globalUser.getAccessToken(), mxId);
        }));
    }

    public void queryUser(UserQuery query) throws UserNotFoundException {
        Optional<Matcher> mOpt = findMatcherForUser(query.getId());
        if (!mOpt.isPresent()) {
            throw new InvalidMatrixIdException(query.getId().getId());
        }

        _MatrixClient user = globalUser.createUser(query.getId().getLocalPart());
        vClients.put(query.getId().getId(), user);

        user.setDisplayName(emailCodec.decode(mOpt.get().group("email")) + " (Bridge)");
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

        Optional<_MatrixClient> clientOpt = findClientForUser(ev.getInvitee());
        if (!clientOpt.isPresent()) {
            log.info("Event is not for us, skipping");
            return;
        }

        handleMembershipEvent(clientOpt.get(), ev);
    }

    private void handleMembershipEvent(_MatrixClient client, _RoomMembershipEvent ev) {
        _Room room = client.getRoom(ev.getRoomId());

        if (RoomMembership.Invite.is(ev.getMembership())) {
            log.info("Joining room {} as {}", ev.getRoomId(), ev.getInvitee());

            room.join();
        }
    }

}
