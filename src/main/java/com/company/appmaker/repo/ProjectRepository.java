package com.company.appmaker.repo;

import com.company.appmaker.model.Project;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends MongoRepository<Project, String> {

    List<Project> findByCompanyNameOrderByCreatedAtDesc(String companyName);

    Optional<Project> findByCompanyNameAndProjectName(String companyName, String projectName);

    boolean existsByCompanyNameAndProjectName(String companyName, String projectName);
}
