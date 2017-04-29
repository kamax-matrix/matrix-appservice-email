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

import io.kamax.matrix.*;
import io.kamax.matrix.bridge.email.config.HomeserverConfig;
import io.kamax.matrix.bridge.email.exception.InvalidHomeserverTokenException;
import io.kamax.matrix.bridge.email.exception.InvalidMatrixIdException;
import io.kamax.matrix.bridge.email.exception.NoHomeserverTokenException;
import io.kamax.matrix.bridge.email.exception.RoomNotFoundException;
import io.kamax.matrix.client.MatrixApplicationServiceClient;
import io.kamax.matrix.client._MatrixApplicationServiceClient;
import io.kamax.matrix.hs.MatrixHomeserver;
import io.kamax.matrix.hs._MatrixHomeserver;
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
public class MatrixEmailBridge implements _MatrixEmailBridge, InitializingBean {

    private Logger log = LoggerFactory.getLogger(MatrixEmailBridge.class);

    @Autowired
    private HomeserverConfig hsCfg;

    private Map<String, _MatrixApplicationServiceClient> clients;
    private List<Pattern> patterns;

    protected String decodeEmail(String email) {
        return email.replace("=", "@");
    }

    protected String encodeEmail(String email) {
        return hsCfg.getUsers().get(0).getTemplate().replace("%EMAIL%", encodeEmail(email.replace("@", "=")));
    }

    protected String decodeEmail(_MatrixID mxId) {
        for (Pattern p : patterns) {
            Matcher m = p.matcher(mxId.getLocalPart());
            if (m.matches()) {
                return m.group(1).replace("=", "@");
            }
        }

        throw new IllegalArgumentException(mxId.getLocalPart() + " is not an encoded e-mail address");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (hsCfg.getUsers().size() < 1) {
            log.error("At least one user template must be configured");
            System.exit(1);
        }
        _MatrixHomeserver hs = new MatrixHomeserver(hsCfg.getHost());
        _MatrixApplicationServiceClient client = new MatrixApplicationServiceClient();
        client.setHomeserver(hs);
        client.setAccessToken(hsCfg.getAsToken());

        clients = new HashMap<>();
        clients.put(hsCfg.getHsToken(), client);

        patterns = new ArrayList<>();
        patterns.add(Pattern.compile(hsCfg.getUsers().get(0).getTemplate().replace("%EMAIL%", "(.*)")));
    }

    protected _MatrixApplicationServiceClient validateCredentials(AHomeserverCall call) {
        if (StringUtils.isEmpty(call.getCredentials())) {
            log.warn("No credentials supplied");

            throw new NoHomeserverTokenException();
        }

        if (!StringUtils.equals(hsCfg.getHsToken(), call.getCredentials())) {
            log.warn("Invalid credentials from HS!");

            throw new InvalidHomeserverTokenException();
        }

        log.info("HS provided valid credentials"); // TODO switch to debug later
        return clients.get(call.getCredentials());
    }

    @Override
    public _MatrixID getId(String mxId) throws InvalidMatrixIdException {
        try {
            return new MatrixID(mxId);
        } catch (IllegalArgumentException e) {
            throw new InvalidMatrixIdException();
        }
    }

    @Override
    public Optional<ThreePidMapping> getMatrixId(ThreePid threePid) {
        if (!ThreePidMedium.Email.is(threePid.getMedium())) {
            return Optional.empty();
        }

        String localpart = encodeEmail(threePid.getAddress());
        return Optional.of(new ThreePidMapping(threePid, new MatrixID(localpart, hsCfg.getDomain())));
    }

    @Override
    public void queryUser(UserQuery query) {
        _MatrixApplicationServiceClient client = validateCredentials(query);

        MatrixUser user = new MatrixUser();
        user.setId(query.getId());
        user.setName(decodeEmail(query.getId()) + " (Bridge)");

        client.createUser(user);
    }

    @Override
    public void queryRoom(RoomQuery query) {
        validateCredentials(query);

        throw new RoomNotFoundException();
    }

    @Override
    public void push(MatrixTransactionPush transaction) {
        validateCredentials(transaction);

        throw new IllegalStateException();
    }

}
