// src/main/java/com/company/appmaker/model/LoggingSettings.java
package com.company.appmaker.model.logging;

import java.util.LinkedHashMap;
import java.util.Map;

public class LoggingSettings {
    // فعال/غیرفعال
    private Boolean enabled = Boolean.TRUE;

    // سطح لاگ پیش‌فرض (root) : TRACE/DEBUG/INFO/WARN/ERROR
    private String rootLevel = "INFO";

    // الگوی پیام‌ها (برای Console/File)
    private String pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

    // Console appender
    private Boolean consoleEnabled = Boolean.TRUE;

    // File appender
    private Boolean fileEnabled = Boolean.FALSE;
    private String  filePath = "logs/app.log";
    private String  fileMaxSize = "10MB";   // فقط برای rolling (در صورت نیاز)
    private Integer fileMaxHistory = 7;     // نگه داشتن چند فایل قدیمی

    // loggerهای سفارشی: کلید = نام logger (مثلاً com.company.app), مقدار = level
    private Map<String,String> loggers = new LinkedHashMap<>();

    // getters/setters
    public Boolean getEnabled(){ return enabled; }
    public void setEnabled(Boolean enabled){ this.enabled = enabled; }
    public String getRootLevel(){ return rootLevel; }
    public void setRootLevel(String rootLevel){ this.rootLevel = rootLevel; }
    public String getPattern(){ return pattern; }
    public void setPattern(String pattern){ this.pattern = pattern; }
    public Boolean getConsoleEnabled(){ return consoleEnabled; }
    public void setConsoleEnabled(Boolean consoleEnabled){ this.consoleEnabled = consoleEnabled; }
    public Boolean getFileEnabled(){ return fileEnabled; }
    public void setFileEnabled(Boolean fileEnabled){ this.fileEnabled = fileEnabled; }
    public String getFilePath(){ return filePath; }
    public void setFilePath(String filePath){ this.filePath = filePath; }
    public String getFileMaxSize(){ return fileMaxSize; }
    public void setFileMaxSize(String fileMaxSize){ this.fileMaxSize = fileMaxSize; }
    public Integer getFileMaxHistory(){ return fileMaxHistory; }
    public void setFileMaxHistory(Integer fileMaxHistory){ this.fileMaxHistory = fileMaxHistory; }
    public Map<String, String> getLoggers(){ return loggers; }
    public void setLoggers(Map<String, String> loggers){ this.loggers = loggers; }
}
