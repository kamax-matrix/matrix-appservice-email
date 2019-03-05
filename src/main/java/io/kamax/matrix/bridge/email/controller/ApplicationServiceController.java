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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.kamax.matrix.MatrixErrorInfo;
import io.kamax.matrix.bridge.email.exception.*;
import io.kamax.matrix.bridge.email.model.matrix.MatrixTransactionPush;
import io.kamax.matrix.bridge.email.model.matrix.RoomQuery;
import io.kamax.matrix.bridge.email.model.matrix.UserQuery;
import io.kamax.matrix.bridge.email.model.matrix._MatrixApplicationService;
import io.kamax.matrix.event._MatrixEvent;
import io.kamax.matrix.json.MatrixJsonEventFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
public class ApplicationServiceController {

    private Logger log = LoggerFactory.getLogger(ApplicationServiceController.class);

    @Autowired
    private _MatrixApplicationService as;

    private JsonParser jsonParser = new JsonParser();

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({InvalidMatrixIdException.class, InvalidBodyContentException.class})
    @ResponseBody
    MatrixErrorInfo handleBadRequest(HttpServletRequest request, MatrixException e) {
        log.error("Error when processing {} {}", request.getMethod(), request.getServletPath(), e);

        return new MatrixErrorInfo(e.getErrorCode());
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NoHomeserverTokenException.class)
    @ResponseBody
    MatrixErrorInfo handleUnauthorized(MatrixException e) {
        return new MatrixErrorInfo(e.getErrorCode());
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(InvalidHomeserverTokenException.class)
    @ResponseBody
    MatrixErrorInfo handleForbidden(MatrixException e) {
        return new MatrixErrorInfo(e.getErrorCode());
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler({RoomNotFoundException.class, UserNotFoundException.class})
    @ResponseBody
    MatrixErrorInfo handleNotFound(MatrixException e) {
        return new MatrixErrorInfo(e.getErrorCode());
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    @ResponseBody
    MatrixErrorInfo handleGeneric(HttpServletRequest request, Throwable t) {
        log.error("Error when processing {} {}", request.getMethod(), request.getServletPath(), t);

        return new MatrixErrorInfo(t);
    }

    @RequestMapping(value = "/rooms/{roomAlias:.+}", method = GET)
    public Object getRoom(
            @RequestParam(name = "access_token", required = false) String accessToken,
            @PathVariable String roomAlias) {
        log.info("Room {} was requested by HS", roomAlias);

        as.queryRoom(new RoomQuery(roomAlias, accessToken));

        return EmptyJsonResponse.get();
    }

    @RequestMapping(value = "/users/{mxId:.+}", method = GET)
    public Object getUser(
            @RequestParam(name = "access_token", required = false) String accessToken,
            @PathVariable String mxId) {
        log.info("User {} was requested by HS", mxId);

        as.queryUser(new UserQuery(as.getId(mxId), accessToken));

        return EmptyJsonResponse.get();
    }

    @RequestMapping(value = "/transactions/{txnId:.+}", method = PUT)
    public Object getTransaction(
            HttpServletRequest request,
            @RequestParam(name = "access_token", required = false) String accessToken,
            @PathVariable String txnId) throws IOException {
        log.info("Processing {}", request.getServletPath());

        String json = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
        try {
            JsonObject rootObj = jsonParser.parse(json).getAsJsonObject();
            JsonArray eventsJson = rootObj.get("events").getAsJsonArray();

            List<_MatrixEvent> events = new ArrayList<>();
            for (JsonElement event : eventsJson) {
                events.add(MatrixJsonEventFactory.get(event.getAsJsonObject()));
            }

            MatrixTransactionPush transaction = new MatrixTransactionPush();
            transaction.setCredentials(accessToken);
            transaction.setId(txnId);
            transaction.setEvents(events);

            as.push(transaction);

            return EmptyJsonResponse.get();
        } catch (IllegalStateException e) {
            throw new InvalidBodyContentException(e);
        }
    }

}
