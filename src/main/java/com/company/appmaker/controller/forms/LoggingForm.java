// src/main/java/com/company/appmaker/controller/forms/LoggingForm.java
package com.company.appmaker.controller.forms;

import com.company.appmaker.model.logging.LoggingSettings;
import java.util.*;

public class LoggingForm {
    private Boolean enabled = Boolean.TRUE;
    private String  rootLevel = "INFO";
    private String  pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

    private Boolean consoleEnabled = Boolean.TRUE;

    private Boolean fileEnabled = Boolean.FALSE;
    private String  filePath = "logs/app.log";
    private String  fileMaxSize = "10MB";
    private Integer fileMaxHistory = 7;

    // برای UI ساده: لیست key/value
    private List<LoggerSlot> loggers = new ArrayList<>();

    public static class LoggerSlot {
        private String name;  // com.company.app
        private String level; // TRACE/DEBUG/INFO/WARN/ERROR
        public String getName(){ return name; }
        public void setName(String name){ this.name = name; }
        public String getLevel(){ return level; }
        public void setLevel(String level){ this.level = level; }
    }

    public static LoggingForm from(LoggingSettings s){
        LoggingForm f = new LoggingForm();
        if (s == null) return f;
        f.enabled        = s.getEnabled();
        f.rootLevel      = s.getRootLevel();
        f.pattern        = s.getPattern();
        f.consoleEnabled = s.getConsoleEnabled();
        f.fileEnabled    = s.getFileEnabled();
        f.filePath       = s.getFilePath();
        f.fileMaxSize    = s.getFileMaxSize();
        f.fileMaxHistory = s.getFileMaxHistory();
        if (s.getLoggers()!=null){
            for (var e : s.getLoggers().entrySet()){
                LoggerSlot ls = new LoggerSlot();
                ls.setName(e.getKey());
                ls.setLevel(e.getValue());
                f.loggers.add(ls);
            }
        }
        return f;
    }

    public void applyTo(LoggingSettings t){
        if (t == null) return;
        t.setEnabled(enabled==null? Boolean.TRUE : enabled);
        t.setRootLevel(rootLevel==null? "INFO" : rootLevel);
        t.setPattern(pattern==null? "" : pattern);
        t.setConsoleEnabled(consoleEnabled==null? Boolean.TRUE : consoleEnabled);
        t.setFileEnabled(fileEnabled==null? Boolean.FALSE : fileEnabled);
        t.setFilePath(filePath==null? "logs/app.log" : filePath);
        t.setFileMaxSize(fileMaxSize==null? "10MB" : fileMaxSize);
        t.setFileMaxHistory(fileMaxHistory==null? 7 : fileMaxHistory);

        Map<String,String> map = new LinkedHashMap<>();
        if (loggers!=null){
            for (LoggerSlot ls : loggers){
                if (ls==null || ls.getName()==null || ls.getName().isBlank()) continue;
                String lvl = (ls.getLevel()==null || ls.getLevel().isBlank()) ? "INFO" : ls.getLevel().trim();
                map.put(ls.getName().trim(), lvl);
            }
        }
        t.setLoggers(map);
    }

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
    public List<LoggerSlot> getLoggers(){ return loggers; }
    public void setLoggers(List<LoggerSlot> loggers){ this.loggers = (loggers!=null?loggers:new ArrayList<>()); }
}
