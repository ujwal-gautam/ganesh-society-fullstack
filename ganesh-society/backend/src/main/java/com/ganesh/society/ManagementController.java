package com.ganesh.society;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.*;

@RestController
@EnableMethodSecurity
public class ManagementController {
    private final AdminUserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    ManagementController(AdminUserRepository u, RoleRepository r, PermissionRepository p, PasswordEncoder e, TokenService t) {
        users = u;
        roles = r;
        permissions = p;
        encoder = e;
        tokens = t;
    }

    record CreateUser(@NotBlank @Email String email, @NotBlank String fullName,
                      @NotBlank @Size(min = 12) String password, Set<Long> roleIds) {
    }

    record UpdateUser(@NotBlank String fullName, @NotBlank @Email String email) {
    }

    record Status(boolean enabled) {
    }

    record Ids(Set<Long> ids) {
    }

    record RoleInput(@NotBlank String name, String description, boolean active) {
    }

    record ResetPassword(@NotBlank @Size(min = 12) String password) {
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private AdminUser actor(Authentication auth) {
        return users.findByEmail(auth.getName()).orElseThrow();
    }

    private AdminUser user(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Role role(Long id) {
        return roles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Set<Role> resolveRoles(Set<Long> ids) {
        Set<Role> result = new HashSet<>();
        if (ids != null) for (Long id : ids) result.add(role(id));
        return result;
    }

    private boolean isSuper(AdminUser u) {
        return u.enabled && u.roles.stream().anyMatch(r -> r.active && r.name.equals("SUPER_ADMIN"));
    }

    private void protectLast(AdminUser target, boolean willRemainSuper) {
        if (isSuper(target) && !willRemainSuper && users.findAll().stream().filter(this::isSuper).count() <= 1)
            throw bad("Cannot remove last active SUPER_ADMIN");
    }

    private void invalidateTokens(AdminUser u) {
        u.tokenVersion = (u.tokenVersion == null ? 0L : u.tokenVersion) + 1L;
    }

    private void superOnly(Authentication auth) {
        if (!isSuper(actor(auth))) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private Map<String, Object> dto(AdminUser u) {
        Map<String, Object> m = new LinkedHashMap<>(tokens.profile(u));
        m.put("enabled", u.enabled);
        m.put("lastLoginAt", u.lastLoginAt);
        return m;
    }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public List<Map<String, Object>> listUsers() {
        return users.findAll().stream().map(this::dto).toList();
    }

    @GetMapping("/api/admin/users/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public Map<String, Object> getUser(@PathVariable Long id) {
        return dto(user(id));
    }

    @PostMapping("/api/admin/users")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Transactional
    public Map<String, Object> create(@Valid @RequestBody CreateUser body, Authentication auth) {
        superOnly(auth);
        String email = body.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email exists");
        AdminUser u = new AdminUser();
        u.email = email;
        u.fullName = body.fullName();
        u.passwordHash = encoder.encode(body.password());
        u.roles = resolveRoles(body.roleIds());
        if (u.roles.isEmpty()) throw bad("Assign a role");
        if (u.roles.stream().anyMatch(r -> r.active && r.name.equals("SUPER_ADMIN")) && users.findAll().stream().anyMatch(this::isSuper))
            throw bad("Only one active SUPER_ADMIN allowed");
        users.save(u);
        tokens.audit(actor(auth).id, "USER_CREATE", email);
        return dto(u);
    }

    @PutMapping("/api/admin/users/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Transactional
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody UpdateUser body, Authentication auth) {
        superOnly(auth);
        AdminUser u = user(id);
        String email = body.email().trim().toLowerCase(Locale.ROOT);
        users.findByEmail(email).ifPresent(other -> {
            if (!other.id.equals(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email exists");
        });
        if (!Objects.equals(u.email, email)) invalidateTokens(u);
        u.email = email;
        u.fullName = body.fullName();
        tokens.audit(actor(auth).id, "USER_UPDATE", id.toString());
        return dto(u);
    }

    @PatchMapping("/api/admin/users/{id}/status")
    @PreAuthorize("hasAuthority('USER_DISABLE')")
    @Transactional
    public Map<String, Object> status(@PathVariable Long id, @RequestBody Status body, Authentication auth) {
        superOnly(auth);
        AdminUser u = user(id);
        protectLast(u, body.enabled());
        if (u.enabled != body.enabled()) {
            u.enabled = body.enabled();
            invalidateTokens(u);
        }
        tokens.audit(actor(auth).id, "USER_STATUS", id + ":" + body.enabled());
        return dto(u);
    }

    @PutMapping("/api/admin/users/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Transactional
    public Map<String, Object> assign(@PathVariable Long id, @RequestBody Ids body, Authentication auth) {
        superOnly(auth);
        AdminUser u = user(id);
        Set<Role> newRoles = resolveRoles(body.ids());
        if (newRoles.isEmpty()) throw bad("Assign a role");
        protectLast(u, newRoles.stream().anyMatch(r -> r.active && r.name.equals("SUPER_ADMIN")));
        if (newRoles.stream().anyMatch(r -> r.active && r.name.equals("SUPER_ADMIN")) && !isSuper(u) && users.findAll().stream().anyMatch(this::isSuper))
            throw bad("Only one active SUPER_ADMIN allowed");
        u.roles = newRoles;
        invalidateTokens(u);
        tokens.audit(actor(auth).id, "USER_ROLES", id.toString());
        return dto(u);
    }

    @PostMapping("/api/admin/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Transactional
    public ResponseEntity<Void> reset(@PathVariable Long id, @Valid @RequestBody ResetPassword body, Authentication auth) {
        superOnly(auth);
        AdminUser u = user(id);
        u.passwordHash = encoder.encode(body.password());
        invalidateTokens(u);
        tokens.audit(actor(auth).id, "PASSWORD_RESET", id.toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/roles")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public List<Map<String, Object>> listRoles() {
        return roles.findAll().stream().map(this::roleDto).toList();
    }

    private Map<String, Object> roleDto(Role r) {
        return Map.of("id", r.id, "name", r.name, "description", r.description == null ? "" : r.description, "active", r.active, "permissionIds", r.permissions.stream().map(p -> p.id).toList());
    }

    @PostMapping("/api/admin/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Transactional
    public Map<String, Object> createRole(@Valid @RequestBody RoleInput body, Authentication auth) {
        superOnly(auth);
        String name = body.name().trim().toUpperCase(Locale.ROOT);
        if (!name.matches("[A-Z][A-Z0-9_]{1,79}")) throw bad("Invalid role name");
        if (name.equals("SUPER_ADMIN")) throw bad("SUPER_ADMIN is a protected role");
        if (roles.findByName(name).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT);
        Role r = new Role();
        r.name = name;
        r.description = body.description();
        r.active = body.active();
        roles.save(r);
        tokens.audit(actor(auth).id, "ROLE_CREATE", name);
        return roleDto(r);
    }

    @PutMapping("/api/admin/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Transactional
    public Map<String, Object> updateRole(@PathVariable Long id, @Valid @RequestBody RoleInput body, Authentication auth) {
        superOnly(auth);
        Role r = role(id);
        if (r.name.equals("SUPER_ADMIN") && !body.active()) throw bad("Cannot deactivate SUPER_ADMIN role");
        r.description = body.description();
        r.active = body.active();
        tokens.audit(actor(auth).id, "ROLE_UPDATE", r.name);
        return roleDto(r);
    }

    @GetMapping("/api/admin/permissions")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public List<Map<String, Object>> listPermissions() {
        return permissions.findAll().stream().map(p -> Map.<String, Object>of("id", p.id, "code", p.code, "module", p.module)).toList();
    }

    @PutMapping("/api/admin/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Transactional
    public Map<String, Object> permissions(@PathVariable Long id, @RequestBody Ids body, Authentication auth) {
        superOnly(auth);
        Role r = role(id);
        if (r.name.equals("SUPER_ADMIN")) throw bad("SUPER_ADMIN permissions are protected");
        Set<Permission> chosen = new HashSet<>();
        if (body.ids() != null) for (Long pid : body.ids())
            chosen.add(permissions.findById(pid).orElseThrow(() -> bad("Unknown permission")));
        r.permissions = chosen;
        tokens.audit(actor(auth).id, "ROLE_PERMISSIONS", r.name);
        return roleDto(r);
    }
}
