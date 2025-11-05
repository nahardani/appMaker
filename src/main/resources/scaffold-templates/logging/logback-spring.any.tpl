<configuration scan="true">
  <springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="logs"/>
  <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} : %msg%n"/>

  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>${LOG_PATTERN}</pattern>
    </encoder>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/{{artifactId}}.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>${LOG_PATH}/{{artifactId}}-%d{yyyy-MM-dd}.log</fileNamePattern>
      <maxHistory>14</maxHistory>
    </rollingPolicy>
    <encoder>
      <pattern>${LOG_PATTERN}</pattern>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
  </root>

  <logger name="{{pkgBase}}" level="DEBUG"/>
</configuration>
