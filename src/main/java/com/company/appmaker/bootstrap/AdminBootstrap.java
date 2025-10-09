package com.company.appmaker.bootstrap;

import java.util.List;

import com.company.appmaker.model.UserAccount;
import com.company.appmaker.repo.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private final UserAccountRepository repo;
    private final BCryptPasswordEncoder enc;

    public AdminBootstrap(UserAccountRepository repo, BCryptPasswordEncoder enc) {
        this.repo = repo;
        this.enc = enc;
    }

    @Override
    public void run(String... args) {
        // اگر کاربر ادمین وجود ندارد، یکی بساز
        String username = "admin";
        String rawPass  = "admin123";
        if (repo.findByUsername(username).isEmpty()) {
            UserAccount ua = new UserAccount();
            ua.setUsername(username);
            ua.setPasswordHash(enc.encode(rawPass));
            ua.setRoles(List.of("ADMIN"));
            ua.setEnabled(true);
            ua.setDisplayName("Administrator");
            repo.save(ua);
            System.out.println("[BOOTSTRAP] Admin user created: admin / admin123");
        }
    }
}
