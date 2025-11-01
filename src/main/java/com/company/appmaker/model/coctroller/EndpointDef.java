package com.company.appmaker.model.coctroller;

import com.company.appmaker.ai.dto.AiArtifact;
import com.company.appmaker.model.FieldDef;
import com.company.appmaker.model.ParamDef;
import com.company.appmaker.model.Project;
import com.company.appmaker.model.ResponsePartDef;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Data
public class EndpointDef {
    private String name;
    private String httpMethod;
    private String path;
    private String requestBodyType;
    private List<ParamDef> params = new ArrayList<>();
    private List<FieldDef> requestFields = new ArrayList<>();
    private String responseType;
    private boolean responseList;
    private String responseModelName;
    private List<FieldDef> responseFields = new ArrayList<>();
    private List<ResponsePartDef> responseParts = new ArrayList<>();
    private List<AiArtifact> aiArtifacts = new ArrayList<>();
    private List<Project.GeneratedFile> aiFiles = new java.util.ArrayList<>();
    private String finalPrompt;  // متن پرامپت ذخیره‌شده برای این اندپوینت
    private String lastAiRaw;    // آخرین خروجی خام AI برای نمایش
    private Instant promptUpdatedAt;
    private Instant rawUpdatedAt;
}
