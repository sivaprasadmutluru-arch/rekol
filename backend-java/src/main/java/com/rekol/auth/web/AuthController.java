package com.rekol.auth.web;

import com.rekol.auth.model.UserAccount;
import com.rekol.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService auth; public AuthController(AuthService auth){this.auth=auth;}
  public record Register(@Email String email,@NotBlank String name,@Size(min=6) String password,String phone){}
  public record Login(@Email String email,@NotBlank String password){}
  public record Otp(String phone,@NotBlank String code){}
  @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> register(@Valid @RequestBody Register p){return auth.register(p.email(),p.phone(),p.name(),p.password());}
  @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Login p){return auth.login(p.email(),p.password());}
  @PostMapping("/otp/request") public Map<String,Object> requestOtp(@RequestBody Map<String,String> p){return Map.of("expires_in",300,"dev_bypass",true);}
  @PostMapping("/otp/verify") public Map<String,Object> verify(@Valid @RequestBody Otp p){return auth.verifyOtp(p.phone(),p.code());}
  @PostMapping("/logout") public Map<String,Object> logout(@RequestHeader(value="Authorization",required=false) String h){auth.logout(h);return Map.of("ok",true);}
  @GetMapping("/me") public Map<String,Object> me(@RequestHeader(value="Authorization",required=false) String h){return auth.userMap(auth.require(h));}
}
