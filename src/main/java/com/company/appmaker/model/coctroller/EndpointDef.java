package com.company.appmaker.model.coctroller;

import com.company.appmaker.model.FieldDef;
import com.company.appmaker.model.ParamDef;
import com.company.appmaker.model.ResponsePartDef;

import java.util.ArrayList;
import java.util.List;

public class EndpointDef {
    private String name;
    private String httpMethod;
    private String path;

    // ورودی
    private String requestBodyType;
    private List<ParamDef> params = new ArrayList<>();
    private List<FieldDef> requestFields = new ArrayList<>();

    // (قدیمی) خروجی ساده
    private String responseType;
    private boolean responseList;
    private String responseModelName;
    private List<FieldDef> responseFields = new ArrayList<>();

    // (جدید) خروجی مرکب
    private List<ResponsePartDef> responseParts = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRequestBodyType() {
        return requestBodyType;
    }

    public void setRequestBodyType(String t) {
        this.requestBodyType = t;
    }

    public List<ParamDef> getParams() {
        return params;
    }

    public void setParams(List<ParamDef> params) {
        this.params = params;
    }

    public List<FieldDef> getRequestFields() {
        return requestFields;
    }

    public void setRequestFields(List<FieldDef> requestFields) {
        this.requestFields = requestFields;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public boolean isResponseList() {
        return responseList;
    }

    public void setResponseList(boolean responseList) {
        this.responseList = responseList;
    }

    public String getResponseModelName() {
        return responseModelName;
    }

    public void setResponseModelName(String responseModelName) {
        this.responseModelName = responseModelName;
    }

    public List<FieldDef> getResponseFields() {
        return responseFields;
    }

    public void setResponseFields(List<FieldDef> responseFields) {
        this.responseFields = responseFields;
    }

    public List<ResponsePartDef> getResponseParts() {
        return responseParts;
    }

    public void setResponseParts(List<ResponsePartDef> responseParts) {
        this.responseParts = (responseParts != null ? responseParts : new ArrayList<>());
    }
}
