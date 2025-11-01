package com.company.appmaker.repo;


import com.company.appmaker.model.DomainProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface DomainPropertyRepo extends MongoRepository<DomainProperty, String> {
    List<DomainProperty> findByStatus(String status);

    @Query("{ 'status':'ACTIVE', 'group': ?0 }")
    List<DomainProperty> findActiveByGroup(String group);

}

