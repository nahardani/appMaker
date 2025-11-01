package com.company.appmaker.repo;


import com.company.appmaker.model.ValueObjectTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ValueObjectTemplateRepo extends MongoRepository<ValueObjectTemplate, String> {
    List<ValueObjectTemplate> findByStatus(String status);

    @Query("{ 'status':'ACTIVE', 'category': ?0 }")
    List<ValueObjectTemplate> findActiveByCategory(String category);

}
