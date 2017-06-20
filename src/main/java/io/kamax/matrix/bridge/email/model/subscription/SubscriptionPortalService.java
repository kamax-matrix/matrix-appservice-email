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

package io.kamax.matrix.bridge.email.model.subscription;

import io.kamax.matrix.bridge.email.config.subscription.SubscriptionPortalConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class SubscriptionPortalService {

    public static final String BASE_PATH = "/subscription";
    public static final String TOKEN_PARAMETER = "token";
    private Pattern subPattern = Pattern.compile(BASE_PATH + "\\?" + TOKEN_PARAMETER + "=(?<token>[^&]*)");

    @Autowired
    private SubscriptionPortalConfig portalCfg;

    public String redactToken(String data) {
        return subPattern.matcher(data).replaceAll("");
    }

    public String getPublicLink(String token) {
        return portalCfg.getUrl().toExternalForm() + BASE_PATH + "?token=" + token;
    }

}
