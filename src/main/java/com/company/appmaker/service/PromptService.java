package com.company.appmaker.service;

import com.company.appmaker.enums.PromptStatus;
import com.company.appmaker.enums.PromptTarget;
import com.company.appmaker.repo.PromptTemplateRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class PromptService {
    private final PromptTemplateRepo repo;
    private final PromptRenderer renderer;

    public PromptTemplate getActive(String id){
        var t = repo.findById(id).orElseThrow();
        if (t.getStatus()!= PromptStatus.ACTIVE) throw new IllegalStateException("Prompt is not active");
        return t;
    }

    public String renderBody(String id, java.util.Map<String,Object> vars){
        return renderer.render(getActive(id).getBody(), vars);
    }

    public java.util.List<PromptTemplate> listForProject(String projectId, String category,
                                                         PromptTarget target, String javaVersion){
        var list = repo.searchActive(projectId, category, target);
        if (javaVersion==null || javaVersion.isBlank()) return list;
        return list.stream().filter(p -> renderer.javaVersionOk(p, javaVersion)).toList();
    }

    public PromptTemplate save(PromptTemplate t){ return repo.save(t); }
}

