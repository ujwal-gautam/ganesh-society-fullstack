package com.ganesh.society;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
@Service public class TokenService {
 private final AdminUserRepository users; private final AuditLogRepository audits; private final PasswordEncoder encoder;
 TokenService(AdminUserRepository u,AuditLogRepository a,PasswordEncoder e){users=u;audits=a;encoder=e;}
 @Transactional public AdminUser authenticate(String email,String password){AdminUser u=users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);if(u==null||!u.enabled||!encoder.matches(password,u.passwordHash))throw new BadCredentialsException("Invalid credentials");u.lastLoginAt=Instant.now();audit(u.id,"LOGIN",u.email);return u;}
 Authentication principal(AdminUser u){Set<GrantedAuthority> auth=new HashSet<>();for(Role role:u.roles)if(role.active){auth.add(new SimpleGrantedAuthority("ROLE_"+role.name));for(Permission p:role.permissions)auth.add(new SimpleGrantedAuthority(p.code));}return new UsernamePasswordAuthenticationToken(u.email,null,auth);}
 @Transactional public void audit(Long actor,String action,String target){AuditLog log=new AuditLog();log.actorId=actor;log.action=action;log.target=target;audits.save(log);}
 public Map<String,Object> profile(AdminUser u){return Map.of("id",u.id,"email",u.email,"fullName",u.fullName==null?"":u.fullName,"roles",u.roles.stream().filter(r->r.active).map(r->r.name).sorted().toList(),"permissions",u.roles.stream().filter(r->r.active).flatMap(r->r.permissions.stream()).map(p->p.code).distinct().sorted().toList());}
}
