/*
 * matrix-appservice-email - Matrix Bridge to E-mail
 * Copyright (C) 2017 Kamax Sarl
 *
 * https://www.kamax.io/
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

package io.kamax.matrix.bridge.email.model.email;

import io.kamax.matrix.bridge.email.config.email.EmailSenderConfig;
import io.kamax.matrix.bridge.email.model._EndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class EmailManager implements InitializingBean, _EmailManager {

    private Logger log = LoggerFactory.getLogger(EmailManager.class);

    @Autowired
    private EmailSenderConfig sendCfg;

    @Autowired
    private _EmailFormatterOutbound formatOut;

    @Autowired
    private _EmailFormatterInbound formatIn;

    @Autowired
    private _EmailFetcher fetcher;

    private Map<String, EmailEndPoint> endpoints = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        fetcher.addListener((key, email) -> {
            Optional<_EmailBridgeMessage> msgOpt = formatIn.get(key, email);
            if (!msgOpt.isPresent()) {
                log.info("Inbound formatter did not return anything, skipping");
            }
            _EmailBridgeMessage msg = msgOpt.get();

            EmailEndPoint ep = endpoints.get(key);
            if (ep == null) {
                // TODO implement
                log.warn("DROP: Received e-mail with invalid key {} from {}", msg.getKey(), msg.getSender());
                return;
            }

            if (!ep.getIdentity().contentEquals(msg.getSender())) {
                log.warn("DROP: Received e-mail with invalid sender: from {} but supposed to be {}", msg.getSender(), ep.getIdentity());
            }

            log.info("Injecting e-mail from {} with key {}", msg.getSender(), msg.getKey());
            ep.inject(msg);
        });

        fetcher.connect();
    }

    private EmailEndPoint createEndpoint(String email, String threadId) {
        String id = getKey(email, threadId);
        EmailEndPoint ep = new EmailEndPoint(id, email, threadId, sendCfg, formatOut);
        ep.addStateListener(this::destroyEndpoint);
        endpoints.put(id, ep);

        log.info("Created new email endpoint {} for {}", id, email);

        return ep;
    }

    private void destroyEndpoint(_EndPoint endpoint) {
        endpoints.remove(endpoint.getId());
    }

    @Override
    public String getKey(String email, String threadId) {
        return threadId;
    }

    @Override
    public EmailEndPoint getEndpoint(String email, String threadId) {
        EmailEndPoint ep = endpoints.get(getKey(email, threadId));
        if (ep != null) {
            return ep;
        }

        return createEndpoint(email, threadId);
    }

}
