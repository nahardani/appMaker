package com.company.appmaker.model.profile;

import java.util.Map;

public class ProfilesConfig {

    public static class EnvProps {
        private Map<String,String> properties; // application-*.properties (کلید/مقدار)
        public Map<String, String> getProperties(){return properties;}
        public void setProperties(Map<String, String> properties){this.properties=properties;}
    }

    private EnvProps dev;
    private EnvProps test;
    private EnvProps prod;

    public EnvProps getDev(){return dev;}
    public void setDev(EnvProps dev){this.dev=dev;}
    public EnvProps getTest(){return test;}
    public void setTest(EnvProps test){this.test=test;}
    public EnvProps getProd(){return prod;}
    public void setProd(EnvProps prod){this.prod=prod;}
}
