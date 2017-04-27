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

import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.ThreePidMapping;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix.bridge.email.config.HomeserverConfig;
import io.kamax.matrix.bridge.email.exception.InvalidHomeserverTokenException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MatrixEmailBridge implements _MatrixEmailBridge {

    private Logger log = LoggerFactory.getLogger(MatrixEmailBridge.class);

    @Autowired
    private HomeserverConfig hsCfg;

    protected void validateCredentials(AHomeserverCall call) {
        if (!StringUtils.equals(hsCfg.getHsToken(), call.getCredentials())) {
            log.warn("Invalid credentials from HS!");

            throw new InvalidHomeserverTokenException();
        }

        log.info("HS provided valid credentials"); // TODO switch to debug later
    }

    @Override
    public Optional<ThreePidMapping> getMatrixId(ThreePid threePid) {
        if (!ThreePidMedium.Email.is(threePid.getMedium())) {
            return Optional.empty();
        }

        String localpart = hsCfg.getUsers().get(0).getTemplate().replace("%EMAIL%", threePid.getAddress().replaceAll("@", "="));
        return Optional.of(new ThreePidMapping(threePid, new MatrixID(localpart, hsCfg.getDomain())));
    }

    @Override
    public void queryUser(UserQuery query) {
        validateCredentials(query);

        // TODO stub
    }

    @Override
    public void queryRoom(RoomQuery query) {
        validateCredentials(query);

        // TODO stub
    }

    @Override
    public void push(MatrixTransactionPush transaction) {
        validateCredentials(transaction);

        // TODO stub
    }

}
