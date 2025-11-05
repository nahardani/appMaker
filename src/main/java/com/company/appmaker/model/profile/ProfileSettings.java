package com.company.appmaker.model.profile;

import java.util.ArrayList;
import java.util.List;

public class ProfileSettings {

    // نام پروفایل فعال پیش‌فرض (وقتی پروژه خروجی اجرا شد)
    private String defaultActive = "dev";

    // تنظیمات هر پروفایل
    private EnvProfile dev = new EnvProfile();
    private EnvProfile test = new EnvProfile();
    private EnvProfile prod = new EnvProfile();

    public String getDefaultActive() { return defaultActive; }
    public void setDefaultActive(String defaultActive) { this.defaultActive = defaultActive; }
    public EnvProfile getDev() { return dev; }
    public void setDev(EnvProfile dev) { this.dev = dev; }
    public EnvProfile getTest() { return test; }
    public void setTest(EnvProfile test) { this.test = test; }
    public EnvProfile getProd() { return prod; }
    public void setProd(EnvProfile prod) { this.prod = prod; }

    public static class EnvProfile {
        private Integer serverPort;             // server.port
        private String  loggingLevelRoot;       // logging.level.root (e.g. INFO/DEBUG)
        private List<String> includes = new ArrayList<>(); // profiles.include
        // فضای آزاد برای پراپرتی‌های اختصاصی هر محیط، به‌صورت YAML (بعداً در Scaffolding تزریق می‌کنیم)
        private String extraYaml;

        public Integer getServerPort() { return serverPort; }
        public void setServerPort(Integer serverPort) { this.serverPort = serverPort; }
        public String getLoggingLevelRoot() { return loggingLevelRoot; }
        public void setLoggingLevelRoot(String loggingLevelRoot) { this.loggingLevelRoot = loggingLevelRoot; }
        public List<String> getIncludes() { return includes; }
        public void setIncludes(List<String> includes) { this.includes = includes; }
        public String getExtraYaml() { return extraYaml; }
        public void setExtraYaml(String extraYaml) { this.extraYaml = extraYaml; }
    }
}
