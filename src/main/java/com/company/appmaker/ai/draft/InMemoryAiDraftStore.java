// com.company.appmaker.ai.draft.InMemoryAiDraftStore
package com.company.appmaker.ai.draft;

import com.company.appmaker.ai.dto.CodeFile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryAiDraftStore implements AiDraftStore {
    private final java.util.concurrent.ConcurrentMap<String, List<CodeFile>> map = new java.util.concurrent.ConcurrentHashMap<>();
    public void put(String projectId, List<CodeFile> files){ map.put(projectId, files); }
    public List<CodeFile> get(String projectId){ return map.getOrDefault(projectId, List.of()); }
    public void clear(String projectId){ map.remove(projectId); }
}
