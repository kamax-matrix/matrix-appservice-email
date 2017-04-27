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

package io.kamax.matrix.bridge.email.controller;

import io.kamax.matrix.bridge.email.model.MatrixTransactionPush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
public class ApplicationServiceController {

    private Logger log = LoggerFactory.getLogger(ApplicationServiceController.class);

    @RequestMapping(value = "/rooms/{roomAlias}", method = GET)
    public void getRoom(
            @RequestParam(name = "access_token") String accessToken,
            @PathVariable String roomAlias,
            HttpServletResponse response) {
        log.warn("Room {} was requested by HS but we don't handle any virtual room", roomAlias);

        response.setStatus(HttpStatus.NOT_FOUND.value());
    }

    @RequestMapping(value = "/users/{mxId}", method = GET)
    public void getUser(
            @RequestParam(name = "access_token") String accessToken,
            @PathVariable String mxId,
            HttpServletResponse response) {
        log.info("User {} was requested by HS", mxId);

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @RequestMapping(value = "/transactions/{txnId}", method = PUT)
    public Object getTransaction(
            @RequestParam(name = "access_token") String accessToken,
            @PathVariable String txnId,
            @RequestBody MatrixTransactionPush data) {
        log.info("We got data: {}", data.getEvents());

        return "{}";
    }

}
