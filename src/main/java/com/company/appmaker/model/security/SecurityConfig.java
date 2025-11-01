package com.company.appmaker.model.security;

import java.util.ArrayList;
import java.util.List;

public class SecurityConfig {

    public enum AuthType { NONE, BASIC, FORM_LOGIN, JWT, OAUTH2 }

    private AuthType authType = AuthType.BASIC;

    // کاربرهای ثابت (برای BASIC/FORM)
    public static class User {
        private String username;
        private String password; // در UI هش/پلین را تعیین می‌کنیم
        private List<String> roles = new ArrayList<>();
        public String getUsername(){return username;}
        public void setUsername(String username){this.username=username;}
        public String getPassword(){return password;}
        public void setPassword(String password){this.password=password;}
        public List<String> getRoles(){return roles;}
        public void setRoles(List<String> roles){this.roles=roles;}
    }

    // پالیسی مسیرها
    public static class Policy {
        private String pathPattern;            // مثلا /api/orders/**
        private List<String> httpMethods;      // GET/POST/...
        private List<String> requiredRoles;    // ROLE_ADMIN, ...
        public String getPathPattern(){return pathPattern;}
        public void setPathPattern(String pathPattern){this.pathPattern=pathPattern;}
        public List<String> getHttpMethods(){return httpMethods;}
        public void setHttpMethods(List<String> httpMethods){this.httpMethods=httpMethods;}
        public List<String> getRequiredRoles(){return requiredRoles;}
        public void setRequiredRoles(List<String> requiredRoles){this.requiredRoles=requiredRoles;}
    }

    // JWT (اگر انتخاب شود)
    public static class Jwt {
        private String issuer;
        private String secret;
        private Long expirationSeconds;
        public String getIssuer(){return issuer;}
        public void setIssuer(String issuer){this.issuer=issuer;}
        public String getSecret(){return secret;}
        public void setSecret(String secret){this.secret=secret;}
        public Long getExpirationSeconds(){return expirationSeconds;}
        public void setExpirationSeconds(Long expirationSeconds){this.expirationSeconds=expirationSeconds;}
    }

    private List<User> users = new ArrayList<>();
    private List<Policy> policies = new ArrayList<>();
    private Jwt jwt;

    public AuthType getAuthType(){return authType;}
    public void setAuthType(AuthType authType){this.authType=authType;}
    public List<User> getUsers(){return users;}
    public void setUsers(List<User> users){this.users=users;}
    public List<Policy> getPolicies(){return policies;}
    public void setPolicies(List<Policy> policies){this.policies=policies;}
    public Jwt getJwt(){return jwt;}
    public void setJwt(Jwt jwt){this.jwt=jwt;}
}
