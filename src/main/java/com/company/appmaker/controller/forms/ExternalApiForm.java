package com.company.appmaker.controller.forms;

import com.company.appmaker.model.externalApi.ExternalApisSettings;
import java.util.*;

public class ExternalApiForm {
    // انتخاب سرویس
    private String selectedName;

    // متا
    private String name;
    private String baseUrl;
    private String authType;   // NONE/BASIC/BEARER/API_KEY
    private String basicUser;
    private String basicPass;
    private String bearerToken;
    private String apiKeyName;
    private String apiKeyValue;
    private String apiKeyIn;   // HEADER/QUERY
    private Integer timeoutMs;
    private Integer rateLimitPerSec;

    // هدرهای پیش‌فرض
    private List<KV> defaultHeaders = new ArrayList<>();

    // اندپوینت‌ها
    private List<Ep> endpoints = new ArrayList<>();

    // getters/setters...

    public String getSelectedName(){return selectedName;}
    public void setSelectedName(String v){this.selectedName=v;}
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
    public List<KV> getDefaultHeaders(){return defaultHeaders;}
    public void setDefaultHeaders(List<KV> defaultHeaders){this.defaultHeaders=defaultHeaders;}
    public List<Ep> getEndpoints(){return endpoints;}
    public void setEndpoints(List<Ep> endpoints){this.endpoints=endpoints;}

    public static class KV {
        private String key;
        private String value;
        public String getKey(){return key;}
        public void setKey(String key){this.key=key;}
        public String getValue(){return value;}
        public void setValue(String value){this.value=value;}
    }

    public static class Ep {
        private String method; // GET/POST
        private String path;
        private List<Q> queryParams = new ArrayList<>();
        private List<B> bodyFields = new ArrayList<>(); // فقط POST
        public String getMethod(){return method;}
        public void setMethod(String method){this.method=method;}
        public String getPath(){return path;}
        public void setPath(String path){this.path=path;}
        public List<Q> getQueryParams(){return queryParams;}
        public void setQueryParams(List<Q> queryParams){this.queryParams=queryParams;}
        public List<B> getBodyFields(){return bodyFields;}
        public void setBodyFields(List<B> bodyFields){this.bodyFields=bodyFields;}
    }

    public static class Q {
        private String name;
        private String example;
        private Boolean required;
        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public String getExample(){return example;}
        public void setExample(String example){this.example=example;}
        public Boolean getRequired(){return required;}
        public void setRequired(Boolean required){this.required=required;}
    }

    public static class B {
        private String name;
        private String type;
        private Boolean required;
        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public String getType(){return type;}
        public void setType(String type){this.type=type;}
        public Boolean getRequired(){return required;}
        public void setRequired(Boolean required){this.required=required;}
    }

    // Map <-> Model
    public static ExternalApiForm from(ExternalApisSettings settings, String selected){
        ExternalApiForm f = new ExternalApiForm();
        f.selectedName = selected;
        if (settings==null || settings.getClients()==null) return f;
        ExternalApisSettings.ClientConfig c = null;
        if (selected!=null){
            for (var it : settings.getClients()){
                if (it!=null && selected.equals(it.getName())) { c = it; break; }
            }
        }
        if (c == null && !settings.getClients().isEmpty()) c = settings.getClients().get(0);
        if (c == null) return f;

        f.name = c.getName();
        f.baseUrl = c.getBaseUrl();
        f.authType = c.getAuthType();
        f.basicUser = c.getBasicUser();
        f.basicPass = c.getBasicPass();
        f.bearerToken = c.getBearerToken();
        f.apiKeyName = c.getApiKeyName();
        f.apiKeyValue = c.getApiKeyValue();
        f.apiKeyIn = c.getApiKeyIn();
        f.timeoutMs = c.getTimeoutMs();
        f.rateLimitPerSec = c.getRateLimitPerSec();

        if (c.getDefaultHeaders()!=null){
            for (var h : c.getDefaultHeaders()){
                KV kv = new KV(); kv.setKey(h.getKey()); kv.setValue(h.getValue());
                f.defaultHeaders.add(kv);
            }
        }
        if (c.getEndpoints()!=null){
            for (var e : c.getEndpoints()){
                Ep ep = new Ep();
                ep.setMethod(e.getMethod());
                ep.setPath(e.getPath());
                if (e.getQueryParams()!=null){
                    for (var q : e.getQueryParams()){
                        Q qf = new Q(); qf.setName(q.getName()); qf.setExample(q.getExample()); qf.setRequired(q.getRequired());
                        ep.getQueryParams().add(qf);
                    }
                }
                if (e.getBodyFields()!=null){
                    for (var b : e.getBodyFields()){
                        B bf = new B(); bf.setName(b.getName()); bf.setType(b.getType()); bf.setRequired(b.getRequired());
                        ep.getBodyFields().add(bf);
                    }
                }
                f.getEndpoints().add(ep);
            }
        }
        return f;
    }

    public ExternalApisSettings.ClientConfig toModel(){
        var c = new ExternalApisSettings.ClientConfig();
        c.setName(name);
        c.setBaseUrl(baseUrl);
        c.setAuthType(authType);
        c.setBasicUser(basicUser);
        c.setBasicPass(basicPass);
        c.setBearerToken(bearerToken);
        c.setApiKeyName(apiKeyName);
        c.setApiKeyValue(apiKeyValue);
        c.setApiKeyIn(apiKeyIn);
        c.setTimeoutMs(timeoutMs);
        c.setRateLimitPerSec(rateLimitPerSec);

        var headers = new ArrayList<ExternalApisSettings.HeaderKV>();
        if (defaultHeaders != null){
            for (var kv : defaultHeaders){
                if (kv==null || kv.getKey()==null || kv.getKey().isBlank()) continue;
                headers.add(new ExternalApisSettings.HeaderKV(kv.getKey().trim(), kv.getValue()));
            }
        }
        c.setDefaultHeaders(headers);

        var eps = new ArrayList<ExternalApisSettings.ClientEndpoint>();
        if (endpoints != null){
            for (var e : endpoints){
                if (e==null || e.getMethod()==null || e.getMethod().isBlank()) continue;
                var m = new ExternalApisSettings.ClientEndpoint();
                m.setMethod(e.getMethod().trim().toUpperCase());
                m.setPath(e.getPath());
                var qs = new ArrayList<ExternalApisSettings.QParam>();
                if (e.getQueryParams()!=null){
                    for (var q : e.getQueryParams()){
                        if (q==null || q.getName()==null || q.getName().isBlank()) continue;
                        qs.add(new ExternalApisSettings.QParam(q.getName().trim(), q.getExample(), Boolean.TRUE.equals(q.getRequired())));
                    }
                }
                m.setQueryParams(qs);
                var bs = new ArrayList<ExternalApisSettings.BodyField>();
                if ("POST".equalsIgnoreCase(m.getMethod()) && e.getBodyFields()!=null){
                    for (var b : e.getBodyFields()){
                        if (b==null || b.getName()==null || b.getName().isBlank()) continue;
                        bs.add(new ExternalApisSettings.BodyField(b.getName().trim(),
                                (b.getType()==null||b.getType().isBlank())?"String":b.getType().trim(),
                                Boolean.TRUE.equals(b.getRequired())));
                    }
                }
                m.setBodyFields(bs);
                eps.add(m);
            }
        }
        c.setEndpoints(eps);
        return c;
    }
}
