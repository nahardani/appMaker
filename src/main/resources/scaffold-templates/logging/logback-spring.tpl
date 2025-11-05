<configuration>
  <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
  <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="app"/>
  <property name="CONSOLE_LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n"/>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>${CONSOLE_LOG_PATTERN}</pattern>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
  </root>
</configuration>
