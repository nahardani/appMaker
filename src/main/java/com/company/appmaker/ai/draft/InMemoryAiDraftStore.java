// com.company.appmaker.ai.draft.InMemoryAiDraftStore
package com.company.appmaker.ai.draft;

import com.company.appmaker.ai.dto.CodeFile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
class InMemoryAiDraftStore implements AiDraftStore {
    private final Map<String, List<CodeFile>> map = new ConcurrentHashMap<>();

    @Override public void put(String projectId, List<CodeFile> files){
        if (projectId == null || projectId.isBlank() || files == null) return;
        map.put(projectId, List.copyOf(files));
    }

    @Override public List<CodeFile> get(String projectId){
        return projectId == null ? List.of() : map.getOrDefault(projectId, List.of());
    }

    @Override public void clear(String projectId){
        if (projectId != null) map.remove(projectId);
    }
}
