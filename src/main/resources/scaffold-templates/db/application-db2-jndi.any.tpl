spring:
  datasource:
    jndi-name: {{db2.jndi:java:comp/env/jdbc/DB2}}
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate.dialect: org.hibernate.dialect.DB2Dialect
