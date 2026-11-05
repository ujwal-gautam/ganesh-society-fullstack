package com.ganesh.society;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final TokenService tokens;
 private final AdminUserRepository users;
 private final PasswordEncoder encoder;
 private final JwtService jwt;
 AuthController(TokenService tokens,AdminUserRepository users,PasswordEncoder encoder,JwtService jwt){
  this.tokens=tokens;this.users=users;this.encoder=encoder;this.jwt=jwt;
 }
 record Login(@NotBlank @Email String email,@NotBlank String password){}
 record PasswordChange(@NotBlank String currentPassword,@NotBlank @Size(min=12) String newPassword){}
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Login login){
  AdminUser user=tokens.authenticate(login.email(),login.password());
  Map<String,Object> result=new HashMap<>(tokens.profile(user));
  result.put("accessToken",jwt.issue(user));result.put("tokenType","Bearer");result.put("expiresIn",jwt.expiresIn());
  return result;
 }
 @PostMapping("/logout") public ResponseEntity<Void> logout(){
  // No server-side session or cookie; client must discard its in-memory access token.
  return ResponseEntity.noContent().build();
 }
 @GetMapping("/me") public Map<String,Object> me(Authentication auth){return tokens.profile(users.findByEmail(auth.getName()).orElseThrow());}
 @PostMapping("/change-password") public ResponseEntity<Void> change(@Valid @RequestBody PasswordChange body,Authentication auth){
  AdminUser user=users.findByEmail(auth.getName()).orElseThrow();
  if(!encoder.matches(body.currentPassword(),user.passwordHash))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Incorrect password");
  user.passwordHash=encoder.encode(body.newPassword());users.save(user);
  tokens.audit(user.id,"PASSWORD_CHANGE",user.email);
  return ResponseEntity.noContent().build();
 }
}
