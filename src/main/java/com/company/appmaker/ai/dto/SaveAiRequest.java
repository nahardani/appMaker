package com.company.appmaker.ai.dto;

import com.company.appmaker.controller.EndpointController;
import lombok.Data;

import java.util.List;
@Data
public class SaveAiRequest {
    public List<Item> files;
}


