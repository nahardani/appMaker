package com.company.appmaker.service;

import com.company.appmaker.enums.PromptStatus;
import com.company.appmaker.enums.PromptTarget;
import com.company.appmaker.repo.PromptTemplateRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {
    private final PromptTemplateRepo repo;
    private final PromptRenderer renderer;



    public Optional<PromptTemplate> findPrompt(PromptTarget target, String javaVersion) {
        if (javaVersion == null || javaVersion.isBlank()) {
            javaVersion = "21";
        }
        return repo.findFirstByTargetAndJavaVersionAndStatusOrderByVersionDesc(
                target,
                javaVersion,
                PromptStatus.ACTIVE
        );
    }

    public PromptTemplate getActive(String id) {
        var t = repo.findById(id).orElseThrow();
        if (t.getStatus() != PromptStatus.ACTIVE) throw new IllegalStateException("Prompt is not active");
        return t;
    }

    public String renderBody(String id, Map<String, Object> vars) {
        return safeRender(getActive(id).getBody(), vars);
    }

    /**
     * جست‌وجو + فیلتر نسخه جاوا (انعطاف‌پذیرتر)
     */
    public List<PromptTemplate> listForProject(String projectId, String category,
                                               PromptTarget target, String javaVersion) {
        String cat = (category != null && category.isBlank()) ? null : category;
        var list = repo.searchActive(projectId, cat, target);  // مطمئن شو searchActive در صورت cat=null همه را می‌آورد
        if (javaVersion == null || javaVersion.isBlank()) return list;
        return list.stream().filter(p -> p.getJavaVersion().equals(javaVersion)).toList();
    }

    public PromptTemplate save(PromptTemplate t) {
        return repo.save(t);
    }

    /**
     * رندر “زنجیره‌ای” با جداسازی واضح هر تارگت و هندلینگِ حالتِ بدون تمپلیت
     */
    public String composeForTargets(String projectId,
                                    String javaVersion,
                                    Map<String, Object> vars,
                                    List<PromptTarget> sequence,
                                    String category) {
        StringBuilder out = new StringBuilder();

        int totalSections = 0;

        for (PromptTarget t : sequence) {
            var items = listForProject(projectId, category, t, javaVersion);

            if (items == null || items.isEmpty()) {
                // سکشن خالی را هم مارک می‌کنیم تا بعداً بدانیم چرا خروجی کم بوده
                out.append("\n\n")
                        .append("### TARGET: ").append(t.name()).append(" — NO_TEMPLATES_FOUND").append("\n")
                        .append("<!-- no active templates for this target (project/category/version filters) -->\n");
                log.debug("composeForTargets: no templates for target={} (projectId={}, category={}, javaVersion={})", t, projectId, category, javaVersion);
                continue;
            }

            for (var pt : items) {
                String body = safeRender(pt.getBody(), vars);
                if (body == null || body.isBlank()) {
                    log.debug("composeForTargets: rendered empty body for template id={}, target={}", pt.getId(), t);
                    continue;
                }

                // مارکرهای واضح برای هر سکشن تا مدل چیزی را قورت ندهد
                out.append("\n\n")
                        .append("### BEGIN TARGET ").append(t.name()).append(" :: ").append(pt.getName()).append("\n")

                        .append(body.trim()).append("\n")
                        .append("### END TARGET ").append(t.name()).append(" :: ").append(pt.getName()).append("\n");

                totalSections++;
            }
        }

        String result = out.toString().trim();
        // اگر هیچ سکشنی نبود، رشتهٔ خالی برگردون تا کالر fallback اجرا کنه
        if (totalSections == 0) {
            log.warn("composeForTargets: no sections rendered (will fallback). projectId={}, category={}, javaVersion={}",
                    projectId, category, javaVersion);
            return "";
        }
        log.debug("composeForTargets: rendered {} sections, length={}", totalSections, result.length());
        return result;
    }

    /**
     * اسافولد کامل (در صورت نیاز هنوز شامل تارگت‌های کلاسیک + تارگت‌های سطح-متد)
     */
    public String composeSpringScaffold(String projectId,
                                        String javaVersion,
                                        Map<String, Object> vars,
                                        String category) {
        var seq = List.of(
                PromptTarget.CONTROLLER,
                PromptTarget.SERVICE,
                PromptTarget.REPOSITORY,
                PromptTarget.ENTITY,
                PromptTarget.DTO,
                PromptTarget.TEST,
                // اگر تارگت‌های سطح متد داری، نگه دار
                PromptTarget.CONTROLLER_METHOD,
                PromptTarget.SERVICE_IMPL
        );
        return composeForTargets(projectId, javaVersion, vars, seq, category);
    }

    // --- helpers
    private String safeRender(String tmpl, Map<String, Object> vars) {
        try {
            if (tmpl == null || tmpl.isBlank()) return "";
            return renderer.render(tmpl, vars);
        } catch (Exception ex) {
            log.warn("safeRender failed: {}", ex.getMessage());
            return "";
        }
    }
}
