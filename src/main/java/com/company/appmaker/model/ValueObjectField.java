package com.company.appmaker.model;



import lombok.Data;
import java.util.Map;
@Data
public class ValueObjectField {
    private String name;
    private String type;
    private Map<String, Object> constraints;
    private String description;

}
