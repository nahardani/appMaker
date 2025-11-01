package com.company.appmaker.controller;

import com.company.appmaker.model.MicroserviceProfile;
import com.company.appmaker.repo.MicroserviceProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/profiles")
public class MicroserviceProfileController {
    private final MicroserviceProfileRepository repo;

    @GetMapping
    public String list(Model model){
        model.addAttribute("profiles", repo.findAll());
        return "profiles/list";
    }

    @GetMapping("/new")
    public String formNew(Model model){
        model.addAttribute("profile", new MicroserviceProfile());
        return "profiles/form";
    }

    @GetMapping("/{id}/edit")
    public String formEdit(@PathVariable String id, Model model){
        model.addAttribute("profile", repo.findById(id).orElseThrow());
        return "profiles/form";
    }

    @PostMapping
    public String create(@ModelAttribute MicroserviceProfile p){
        repo.save(p);
        return "redirect:/profiles";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id, @ModelAttribute MicroserviceProfile patch){
        var p = repo.findById(id).orElseThrow();
        // کپی فیلدها
        p.setName(patch.getName());
        p.setServiceName(patch.getServiceName());
        p.setBasePackage(patch.getBasePackage());
        p.setBasePath(patch.getBasePath());
        p.setApiVersion(patch.getApiVersion());
        p.setJavaVersion(patch.getJavaVersion());
        p.setUseMongo(patch.isUseMongo());
        p.setUseUlidIds(patch.isUseUlidIds());
        p.setEnableActuator(patch.isEnableActuator());
        p.setEnableOpenApi(patch.isEnableOpenApi());
        p.setEnableValidation(patch.isEnableValidation());
        p.setEnableMetrics(patch.isEnableMetrics());
        p.setEnableSecurityBasic(patch.isEnableSecurityBasic());
        p.setAddDockerfile(patch.isAddDockerfile());
        p.setAddCompose(patch.isAddCompose());
        p.setEnableTestcontainers(patch.isEnableTestcontainers());
        repo.save(p);
        return "redirect:/profiles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id){
        repo.deleteById(id);
        return "redirect:/profiles";
    }
}
