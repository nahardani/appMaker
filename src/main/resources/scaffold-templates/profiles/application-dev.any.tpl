spring:
  profiles: dev
  datasource:
    url: jdbc:h2:mem:{{artifactId}};DB_CLOSE_DELAY=-1
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

logging:
  level:
    root: INFO
    {{pkgBase}}: DEBUG
