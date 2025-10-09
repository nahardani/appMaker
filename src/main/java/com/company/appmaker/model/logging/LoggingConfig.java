package com.company.appmaker.model.logging;

import java.util.Map;

public class LoggingConfig {
    private String rootLevel;             // INFO/DEBUG/ERROR
    private Map<String,String> packageLevels; // com.company..=DEBUG
    private String pattern;               // PatternLayout

    public String getRootLevel(){return rootLevel;}
    public void setRootLevel(String rootLevel){this.rootLevel=rootLevel;}
    public Map<String, String> getPackageLevels(){return packageLevels;}
    public void setPackageLevels(Map<String, String> packageLevels){this.packageLevels=packageLevels;}
    public String getPattern(){return pattern;}
    public void setPattern(String pattern){this.pattern=pattern;}
}
