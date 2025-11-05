spring:
  datasource:
    url: jdbc:db2://{{db2.host:localhost}}:{{db2.port:50000}}/{{db2.name:DBNAME}}
    username: {{db2.user:db2user}}
    password: {{db2.pass:db2pass}}
    driver-class-name: com.ibm.db2.jcc.DB2Driver
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate.dialect: org.hibernate.dialect.DB2Dialect
