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

package io.kamax.matrix.bridge.email.controller;

import io.kamax.matrix.bridge.email.exception.InvalidEmailKeyException;
import io.kamax.matrix.bridge.email.model.subscription.SubscriptionPortalService;
import io.kamax.matrix.bridge.email.model.subscription._BridgeSubscription;
import io.kamax.matrix.bridge.email.model.subscription._SubscriptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class SubscriptionController {

    private Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    @Autowired
    private _SubscriptionManager subMgr;

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidEmailKeyException.class)
    String handleBadRequest() {
        return "subscription/invalidToken";
    }

    @RequestMapping(value = SubscriptionPortalService.BASE_PATH, method = GET)
    public String listSubscriptions(Model model, @RequestParam String token) {
        log.info("Subscription list request");

        Optional<_BridgeSubscription> subOpt = subMgr.getWithEmailKey(token);
        if (!subOpt.isPresent()) {
            throw new InvalidEmailKeyException();
        }

        log.info("E-mail token is valid: {}", token);
        String email = subOpt.get().getEmailEndpoint().getIdentity();
        log.info("E-mail user: {}", email);

        List<SubscriptionItem> subs = new ArrayList<>();
        for (_BridgeSubscription sub : subMgr.listForEmail(email)) {
            subs.add(new SubscriptionItem(sub));
        }
        log.info("Found {} subscription(s)", subs.size());

        model.addAttribute("subList", subs);

        return "subscription/list";
    }

    @RequestMapping(value = SubscriptionPortalService.BASE_PATH + "/remove", method = GET)
    public String delete(Model model, @RequestParam String token) {
        log.info("Subscription {} remove request", token);

        Optional<_BridgeSubscription> subOpt = subMgr.getWithEmailKey(token);
        if (!subOpt.isPresent()) {
            throw new InvalidEmailKeyException();
        }

        subOpt.get().terminate();

        return "subscription/deleteSuccess";
    }

}
