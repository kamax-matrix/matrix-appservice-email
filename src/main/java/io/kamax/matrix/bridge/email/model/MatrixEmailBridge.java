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
import io.kamax.matrix.bridge.email.config.IdentityConfig;
import io.kamax.matrix.bridge.email.exception.InvalidHomeserverTokenException;
import io.kamax.matrix.bridge.email.exception.InvalidMatrixIdException;
import io.kamax.matrix.bridge.email.exception.NoHomeserverTokenException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class MatrixEmailBridge implements _MatrixEmailBridge, InitializingBean {

    private Logger log = LoggerFactory.getLogger(MatrixEmailBridge.class);

    @Autowired
    private HomeserverConfig hsCfg;

    @Autowired
    private IdentityConfig isCfg;

    private Map<String, MatrixEmailBridgeHomeserverHandler> handlers;
    private BridgeEmailCodec emailCodec;

    @Override
    public void afterPropertiesSet() throws Exception {
        handlers = new HashMap<>();
        handlers.put(hsCfg.getHsToken(), new MatrixEmailBridgeHomeserverHandler(hsCfg));

        emailCodec = new BridgeEmailCodec();
    }

    protected MatrixEmailBridgeHomeserverHandler validateCredentials(AHomeserverCall call) {
        if (StringUtils.isEmpty(call.getCredentials())) {
            log.warn("No credentials supplied");

            throw new NoHomeserverTokenException();
        }

        if (!handlers.containsKey(call.getCredentials())) {
            log.warn("Invalid credentials");

            throw new InvalidHomeserverTokenException();
        }

        log.info("HS provided valid credentials"); // TODO switch to debug later
        return handlers.get(call.getCredentials());
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

        String localpart = emailCodec.encode(isCfg.getTemplate(), threePid.getAddress());
        return Optional.of(new ThreePidMapping(threePid, new MatrixID(localpart, isCfg.getDomain())));
    }

    @Override
    public void queryUser(UserQuery query) {
        validateCredentials(query).queryUser(query);
    }

    @Override
    public void queryRoom(RoomQuery query) {
        validateCredentials(query).queryRoom(query);
    }

    @Override
    public void push(MatrixTransactionPush transaction) {
        validateCredentials(transaction).push(transaction);
    }

}
