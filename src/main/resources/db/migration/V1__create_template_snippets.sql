-- resources/db/migration/V1__create_template_snippets.sql
CREATE TABLE template_snippets (
                                   id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                   section VARCHAR(100) NOT NULL,
                                   key_name VARCHAR(200) NOT NULL,
                                   java_version VARCHAR(10) NOT NULL DEFAULT 'any',
                                   language VARCHAR(10),
                                   content TEXT NOT NULL,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   CONSTRAINT uq_template UNIQUE (section, key_name, java_version, language)
);

CREATE INDEX idx_template_section_version ON template_snippets(section, key_name, java_version, language);
