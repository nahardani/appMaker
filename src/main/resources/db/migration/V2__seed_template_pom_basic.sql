-- resources/db/migration/V2__seed_template_pom_basic.sql
INSERT INTO template_snippets(section, key_name, java_version, language, content)
VALUES
    ('pom','pom.basic','8', NULL,
     '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\" ...>\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>${groupId}</groupId>\n  <artifactId>${artifactId}</artifactId>\n  <version>0.0.1-SNAPSHOT</version>\n  <properties>\n    <java.version>1.8</java.version>\n  </properties>\n  <!-- ... -->\n</project>'
    ),
    ('pom','pom.basic','11', NULL,
     '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\" ...>\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>${groupId}</groupId>\n  <artifactId>${artifactId}</artifactId>\n  <version>0.0.1-SNAPSHOT</version>\n  <properties>\n    <java.version>11</java.version>\n  </properties>\n</project>'
    ),
    ('pom','pom.basic','17', NULL,
     '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\" ...>\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>${groupId}</groupId>\n  <artifactId>${artifactId}</artifactId>\n  <version>0.0.1-SNAPSHOT</version>\n  <properties>\n    <java.version>17</java.version>\n  </properties>\n</project>'
    ),
    ('pom','pom.basic','21', NULL,
     '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\" ...>\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>${groupId}</groupId>\n  <artifactId>${artifactId}</artifactId>\n  <version>0.0.1-SNAPSHOT</version>\n  <properties>\n    <java.version>21</java.version>\n  </properties>\n</project>'
    ),
    ('pom','pom.basic','any', NULL,
     '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- fallback pom template -->\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\" ...>\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>${groupId}</groupId>\n  <artifactId>${artifactId}</artifactId>\n  <version>0.0.1-SNAPSHOT</version>\n  <properties>\n    <java.version>${java.version}</java.version>\n  </properties>\n</project>'
    );
