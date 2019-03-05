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

package io.kamax.matrix.bridge.email.config.matrix;

import io.kamax.matrix.bridge.email.exception.InvalidConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties("matrix.home")
public class HomeserverConfig implements InitializingBean {

    private Logger log = LoggerFactory.getLogger(HomeserverConfig.class);

    @Autowired
    private MatrixConfig mxCfg;

    private String host;
    private String asToken;
    private String hsToken;
    private String localpart;
    private List<EntityTemplateConfig> users;

    public String getDomain() {
        return mxCfg.getDomain();
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

    public List<EntityTemplateConfig> getUsers() {
        return users;
    }

    public void setUsers(List<EntityTemplateConfig> users) {
        this.users = users;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isBlank(host)) {
            throw new InvalidConfigurationException("Matrix HS client endpoint must be configured");
        }

        if (StringUtils.isBlank(asToken)) {
            throw new InvalidConfigurationException("Matrix AS token must be configured");
        }

        if (StringUtils.isBlank(hsToken)) {
            throw new InvalidConfigurationException("Matrix HS token must be configured");
        }

        if (StringUtils.isBlank(localpart)) {
            throw new InvalidConfigurationException("Matrix AS localpart must be configured");
        }

        if (users == null || users.isEmpty()) {
            throw new InvalidConfigurationException("At least one Matrix user template must be configured");
        }

        log.info("Domain: {}", getDomain());
        log.info("Host: {}", getHost());
        log.info("AS Token: {}", getAsToken());
        log.info("HS Token: {}", getHsToken());
        log.info("Localpart: {}", getLocalpart());
        log.info("Users:");
        for (EntityTemplateConfig p : getUsers()) {
            log.info("- {}", p);
        }
    }

}
