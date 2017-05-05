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

import io.kamax.matrix.ThreePid;
import io.kamax.matrix.ThreePidMapping;
import io.kamax.matrix.bridge.email.model.matrix._MatrixApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class MappingController {

    @Autowired
    private _MatrixApplicationService as;

    @RequestMapping(value = "/_matrix/identity/api/v1/lookup", method = GET)
    ResponseEntity lookup(@RequestParam String medium, @RequestParam String address) {
        ThreePid threePid = new ThreePid(medium, address);

        Optional<ThreePidMapping> mapping = as.getMatrixId(threePid);
        if (mapping.isPresent()) {
            return ResponseEntity.ok(ThreePidMappingAnswer.get(mapping.get()));
        } else {
            return ResponseEntity.ok(EmptyJsonResponse.get());
        }
    }

}
