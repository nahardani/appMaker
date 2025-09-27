package com.company.appmaker.controller.forms;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class SecurityForm {

    @NotNull
    private String authType; // NONE/BASIC/BEARER/JWT/OAUTH2

    // BASIC
    private String basicUsername;
    private String basicPassword;

    // BEARER
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
    private List<String> oauth2Scopes = new ArrayList<>(); // CSV را در کنترلر به لیست تبدیل می‌کنیم اگر از یک input استفاده شود

    // جدول نقش‌ها
    private List<RoleSlot> roles = new ArrayList<>();

    // جدول قوانین
    private List<RuleSlot> rules = new ArrayList<>();

    public static class RoleSlot {
        private String name;
        private String desc;
        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public String getDesc(){return desc;}
        public void setDesc(String desc){this.desc=desc;}
    }

    public static class RuleSlot {
        private String pathPattern;
        private String httpMethod;
        private String requirement;
        public String getPathPattern(){return pathPattern;}
        public void setPathPattern(String pathPattern){this.pathPattern=pathPattern;}
        public String getHttpMethod(){return httpMethod;}
        public void setHttpMethod(String httpMethod){this.httpMethod=httpMethod;}
        public String getRequirement(){return requirement;}
        public void setRequirement(String requirement){this.requirement=requirement;}
    }

    // ===== getters/setters =====
    public String getAuthType(){return authType;}
    public void setAuthType(String authType){this.authType=authType;}

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

    public List<RoleSlot> getRoles(){return roles;}
    public void setRoles(List<RoleSlot> roles){this.roles=roles;}

    public List<RuleSlot> getRules(){return rules;}
    public void setRules(List<RuleSlot> rules){this.rules=rules;}
}
