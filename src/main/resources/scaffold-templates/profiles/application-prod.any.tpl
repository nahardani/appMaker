spring:
  profiles: test
  datasource:
    url: jdbc:h2:mem:{{artifactId}}Test;DB_CLOSE_DELAY=-1
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop

logging:
  level:
    root: WARN
