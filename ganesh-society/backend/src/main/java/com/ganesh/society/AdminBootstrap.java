package com.ganesh.society;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final AdminUserRepository users;
    private final PasswordEncoder encoder;
    @Value("${app.admin-email}")
    String email;
    @Value("${app.admin-password}")
    String password;

    public AdminBootstrap(AdminUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public void run(String... args) {
        if (password == null || password.isBlank()) {
            System.err.println("ADMIN_PASSWORD is unset: no initial admin created");
            return;
        }
        if (users.findByEmail(email).isEmpty()) {
            AdminUser u = new AdminUser();
            u.email = email;
            u.passwordHash = encoder.encode(password);
            users.save(u);
        }
    }
}
