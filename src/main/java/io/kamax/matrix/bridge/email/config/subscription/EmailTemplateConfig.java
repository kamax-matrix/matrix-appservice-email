package io.kamax.matrix.bridge.email.config.subscription;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EmailTemplateConfig {

    private String subject;
    private List<EmailTemplateContentConfig> content = new ArrayList<>();

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<EmailTemplateContentConfig> getContent() {
        return content;
    }

    public void setContent(List<EmailTemplateContentConfig> content) {
        this.content = content;
    }

}
