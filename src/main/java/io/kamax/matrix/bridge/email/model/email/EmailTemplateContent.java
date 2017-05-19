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

package io.kamax.matrix.bridge.email.model.email;

import io.kamax.matrix.bridge.email.config.subscription.EmailTemplateContentConfig;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Scope("prototype")
@Lazy
public class EmailTemplateContent implements InitializingBean, _EmailTemplateContent {

    @Autowired
    private ApplicationContext app;

    private EmailTemplateContentConfig cfg;
    private Resource header;
    private Resource footer;
    private Resource content;

    public EmailTemplateContent(EmailTemplateContentConfig cfg) {
        this.cfg = cfg;
    }

    private Resource get(String path) {
        return app.getResource(path);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        header = get(cfg.getHeader());
        footer = get(cfg.getFooter());
        content = get(cfg.getContent());
    }

    @Override
    public String getType() {
        return cfg.getType();
    }

    @Override
    public String getHeader() throws IOException {
        return IOUtils.toString(header.getInputStream(), StandardCharsets.UTF_8);
    }

    @Override
    public String getFooter() throws IOException {
        return IOUtils.toString(footer.getInputStream(), StandardCharsets.UTF_8);
    }

    @Override
    public String getContent() throws IOException {
        return IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8);
    }

}
