package com.company.appmaker.service;

import com.company.appmaker.model.TemplateSnippet;
import com.company.appmaker.repo.TemplateSnippetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service برای TemplateSnippet — شامل متدهای get/delete که کنترلر نیاز دارد.
 * فرض: TemplateSnippetRepository extends org.springframework.data.mongodb.repository.MongoRepository<TemplateSnippet, String>
 * یا JpaRepository<TemplateSnippet, String> (id به صورت String).
 */
@Service
public class TemplateSnippetService {

    private final TemplateSnippetRepository repo;

    public TemplateSnippetService(TemplateSnippetRepository repo) {
        this.repo = repo;
    }

    /**
     * گرفتن یک TemplateSnippet بر اساس id.
     * @param id شناسه (String)
     * @return Optional که یا آبجکت را دارد یا خالی است
     */
    public Optional<TemplateSnippet> get(String id) {
        if (id == null) return Optional.empty();
        return repo.findById(id);
    }

    /**
     * حذف TemplateSnippet بر اساس id.
     * اگر مورد وجود داشته باشد حذف و true برمی‌گرداند، در غیر این صورت false.
     * این متد خطاها را به بیرون پرت نمی‌کند مگر استثنای غیرمنتظره — در آن صورت می‌توانید لاگ بزنید و false برگردانید یا استثنا را دوباره پرتاب کنید.
     *
     * @param id شناسه (String)
     * @return true اگر موجود بود و حذف شد، false اگر وجود نداشت
     */
    public boolean delete(String id) {
        if (id == null) return false;
        boolean exists = repo.existsById(id);
        if (!exists) return false;
        repo.deleteById(id);
        return true;
    }

    /* متدهای مکمل مفید (اختیاری) */

    public List<TemplateSnippet> listAll() {
        return repo.findAll();
    }

    public TemplateSnippet save(TemplateSnippet t) {
        return repo.save(t);
    }

    public boolean exists(String id) {
        if (id == null) return false;
        return repo.existsById(id);
    }
}
