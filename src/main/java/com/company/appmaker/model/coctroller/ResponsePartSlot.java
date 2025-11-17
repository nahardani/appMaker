package com.company.appmaker.model.coctroller;

import com.company.appmaker.controller.WizardController;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ResponsePartSlot {
    private String name;
    private String container;
    private String kind;
    private String scalarType;
    private String objectName;
    private List<FieldSlot> fields = new ArrayList<>();
}
