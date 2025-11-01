spring:
  application:
    name: {{artifactId}}
  messages:
    basename: {{i18nBase:messages}}
    fallback-to-system-locale: false
  main:
    banner-mode: "console"
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: "health,info"
