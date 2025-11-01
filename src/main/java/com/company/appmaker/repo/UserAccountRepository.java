package com.company.appmaker.repo;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.company.appmaker.model.UserAccount;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {
    Optional<UserAccount> findByUsername(String username);
}
