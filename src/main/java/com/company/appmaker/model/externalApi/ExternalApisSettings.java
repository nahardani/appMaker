package com.company.appmaker.model.externalApi;

import java.util.*;

public class ExternalApisSettings {
    private List<ClientConfig> clients = new ArrayList<>();
    public List<ClientConfig> getClients(){ return clients; }
    public void setClients(List<ClientConfig> clients){ this.clients = clients; }

    public static class ClientConfig {
        private String name;          // نام نمایشی: e.g., "PaymentAPI"
        private String baseUrl;       // e.g., https://payments.example.com
        private String authType;      // NONE | BASIC | BEARER | API_KEY
        private String basicUser;
        private String basicPass;
        private String bearerToken;
        // API key
        private String apiKeyName;    // X-API-Key
        private String apiKeyValue;
        private String apiKeyIn;      // HEADER | QUERY
        // عمومی
        private Integer timeoutMs;    // اتصال/خواندن (ساده)
        private Integer rateLimitPerSec;
        private List<HeaderKV> defaultHeaders = new ArrayList<>();
        private List<ClientEndpoint> endpoints = new ArrayList<>();

        // getters/setters...

        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public String getBaseUrl(){return baseUrl;}
        public void setBaseUrl(String baseUrl){this.baseUrl=baseUrl;}
        public String getAuthType(){return authType;}
        public void setAuthType(String authType){this.authType=authType;}
        public String getBasicUser(){return basicUser;}
        public void setBasicUser(String basicUser){this.basicUser=basicUser;}
        public String getBasicPass(){return basicPass;}
        public void setBasicPass(String basicPass){this.basicPass=basicPass;}
        public String getBearerToken(){return bearerToken;}
        public void setBearerToken(String bearerToken){this.bearerToken=bearerToken;}
        public String getApiKeyName(){return apiKeyName;}
        public void setApiKeyName(String apiKeyName){this.apiKeyName=apiKeyName;}
        public String getApiKeyValue(){return apiKeyValue;}
        public void setApiKeyValue(String apiKeyValue){this.apiKeyValue=apiKeyValue;}
        public String getApiKeyIn(){return apiKeyIn;}
        public void setApiKeyIn(String apiKeyIn){this.apiKeyIn=apiKeyIn;}
        public Integer getTimeoutMs(){return timeoutMs;}
        public void setTimeoutMs(Integer timeoutMs){this.timeoutMs=timeoutMs;}
        public Integer getRateLimitPerSec(){return rateLimitPerSec;}
        public void setRateLimitPerSec(Integer rateLimitPerSec){this.rateLimitPerSec=rateLimitPerSec;}
        public List<HeaderKV> getDefaultHeaders(){return defaultHeaders;}
        public void setDefaultHeaders(List<HeaderKV> defaultHeaders){this.defaultHeaders=defaultHeaders;}
        public List<ClientEndpoint> getEndpoints(){return endpoints;}
        public void setEndpoints(List<ClientEndpoint> endpoints){this.endpoints=endpoints;}
    }

    public static class HeaderKV {
        private String key;
        private String value;
        public HeaderKV() {}
        public HeaderKV(String k, String v){this.key=k; this.value=v;}
        public String getKey(){return key;}
        public void setKey(String key){this.key=key;}
        public String getValue(){return value;}
        public void setValue(String value){this.value=value;}
    }

    public static class ClientEndpoint {
        private String method; // GET | POST
        private String path;   // /orders/{id} یا /orders/search
        private List<QParam> queryParams = new ArrayList<>();
        private List<BodyField> bodyFields = new ArrayList<>(); // فقط برای POST

        public String getMethod(){return method;}
        public void setMethod(String method){this.method=method;}
        public String getPath(){return path;}
        public void setPath(String path){this.path=path;}
        public List<QParam> getQueryParams(){return queryParams;}
        public void setQueryParams(List<QParam> queryParams){this.queryParams=queryParams;}
        public List<BodyField> getBodyFields(){return bodyFields;}
        public void setBodyFields(List<BodyField> bodyFields){this.bodyFields=bodyFields;}
    }

    public static class QParam {
        private String name;
        private String example; // نمایشی؛ اجباری نیست
        private Boolean required;
        public QParam(){}
        public QParam(String n, String ex, Boolean r){this.name=n; this.example=ex; this.required=r;}
        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public String getExample(){return example;}
        public void setExample(String example){this.example=example;}
        public Boolean getRequired(){return required;}
        public void setRequired(Boolean required){this.required=required;}
    }

    public static class BodyField {
        private String name;
        private String type;   // String/Long/... (ساده)
        private Boolean required;
        public BodyField(){}
        public BodyField(String n, String t, Boolean r){this.name=n; this.type=t; this.required=r;}
        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public String getType(){return type;}
        public void setType(String type){this.type=type;}
        public Boolean getRequired(){return required;}
        public void setRequired(Boolean required){this.required=required;}
    }
}
