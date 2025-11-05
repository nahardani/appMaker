package com.company.appmaker.repo;


import com.company.appmaker.enums.PromptStatus;
import com.company.appmaker.enums.PromptTarget;
import com.company.appmaker.service.PromptTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface PromptTemplateRepo extends MongoRepository<PromptTemplate, String> {

    // فهرست فعال‌ها با فیلترهای اختیاری
    @Query("""
    {
      "status": "ACTIVE",
      "$and": [
        { "$or": [ { "scope": "GLOBAL" }, { "scope": "PROJECT", "projectId": ?0 } ] },
        { "$expr": { "$cond": [ { "$ne": [ ?1, null ] }, { "$eq": [ "$category", ?1 ] }, true ] } },
        { "$expr": { "$cond": [ { "$ne": [ ?2, null ] }, { "$eq": [ "$target",   ?2 ] }, true ] } }
      ]
    }
  """)
    List<PromptTemplate> searchActive(String projectId, String category, PromptTarget target);

    List<PromptTemplate> findByStatusAndCategoryAndTarget(
            PromptStatus status, String category, PromptTarget target);

    Optional<PromptTemplate> findFirstByTargetAndJavaVersionAndStatusOrderByVersionDesc(
            PromptTarget target,
            String javaVersion,
            PromptStatus status
    );
}
