package com.company.appmaker.repo;

import com.company.appmaker.model.MicroserviceProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MicroserviceProfileRepository extends MongoRepository<MicroserviceProfile, String> {}
