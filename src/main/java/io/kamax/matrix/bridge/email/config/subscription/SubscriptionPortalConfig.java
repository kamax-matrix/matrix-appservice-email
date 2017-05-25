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

package io.kamax.matrix.bridge.email.config.subscription;


import io.kamax.matrix.bridge.email.exception.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@ConfigurationProperties("subscription.portal")
public class SubscriptionPortalConfig implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(SubscriptionPortalConfig.class);

    private URL urlValidated;

    public URL getUrl() {
        return urlValidated;
    }

    public void setUrl(String url) {
        try {
            // Remove any trailing slash
            while (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            urlValidated = new URL(url);
        } catch (MalformedURLException e) {
            throw new InvalidConfigurationException(url + " is not a valid URL for the subscription portal");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (urlValidated == null) {
            throw new InvalidConfigurationException("Subscription Portal URL must be set");
        }

        log.info("Subscription Portal URL: {}", urlValidated.toExternalForm());
    }

}
