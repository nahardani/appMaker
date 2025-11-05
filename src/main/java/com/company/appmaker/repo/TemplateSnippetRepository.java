package com.company.appmaker.repo;

import com.company.appmaker.model.TemplateSnippet;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateSnippetRepository extends MongoRepository<TemplateSnippet, String> {
    List<TemplateSnippet> findBySection(String section);

    Optional<TemplateSnippet> findFirstBySectionAndKeyNameAndJavaVersionAndLanguage(
            String section, String keyName, String javaVersion, String language);

    Optional<TemplateSnippet> findFirstBySectionAndKeyNameAndJavaVersionAndLanguageIsNull(
            String section, String key, String javaVersion
    );

}
