package com.company.appmaker.model.security;

import java.util.ArrayList;
import java.util.List;

public class SecuritySettings {

    public enum AuthType { NONE, BASIC, BEARER, JWT, OAUTH2 }

    private AuthType authType = AuthType.NONE;

    // BASIC
    private String basicUsername;
    private String basicPassword;

    // BEARER (توکن ثابت)
    private String bearerToken;

    // JWT
    private String jwtSecret;
    private String jwtIssuer;
    private String jwtAudience;
    private Integer jwtExpirationSeconds;

    // OAUTH2
    private String oauth2ClientId;
    private String oauth2ClientSecret;
    private String oauth2Issuer;
    private List<String> oauth2Scopes = new ArrayList<>();

    // نقش‌ها
    private List<Role> roles = new ArrayList<>();

    // قوانین دسترسی (الگوی مسیر/متد/نیازمندی)
    private List<Rule> rules = new ArrayList<>();

    // ===== inner types =====
    public static class Role {
        private String name;
        private String desc;
        public Role() {}
        public Role(String name, String desc){ this.name=name; this.desc=desc; }
        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public String getDesc(){return desc;}
        public void setDesc(String desc){this.desc=desc;}
    }

    public static class Rule {
        private String pathPattern;   // مثل /api/orders/**
        private String httpMethod;    // GET/POST/PUT/...
        private String requirement;   // authenticated | hasRole(ROLE_ADMIN) | permitAll | ...
        public Rule() {}
        public Rule(String pathPattern, String httpMethod, String requirement){
            this.pathPattern=pathPattern; this.httpMethod=httpMethod; this.requirement=requirement;
        }
        public String getPathPattern(){return pathPattern;}
        public void setPathPattern(String pathPattern){this.pathPattern=pathPattern;}
        public String getHttpMethod(){return httpMethod;}
        public void setHttpMethod(String httpMethod){this.httpMethod=httpMethod;}
        public String getRequirement(){return requirement;}
        public void setRequirement(String requirement){this.requirement=requirement;}
    }

    // ===== getters/setters =====
    public AuthType getAuthType(){return authType;}
    public void setAuthType(AuthType authType){this.authType=authType;}

    public String getBasicUsername(){return basicUsername;}
    public void setBasicUsername(String basicUsername){this.basicUsername=basicUsername;}
    public String getBasicPassword(){return basicPassword;}
    public void setBasicPassword(String basicPassword){this.basicPassword=basicPassword;}

    public String getBearerToken(){return bearerToken;}
    public void setBearerToken(String bearerToken){this.bearerToken=bearerToken;}

    public String getJwtSecret(){return jwtSecret;}
    public void setJwtSecret(String jwtSecret){this.jwtSecret=jwtSecret;}
    public String getJwtIssuer(){return jwtIssuer;}
    public void setJwtIssuer(String jwtIssuer){this.jwtIssuer=jwtIssuer;}
    public String getJwtAudience(){return jwtAudience;}
    public void setJwtAudience(String jwtAudience){this.jwtAudience=jwtAudience;}
    public Integer getJwtExpirationSeconds(){return jwtExpirationSeconds;}
    public void setJwtExpirationSeconds(Integer jwtExpirationSeconds){this.jwtExpirationSeconds=jwtExpirationSeconds;}

    public String getOauth2ClientId(){return oauth2ClientId;}
    public void setOauth2ClientId(String oauth2ClientId){this.oauth2ClientId=oauth2ClientId;}
    public String getOauth2ClientSecret(){return oauth2ClientSecret;}
    public void setOauth2ClientSecret(String oauth2ClientSecret){this.oauth2ClientSecret=oauth2ClientSecret;}
    public String getOauth2Issuer(){return oauth2Issuer;}
    public void setOauth2Issuer(String oauth2Issuer){this.oauth2Issuer=oauth2Issuer;}
    public List<String> getOauth2Scopes(){return oauth2Scopes;}
    public void setOauth2Scopes(List<String> oauth2Scopes){this.oauth2Scopes=oauth2Scopes;}

    public List<Role> getRoles(){return roles;}
    public void setRoles(List<Role> roles){this.roles=roles;}

    public List<Rule> getRules(){return rules;}
    public void setRules(List<Rule> rules){this.rules=rules;}
}
