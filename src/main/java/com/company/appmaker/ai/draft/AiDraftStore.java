package com.company.appmaker.ai.draft;

import com.company.appmaker.ai.dto.CodeFile;
import java.util.List;

public interface AiDraftStore {
    void put(String projectId, List<CodeFile> files);
    List<CodeFile> get(String projectId);
    void clear(String projectId);
}