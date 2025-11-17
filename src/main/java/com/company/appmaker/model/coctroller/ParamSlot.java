package com.company.appmaker.model.coctroller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ParamSlot {
    private String name;
    private String in;
    private String javaType;
    private Boolean required;
}
