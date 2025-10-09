package com.company.appmaker.model.externalApi;

import java.util.Map;

public class ExternalApiConfig {
    private String name;
    private String baseUrl;
    private String authType; // NONE/BASIC/BEARER/APIKEY/...
    private Map<String,String> defaultHeaders;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;

    public String getName(){return name;}
    public void setName(String name){this.name=name;}
    public String getBaseUrl(){return baseUrl;}
    public void setBaseUrl(String baseUrl){this.baseUrl=baseUrl;}
    public String getAuthType(){return authType;}
    public void setAuthType(String authType){this.authType=authType;}
    public Map<String, String> getDefaultHeaders(){return defaultHeaders;}
    public void setDefaultHeaders(Map<String, String> defaultHeaders){this.defaultHeaders=defaultHeaders;}
    public Integer getConnectTimeoutMs(){return connectTimeoutMs;}
    public void setConnectTimeoutMs(Integer connectTimeoutMs){this.connectTimeoutMs=connectTimeoutMs;}
    public Integer getReadTimeoutMs(){return readTimeoutMs;}
    public void setReadTimeoutMs(Integer readTimeoutMs){this.readTimeoutMs=readTimeoutMs;}
}
