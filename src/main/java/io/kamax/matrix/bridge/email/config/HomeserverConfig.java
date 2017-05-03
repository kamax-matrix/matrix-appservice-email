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

package io.kamax.matrix.bridge.email.config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties("homeserver")
public class HomeserverConfig implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(HomeserverConfig.class);

    private String domain;
    private String host;
    private String asToken;
    private String hsToken;
    private String localpart;
    private List<EntityTemplate> users;
    private List<EntityTemplate> ids;
    private List<EntityTemplate> aliases;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAsToken() {
        return asToken;
    }

    public void setAsToken(String asToken) {
        this.asToken = asToken;
    }

    public String getHsToken() {
        return hsToken;
    }

    public void setHsToken(String hsToken) {
        this.hsToken = hsToken;
    }

    public String getLocalpart() {
        return localpart;
    }

    public void setLocalpart(String localpart) {
        this.localpart = localpart;
    }

    public List<EntityTemplate> getUsers() {
        return users;
    }

    public void setUsers(List<EntityTemplate> users) {
        this.users = users;
    }

    public List<EntityTemplate> getIds() {
        return ids;
    }

    public void setIds(List<EntityTemplate> ids) {
        this.ids = ids;
    }

    public List<EntityTemplate> getAliases() {
        return aliases;
    }

    public void setAliases(List<EntityTemplate> aliases) {
        this.aliases = aliases;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (users == null) {
            users = new ArrayList<>();
        }

        if (ids == null) {
            ids = new ArrayList<>();
        }

        if (aliases == null) {
            aliases = new ArrayList<>();
        }

        if (StringUtils.isBlank(host)) {
            host = "https://" + domain;
        }

        log.info("Domain: {}", getDomain());
        log.info("Host: {}", getHost());
        log.info("AS Token: {}", getAsToken());
        log.info("HS Token: {}", getHsToken());
        log.info("Localpart: {}", getLocalpart());
        log.info("Users:");
        for (EntityTemplate p : getUsers()) {
            log.info("- {}", p);
        }
        log.info("Rooms IDs:");
        for (EntityTemplate p : getIds()) {
            log.info("- {}", p);
        }
        log.info("Room Aliases:");
        for (EntityTemplate p : getAliases()) {
            log.info("- {}", p);
        }
    }

}
