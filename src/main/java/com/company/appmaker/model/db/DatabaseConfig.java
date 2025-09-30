package com.company.appmaker.model.db;

public class DatabaseConfig {

    public enum DbType { POSTGRES, MYSQL, ORACLE, SQLSERVER, MONGODB, H2 }

    private DbType type = DbType.POSTGRES;
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private Integer poolMin;
    private Integer poolMax;

    public DbType getType(){return type;}
    public void setType(DbType type){this.type=type;}
    public String getUrl(){return url;}
    public void setUrl(String url){this.url=url;}
    public String getUsername(){return username;}
    public void setUsername(String username){this.username=username;}
    public String getPassword(){return password;}
    public void setPassword(String password){this.password=password;}
    public String getDriverClassName(){return driverClassName;}
    public void setDriverClassName(String driverClassName){this.driverClassName=driverClassName;}
    public Integer getPoolMin(){return poolMin;}
    public void setPoolMin(Integer poolMin){this.poolMin=poolMin;}
    public Integer getPoolMax(){return poolMax;}
    public void setPoolMax(Integer poolMax){this.poolMax=poolMax;}
}
