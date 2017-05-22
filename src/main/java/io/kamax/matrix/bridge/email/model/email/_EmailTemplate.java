package io.kamax.matrix.bridge.email.model.email;

import java.util.List;
import java.util.Optional;

public interface _EmailTemplate {

    String getSubject();

    List<_EmailTemplateContent> listContents();

    Optional<_EmailTemplateContent> getContent(String mime);

}
