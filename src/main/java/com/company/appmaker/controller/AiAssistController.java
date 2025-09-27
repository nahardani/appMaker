package com.company.appmaker.controller;

import com.company.appmaker.ai.OllamaClient;
import com.company.appmaker.ai.PromptFactory;
import com.company.appmaker.ai.dto.AiDtos;
import com.company.appmaker.model.Project;
import com.company.appmaker.repo.ProjectRepository;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/wizard/{id}/ai")
public class AiAssistController {

    private final ProjectRepository repo;
    private final OllamaClient ollama;

    public AiAssistController(ProjectRepository repo, OllamaClient ollama){
        this.repo = repo; this.ollama = ollama;
    }

    private Project load(String id){ return repo.findById(id).orElse(null); }

    @PostMapping(value="/plan", consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AiDtos.GenerateRes plan(@PathVariable String id,
                                   @Valid @RequestBody AiDtos.GenerateReq req){
        var p = load(id); if(p==null) throw new IllegalArgumentException("project not found");

        String pkgBase = inferPackageBase(p); // می‌تونی از company + project بسازی
        String prompt  = PromptFactory.buildControllerPrompt(p.getProjectName(), pkgBase,
                safe(req.controllerName, "ApiController"),
                (StringUtils.hasText(req.basePath)? req.basePath : "/api"),
                safe(req.prompt, ""));

        String raw = ollama.generate(safe(req.model,"llama3"), prompt);
        return parseResponse(raw);
    }

    @PostMapping(value="/generate", consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AiDtos.GenerateRes generate(@PathVariable String id,
                                       @Valid @RequestBody AiDtos.GenerateReq req){
        // فعلاً همان plan — در آینده می‌توان step دوم قوی‌تر کرد
        return plan(id, req);
    }

    @PostMapping(value="/apply", consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String apply(@PathVariable String id,
                        @RequestParam String controllerName,
                        @RequestParam(required=false) String basePath,
                        @RequestParam String payload){
        var p = load(id); if(p==null) return "redirect:/projects";

        // payload همان AiDtos.GenerateRes به‌صورت JSON است
        var res = parseResJson(payload);
        // فایل‌ها را روی دیسک پروژهٔ خروجی یا روی اسکفولد محلی بنویس:
        var created = writeFilesToWorkspace(p, res.files);

        // می‌توانی اینجا Project را هم آپدیت کنی (ثبت کنترلر/اندپوینت‌ها)
        repo.save(p);

        return "redirect:/wizard/" + id + "/controllers?ctrl=" + controllerName;
    }

    /* ===== Utilities ===== */

    private String inferPackageBase(Project p){
        // ساده: com.company.<companyName>.<projectName>
        String c = (p.getCompanyName()==null?"company":p.getCompanyName()).toLowerCase().replaceAll("[^a-z0-9]+",".");
        String n = (p.getProjectName()==null?"app":p.getProjectName()).toLowerCase().replaceAll("[^a-z0-9]+",".");
        return "com." + c + "." + n;
    }

    private String safe(String s, String def){ return (s==null||s.isBlank())? def : s; }

    // پارس خروجی مدل طبق مارکرهای PLAN/FILES
    private AiDtos.GenerateRes parseResponse(String raw){
        AiDtos.GenerateRes out = new AiDtos.GenerateRes();
        if(raw==null) return out;

        String plan = between(raw, "===PLAN===", "===FILES===");
        out.plan = plan != null ? plan.trim() : raw.trim();

        String files = after(raw, "===FILES===");
        if(files != null){
            // هر فایل در یک بلاک: <path>:::<lang>:::<code>
            var lines = files.split("\n");
            StringBuilder buf = new StringBuilder();
            String path=null, lang=null;
            for(String ln : lines){
                if(ln.contains(":::")){
                    // اگر فایل قبلی در جریان بود، ببند
                    if(path!=null){
                        AiDtos.FileItem fi = new AiDtos.FileItem();
                        fi.path = path; fi.lang = lang; fi.content = buf.toString();
                        out.files.add(fi);
                        buf.setLength(0);
                    }
                    String[] parts = ln.split(":::",3);
                    path = trim(parts[0]);
                    lang = parts.length>1 ? trim(parts[1]) : "text";
                    if(parts.length==3) buf.append(parts[2]).append("\n");
                }else{
                    buf.append(ln).append("\n");
                }
            }
            if(path!=null){
                AiDtos.FileItem fi = new AiDtos.FileItem();
                fi.path = path; fi.lang = lang; fi.content = buf.toString();
                out.files.add(fi);
            }
        }
        return out;
    }

    private String trim(String s){ return s==null?null:s.trim(); }
    private String between(String s, String a, String b){
        int i = s.indexOf(a); if(i<0) return null; i+=a.length();
        int j = s.indexOf(b, i); if(j<0) return null;
        return s.substring(i, j);
    }
    private String after(String s, String a){
        int i = s.indexOf(a); if(i<0) return null; i+=a.length();
        return s.substring(i);
    }

    private AiDtos.GenerateRes parseResJson(String json){
        try{
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.readValue(json.getBytes(StandardCharsets.UTF_8), AiDtos.GenerateRes.class);
        }catch(Exception e){
            throw new RuntimeException("invalid AI payload", e);
        }
    }

    private List<String> writeFilesToWorkspace(Project p, List<AiDtos.FileItem> files){
        // TODO: مسیر خروجی واقعی پروژهٔ تولیدی
        // فعلاً در یک فولدر موقت کنار برنامه می‌نویسیم
        List<String> created = new ArrayList<>();
        try{
            java.nio.file.Path root = java.nio.file.Paths.get("generated", p.getId());
            java.nio.file.Files.createDirectories(root);
            for(var f : files){
                if(f.path==null || f.content==null) continue;
                java.nio.file.Path target = root.resolve(f.path).normalize();
                java.nio.file.Files.createDirectories(target.getParent());
                java.nio.file.Files.writeString(target, f.content, StandardCharsets.UTF_8);
                created.add(target.toString());
            }
        }catch(Exception e){
            throw new RuntimeException("writing files failed", e);
        }
        return created;
    }
}
