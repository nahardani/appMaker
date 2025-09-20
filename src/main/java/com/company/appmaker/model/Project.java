package com.company.appmaker.model;
import java.time.Instant; import java.util.ArrayList; import java.util.List;
import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection="projects") public class Project {
  @Id private String id; private String projectName; private String companyName; private Instant createdAt; private String javaVersion;
  private java.util.List<String> packages = new ArrayList<>(); private java.util.List<ControllerDef> controllers = new ArrayList<>();
  public Project() {} public Project(String projectName,String companyName){this.projectName=projectName;this.companyName=companyName;this.createdAt=Instant.now();}
  public String getId(){return id;} public void setId(String id){this.id=id;}
  public String getProjectName(){return projectName;} public void setProjectName(String projectName){this.projectName=projectName;}
  public String getCompanyName(){return companyName;} public void setCompanyName(String companyName){this.companyName=companyName;}
  public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
  public String getJavaVersion(){return javaVersion;} public void setJavaVersion(String javaVersion){this.javaVersion=javaVersion;}
  public java.util.List<String> getPackages(){return packages;} public void setPackages(java.util.List<String> packages){this.packages=packages;}
  public java.util.List<ControllerDef> getControllers(){return controllers;} public void setControllers(java.util.List<ControllerDef> controllers){this.controllers=controllers;}
}