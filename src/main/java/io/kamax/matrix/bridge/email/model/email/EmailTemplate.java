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

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Scope("prototype")
@Lazy
public class EmailTemplate implements _EmailTemplate {

    private String subject;
    private Map<String, _EmailTemplateContent> contentTemplates;

    public EmailTemplate(String subject, List<_EmailTemplateContent> contentTemplates) {
        this.subject = subject;
        this.contentTemplates = new HashMap<>();
        for (_EmailTemplateContent content : contentTemplates) {
            this.contentTemplates.put(content.getType(), content);
        }
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public List<_EmailTemplateContent> listContents() {
        return new ArrayList<>(contentTemplates.values());
    }

    @Override
    public Optional<_EmailTemplateContent> getContent(String mime) {
        return Optional.ofNullable(contentTemplates.get(mime));
    }

}
