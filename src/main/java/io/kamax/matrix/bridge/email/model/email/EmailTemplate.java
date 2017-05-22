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
