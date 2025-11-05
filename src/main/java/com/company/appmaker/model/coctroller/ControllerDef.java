package com.company.appmaker.model.coctroller;

import com.company.appmaker.model.Project;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ControllerDef {
    private String name;
    private String basePath;
    private String type;
    private List<String> methods = new ArrayList<>();
    private List<EndpointDef> endpoints = new ArrayList<>();
    private String defaultHttpMethod;
    private List<Project.GeneratedFile> aiFiles = new ArrayList<>();
}
