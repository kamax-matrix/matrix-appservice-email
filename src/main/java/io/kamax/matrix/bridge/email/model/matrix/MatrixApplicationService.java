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
import io.kamax.matrix.bridge.email.config.bridge.BridgeCommandConfig;
import io.kamax.matrix.bridge.email.config.bridge.BridgeInviteConfig;
import io.kamax.matrix.bridge.email.config.matrix.HomeserverConfig;
import io.kamax.matrix.bridge.email.config.matrix.IdentityConfig;
import io.kamax.matrix.bridge.email.exception.*;
import io.kamax.matrix.bridge.email.model.BridgeEmailCodec;
import io.kamax.matrix.bridge.email.model.subscription._BridgeSubscription;
import io.kamax.matrix.bridge.email.model.subscription._SubscriptionManager;
import io.kamax.matrix.client._MatrixClient;
import io.kamax.matrix.client.as._MatrixApplicationServiceClient;
import io.kamax.matrix.event._MatrixEvent;
import io.kamax.matrix.event._RoomMembershipEvent;
import io.kamax.matrix.event._RoomMessageEvent;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.matrix.hs._MatrixRoom;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MatrixApplicationService implements _MatrixApplicationService {

    private Logger log = LoggerFactory.getLogger(MatrixApplicationService.class);

    @Autowired
    private HomeserverConfig hsCfg;

    @Autowired
    private IdentityConfig isCfg;

    @Autowired
    private BridgeInviteConfig bInvCfg;

    @Autowired
    private BridgeCommandConfig bCmdCfg;

    @Autowired
    private BridgeEmailCodec emailCodec;

    @Autowired
    private MatrixManager mgr;

    @Autowired
    private _SubscriptionManager subMgr;

    private Pattern cmdOptsParser = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

    private String lastTransactionId;

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

        return mgr.getClient();
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

        return Optional.of(new ThreePidMapping(threePid, getMatrixId(threePid.getAddress())));
    }

    private _MatrixID getMatrixId(String email) {
        return new MatrixID(encode(isCfg.getTemplate(), email), isCfg.getDomain());
    }

    @Override
    public void queryUser(UserQuery query) throws UserNotFoundException {
        validateCredentials(query);

        Optional<_MatrixBridgeUser> mOpt = mgr.findClientForUser(query.getId());
        if (!mOpt.isPresent()) {
            throw new InvalidMatrixIdException(query.getId().getId());
        }

        _MatrixClient user = mgr.getClient().createUser(query.getId().getLocalPart());
        user.setDisplayName(mOpt.get().getEmail() + " (Bridge)");
    }

    @Override
    public void queryRoom(RoomQuery query) throws RoomNotFoundException {
        validateCredentials(query);

        log.error("Room {} was requested, but we don't handle rooms", query.getAlias());
        throw new RoomNotFoundException();
    }

    @Override
    public void push(MatrixTransactionPush transaction) {
        validateCredentials(transaction);

        if (StringUtils.equals(lastTransactionId, transaction.getId())) {
            log.info("Transaction {} has already been processed, skipping");
            return;
        }

        for (_MatrixEvent event : transaction.getEvents()) {
            if (event instanceof _RoomMembershipEvent) {
                pushMembershipEvent((_RoomMembershipEvent) event);
            } else if (event instanceof _RoomMessageEvent) {
                pushMessageEvent((_RoomMessageEvent) event);
            } else {
                log.info("Unknown event type {} from {}", event.getType(), event.getSender());
            }
        }

        lastTransactionId = transaction.getId();
    }

    private void pushMessageEvent(_RoomMessageEvent ev) {
        log.info("We got message event {} in {}", ev.getType(), ev.getRoomId());

        if (StringUtils.isBlank(ev.getBodyType())) {
            log.warn("Possibly invalid message type. Transaction info: {}", ev);
            return;
        }

        if (StringUtils.equals("m.notice", ev.getBodyType())) {
            log.info("Ignoring event with body type {}", ev.getBodyType());
            return;
        }

        if (ev.getBody().startsWith(bCmdCfg.getKeyword() + " ")) {
            handleCommand(ev, mgr.getClient());
        } else {
            log.debug("Computing forward list");
            log.debug("Listing users in the room {}", ev.getRoomId());
            List<_MatrixID> users = mgr.getClient().getRoom(ev.getRoomId()).getJoinedUsers();
            for (_MatrixID user : users) {
                if (!mgr.isOurUser(user)) {
                    log.debug("{} is not a bridged user, skipping", user);
                    continue;
                }

                if (user.equals(ev.getSender())) {
                    log.debug("{} is the original sender of the event, skipping", user);
                    continue;
                }

                log.debug("{} is a valid potential bridge user", user);
                Optional<_MatrixBridgeUser> userOpt = mgr.findClientForUser(user);
                if (!userOpt.isPresent()) {
                    log.warn("No Matrix client for MXID {} while present in the room", user);
                    continue;
                }

                MatrixEndPoint ep = mgr.getEndpoint(user.getId(), ev.getRoomId());
                log.info("Injecting message {} from room {} to {}", ev.getId(), ev.getRoomId(), user);
                ep.inject(new MatrixBridgeMessage(ev.getId(), ev.getTime(), mgr.getClient().getUser(ev.getSender()), ev.getBody()));
            }
        }
    }

    private void pushMembershipEvent(_RoomMembershipEvent ev) {
        log.info("We got membership event {} for {}", ev.getMembership(), ev.getInvitee());

        if (mgr.getClient().getUser().equals(ev.getInvitee())) {
            if (RoomMembership.Invite.is(ev.getMembership())) {
                _MatrixRoom room = mgr.getClient().getRoom(ev.getRoomId());
                log.info("Joining room {} as AS user", room.getAddress());
                room.join();
            } else {
                log.debug("Ignoring room {} membership {} for AS user", ev.getRoomId(), ev.getMembership());
            }
        } else {
            Optional<_MatrixBridgeUser> clientOpt = mgr.findClientForUser(ev.getInvitee());
            if (!clientOpt.isPresent()) {
                log.info("Event is not for us, skipping");
                return;
            }

            handleMembershipEvent(clientOpt.get(), ev);
        }
    }

    private void handleMembershipEvent(_MatrixBridgeUser user, _RoomMembershipEvent ev) {
        _MatrixRoom room = user.getClient().getRoom(ev.getRoomId());

        if (RoomMembership.Invite.is(ev.getMembership())) {
            if (!ev.getInvitee().isValid()) {
                log.warn("{} is not a valid MXID, declining invite", ev.getInvitee());

                room.leave();
                return;
            }

            if (!bInvCfg.getDomain().isEmpty() && !bInvCfg.getDomain().contains(ev.getSender().getDomain())) {
                log.info("{} is not part of allowed domains, rejecting invite", ev.getSender());
                room.leave();
                return;
            }

            _BridgeSubscription sub = subMgr.create(ev.getSender(), Instant.now(), user, ev.getRoomId());
            log.info("Subscription | Matrix key: {} | Email key: {}", sub.getMatrixKey(), sub.getEmailKey());

            log.info("Joining room {} on {} as {}", ev.getRoomId(), hsCfg.getDomain(), ev.getInvitee());

            room.join();
        }

        if (RoomMembership.Join.is(ev.getMembership())) {
            log.info("Joined room {} on {} as {}", ev.getRoomId(), hsCfg.getDomain(), ev.getInvitee());

            if (!user.is(mgr.getClient())) {
                log.info("We are a bridge user, registering subscription");

                Optional<_BridgeSubscription> subOpt = subMgr.getWithMatrixKey(mgr.getKey(user.getClient().getUser().getId(), ev.getRoomId()));
                if (!subOpt.isPresent()) {
                    log.info("Joined room without a subscription, leaving");
                    user.getClient().getRoom(ev.getRoomId()).leave();
                } else {
                    log.info("Starting subscription {}", subOpt.get().getId());
                    subOpt.get().commence();
                }
            }
        }

        if (RoomMembership.Leave.is(ev.getMembership())) {
            log.info("Left room {} on {} as {}", ev.getRoomId(), hsCfg.getDomain(), ev.getInvitee());

            if (!user.is(mgr.getClient())) {
                log.info("We are a bridge user, cleaning up endpoint and subscription");

                MatrixEndPoint ep = mgr.getEndpoint(user.getClient().getUser().getId(), ev.getRoomId());
                ep.close();

                Optional<_BridgeSubscription> subOpt = subMgr.getWithMatrixKey(mgr.getKey(user.getClient().getUser().getId(), ev.getRoomId()));
                if (subOpt.isPresent()) {
                    log.info("Subscription is still active, canceling");
                    subOpt.get().terminate();
                } else {
                    log.info("Subscription was already removed, skipping");
                }
            }
        }
    }

    private void showHelp(_MatrixRoom room) {
        room.sendNotice(bCmdCfg.getKeyword() + " mxisd <email>\t\tTranslate an e-mail into a Matrix ID");
    }

    // TODO turn commands into objects to make this more efficient and extendible
    private void handleCommand(_RoomMessageEvent ev, _MatrixApplicationServiceClient client) {
        List<String> opts = new ArrayList<>();
        Matcher m = cmdOptsParser.matcher(ev.getBody());
        while (m.find()) {
            opts.add(m.group(1));
        }

        _MatrixRoom room = client.getRoom(ev.getRoomId());
        if (opts.size() < 2) {
            showHelp(room);
        } else {
            switch (opts.get(1)) {
                case "mxid":
                    if (opts.size() != 3) {
                        showHelp(room);
                    } else {
                        String email = opts.get(2);
                        String mxid = getMatrixId(email).getId();
                        String plain = "MXID for " + email + " :   " + mxid;
                        String formatted = "MXID for " + email + ":<pre><code>" + mxid + "</code></pre>";
                        room.sendNotice(formatted, plain);
                    }

                    break;
                default:
                    showHelp(room);
            }
        }
    }

}
