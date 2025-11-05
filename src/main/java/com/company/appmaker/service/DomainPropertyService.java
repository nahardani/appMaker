package com.company.appmaker.service;


import com.company.appmaker.ai.dto.PropForPrompt;
import com.company.appmaker.repo.DomainPropertyRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DomainPropertyService {

    private final DomainPropertyRepo repo;

    /** فقط ACTIVE ها را برای مصرف در پرامپت برمی‌گرداند. */
    public List<PropForPrompt> listActiveForPrompt() {
        var list = repo.findByStatus("ACTIVE");
        var out = new ArrayList<PropForPrompt>(list.size());
        for (var e : list) {
            if (e == null) continue;
            out.add(map(e));
        }
        // حذف تکراری‌ها بر اساس id (اگر در دیتا تکرار باشد)
        var seen = new HashSet<String>();
        var dedup = new ArrayList<PropForPrompt>();
        for (var p : out) {
            if (p.id() != null && seen.add(p.id())) dedup.add(p);
        }
        return dedup;
    }

    @SuppressWarnings("unchecked")
    private static PropForPrompt map(Object entity){
        String id          = get(entity, "getId",         String.class, null);     // _id
        String group       = get(entity, "getGroup",      String.class, null);
        String dataType    = get(entity, "getDataType",   String.class, null);
        String desc        = get(entity, "getDescription",String.class, null);
        List<String> syns  = get(entity, "getSynonyms",   List.class,   List.of());
        String example     = get(entity, "getExample",    String.class, null);
        Map<String,Object> rules =
                get(entity, "getRules", Map.class, Map.of()); // هر ساختاری داشت برمی‌گردد
        return new PropForPrompt(id, group, dataType, desc, syns, example, rules);
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Object o, String method, Class<T> type, T def){
        try { return (T) o.getClass().getMethod(method).invoke(o); }
        catch (Exception ignore){ return def; }
    }
}
