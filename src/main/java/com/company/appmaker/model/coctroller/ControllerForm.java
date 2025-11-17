package com.company.appmaker.model.coctroller;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ControllerForm {
    @NotBlank
    private String name;
    @NotBlank private String basePath;
    @NotBlank private String type;
    @NotBlank private String httpMethod;
    private String endpointPath;
    private String endpointName;
    private String responseType;
    private Boolean responseList;
    private Boolean editing = false;
    private String originalControllerName;
    private Integer endpointIndex;
    private List<ParamSlot> params = new ArrayList<>();
    private List<FieldSlot> requestFields = new ArrayList<>();
    private Boolean useEndpointPath;
    private String responseContainer;
    private String responseModelKind;
    private String responseScalarType;
    private String responseObjectName;
    private List<FieldSlot> responseFields = new java.util.ArrayList<>();
    private List<ResponsePartSlot> responseParts = new java.util.ArrayList<>();
    private String description;
}
