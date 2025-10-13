package com.company.appmaker.controller;


import com.company.appmaker.model.DomainProperty;
import com.company.appmaker.repo.DomainPropertyRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/domain-props")
public class DomainPropertyController {

    private final DomainPropertyRepo repo;

    @GetMapping
    public String list(@RequestParam(required=false) String group,
                       @RequestParam(required=false) String status,
                       Model model){
        List<DomainProperty> data = (group!=null && !group.isBlank())
                ? repo.findActiveByGroup(group)
                : repo.findAll();
        model.addAttribute("props", data);
        model.addAttribute("group", group);
        model.addAttribute("status", status);
        return "props/list";
    }



    @GetMapping("/new")
    public String createForm(Model model){
        var p = new DomainProperty();
        p.setStatus("ACTIVE"); // مقدار پیش‌فرض
        model.addAttribute("p", p);
        return "props/form";
    }


    @PostMapping
    public String create(@ModelAttribute("p") DomainProperty p){
        var now = Instant.now();
        if (p.getVersion()==null) p.setVersion(1L);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        repo.save(p);
        return "redirect:/domain-props";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model){
        model.addAttribute("p", repo.findById(id).orElseThrow());
        return "props/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id, @ModelAttribute("p") DomainProperty patch){
        var p = repo.findById(id).orElseThrow();
        p.setGroup(patch.getGroup());
        p.setDisplayName(patch.getDisplayName());
        p.setDataType(patch.getDataType());
        p.setDescription(patch.getDescription());
        p.setSynonyms(patch.getSynonyms());
        p.setRules(patch.getRules());
        p.setEnumValues(patch.getEnumValues());
        p.setExample(patch.getExample());
        p.setStatus(patch.getStatus());
        p.setVersion(p.getVersion()==null?1L:p.getVersion()+1);
        p.setUpdatedAt(Instant.now());
        repo.save(p);
        return "redirect:/domain-props";
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable String id){
        var p = repo.findById(id).orElseThrow();
        p.setStatus("ARCHIVED");
        p.setUpdatedAt(Instant.now());
        repo.save(p);
        return "redirect:/domain-props";
    }
}
