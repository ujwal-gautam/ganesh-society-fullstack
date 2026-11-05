package com.ganesh.society;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Component public class AdminBootstrap implements CommandLineRunner {
 private final AdminUserRepository users;private final RoleRepository roles;private final PasswordEncoder encoder;
 @Value("${app.bootstrap-enabled:false}") boolean enabled;
 @Value("${app.bootstrap-email:}") String email;
 @Value("${app.bootstrap-password:}") String password;
 AdminBootstrap(AdminUserRepository u,RoleRepository r,PasswordEncoder e){users=u;roles=r;encoder=e;}
 @Override @Transactional public void run(String... args){if(!enabled)return;if(email.isBlank()||password.length()<12)throw new IllegalStateException("Provisioning requires email and a password of at least 12 characters");String normalized=email.trim().toLowerCase(Locale.ROOT);AdminUser u=users.findByEmail(normalized).orElseGet(()->{AdminUser n=new AdminUser();n.email=normalized;n.passwordHash=encoder.encode(password);n.fullName="Initial administrator";return users.save(n);});Role superRole=roles.findByName("SUPER_ADMIN").orElseThrow();u.roles.add(superRole);users.save(u);}
}
