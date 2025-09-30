package com.company.appmaker.controller.forms;

import com.company.appmaker.model.profile.ProfileSettings;
import java.util.Arrays;
import java.util.List;

public class ProfilesForm {

    private String defaultActive;

    // فیلدهای dev
    private Integer devServerPort;
    private String  devLoggingLevelRoot;
    private String  devIncludesCsv; // ورودی UI به‌صورت CSV
    private String  devExtraYaml;

    // فیلدهای test
    private Integer testServerPort;
    private String  testLoggingLevelRoot;
    private String  testIncludesCsv;
    private String  testExtraYaml;

    // فیلدهای prod
    private Integer prodServerPort;
    private String  prodLoggingLevelRoot;
    private String  prodIncludesCsv;
    private String  prodExtraYaml;

    // --- getters/setters همه فیلدها ---

    public static ProfilesForm from(ProfileSettings s) {
        ProfilesForm f = new ProfilesForm();
        if (s == null) {
            f.defaultActive = "dev";
            return f;
        }
        f.defaultActive = nz(s.getDefaultActive(), "dev");

        if (s.getDev()!=null){
            f.devServerPort = s.getDev().getServerPort();
            f.devLoggingLevelRoot = s.getDev().getLoggingLevelRoot();
            f.devIncludesCsv = String.join(",", s.getDev().getIncludes() == null ? List.of() : s.getDev().getIncludes());
            f.devExtraYaml = s.getDev().getExtraYaml();
        }
        if (s.getTest()!=null){
            f.testServerPort = s.getTest().getServerPort();
            f.testLoggingLevelRoot = s.getTest().getLoggingLevelRoot();
            f.testIncludesCsv = String.join(",", s.getTest().getIncludes() == null ? List.of() : s.getTest().getIncludes());
            f.testExtraYaml = s.getTest().getExtraYaml();
        }
        if (s.getProd()!=null){
            f.prodServerPort = s.getProd().getServerPort();
            f.prodLoggingLevelRoot = s.getProd().getLoggingLevelRoot();
            f.prodIncludesCsv = String.join(",", s.getProd().getIncludes() == null ? List.of() : s.getProd().getIncludes());
            f.prodExtraYaml = s.getProd().getExtraYaml();
        }
        return f;
    }

    public void applyTo(ProfileSettings s) {
        if (s == null) return;
        s.setDefaultActive(nz(defaultActive, "dev"));

        if (s.getDev()==null) s.setDev(new ProfileSettings.EnvProfile());
        s.getDev().setServerPort(devServerPort);
        s.getDev().setLoggingLevelRoot(z(devLoggingLevelRoot));
        s.getDev().setIncludes(parseCsv(devIncludesCsv));
        s.getDev().setExtraYaml(z(devExtraYaml));

        if (s.getTest()==null) s.setTest(new ProfileSettings.EnvProfile());
        s.getTest().setServerPort(testServerPort);
        s.getTest().setLoggingLevelRoot(z(testLoggingLevelRoot));
        s.getTest().setIncludes(parseCsv(testIncludesCsv));
        s.getTest().setExtraYaml(z(testExtraYaml));

        if (s.getProd()==null) s.setProd(new ProfileSettings.EnvProfile());
        s.getProd().setServerPort(prodServerPort);
        s.getProd().setLoggingLevelRoot(z(prodLoggingLevelRoot));
        s.getProd().setIncludes(parseCsv(prodIncludesCsv));
        s.getProd().setExtraYaml(z(prodExtraYaml));
    }

    private static List<String> parseCsv(String csv){
        if (csv==null || csv.isBlank()) return new java.util.ArrayList<>();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList();
    }
    private static String nz(String v, String def){ return (v==null || v.isBlank()) ? def : v.trim(); }
    private static String z(String v){ return (v==null || v.isBlank()) ? null : v.trim(); }


    public String getDefaultActive() {
        return defaultActive;
    }

    public void setDefaultActive(String defaultActive) {
        this.defaultActive = defaultActive;
    }

    public Integer getDevServerPort() {
        return devServerPort;
    }

    public void setDevServerPort(Integer devServerPort) {
        this.devServerPort = devServerPort;
    }

    public String getDevLoggingLevelRoot() {
        return devLoggingLevelRoot;
    }

    public void setDevLoggingLevelRoot(String devLoggingLevelRoot) {
        this.devLoggingLevelRoot = devLoggingLevelRoot;
    }

    public String getDevIncludesCsv() {
        return devIncludesCsv;
    }

    public void setDevIncludesCsv(String devIncludesCsv) {
        this.devIncludesCsv = devIncludesCsv;
    }

    public String getDevExtraYaml() {
        return devExtraYaml;
    }

    public void setDevExtraYaml(String devExtraYaml) {
        this.devExtraYaml = devExtraYaml;
    }

    public Integer getTestServerPort() {
        return testServerPort;
    }

    public void setTestServerPort(Integer testServerPort) {
        this.testServerPort = testServerPort;
    }

    public String getTestLoggingLevelRoot() {
        return testLoggingLevelRoot;
    }

    public void setTestLoggingLevelRoot(String testLoggingLevelRoot) {
        this.testLoggingLevelRoot = testLoggingLevelRoot;
    }

    public String getTestIncludesCsv() {
        return testIncludesCsv;
    }

    public void setTestIncludesCsv(String testIncludesCsv) {
        this.testIncludesCsv = testIncludesCsv;
    }

    public String getTestExtraYaml() {
        return testExtraYaml;
    }

    public void setTestExtraYaml(String testExtraYaml) {
        this.testExtraYaml = testExtraYaml;
    }

    public Integer getProdServerPort() {
        return prodServerPort;
    }

    public void setProdServerPort(Integer prodServerPort) {
        this.prodServerPort = prodServerPort;
    }

    public String getProdLoggingLevelRoot() {
        return prodLoggingLevelRoot;
    }

    public void setProdLoggingLevelRoot(String prodLoggingLevelRoot) {
        this.prodLoggingLevelRoot = prodLoggingLevelRoot;
    }

    public String getProdIncludesCsv() {
        return prodIncludesCsv;
    }

    public void setProdIncludesCsv(String prodIncludesCsv) {
        this.prodIncludesCsv = prodIncludesCsv;
    }

    public String getProdExtraYaml() {
        return prodExtraYaml;
    }

    public void setProdExtraYaml(String prodExtraYaml) {
        this.prodExtraYaml = prodExtraYaml;
    }
}
