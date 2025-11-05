// com.company.appmaker.service.ValueObjectService.java
package com.company.appmaker.service;


import com.company.appmaker.ai.dto.VOFieldForPrompt;
import com.company.appmaker.ai.dto.ValueObjectForPrompt;
import com.company.appmaker.repo.ValueObjectTemplateRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ValueObjectService {

    private final ValueObjectTemplateRepo repo;

    /** ACTIVE VOs را به فرم سبک برای پرامپت برمی‌گرداند. */
    public List<ValueObjectForPrompt> listActiveForPrompt() {
        var list = repo.findByStatus("ACTIVE");
        var out = new ArrayList<ValueObjectForPrompt>(list.size());
        for (var vo : list) {
            if (vo == null) continue;
            out.add(map(vo));
        }
        // dedup بر اساس name
        var byName = new LinkedHashMap<String, ValueObjectForPrompt>();
        for (var v : out) if (v.name() != null) byName.put(v.name(), v);
        return new ArrayList<>(byName.values());
    }

    @SuppressWarnings("unchecked")
    private static ValueObjectForPrompt map(Object vo){
        String name = get(vo, "getName", String.class, null);
        List<?> rawFields = get(vo, "getFields", List.class, List.of());
        var fields = new ArrayList<VOFieldForPrompt>();
        for (Object f : rawFields){
            String fname = get(f, "getName", String.class, "field");
            String ftype = get(f, "getType", String.class, "String"); // اگر مدل شما getJavaType دارد، همین‌جا عوض کن
            boolean req  = Boolean.TRUE.equals(get(f, "isRequired", Boolean.class, false));
            Map<String,Object> cons = get(f, "getConstraints", Map.class, Map.of());
            fields.add(new VOFieldForPrompt(fname, ftype, req, cons == null ? Map.of() : cons));
        }
        return new ValueObjectForPrompt(name, fields);
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Object o, String method, Class<T> type, T def){
        try { return (T) o.getClass().getMethod(method).invoke(o); }
        catch (Exception ignore){ return def; }
    }
}
