package com.rekol.auth.service;

import com.rekol.auth.model.*;
import com.rekol.auth.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
public class AuthService {
  private final UserRepository users; private final SessionRepository sessions; private final OtpRepository otps;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(); private final SecureRandom random = new SecureRandom();
  @Value("${rekol.dev-otp-bypass:true}") private boolean devOtpBypass;
  @Value("${rekol.session-hours:24}") private long sessionHours;
  public AuthService(UserRepository users, SessionRepository sessions, OtpRepository otps){this.users=users;this.sessions=sessions;this.otps=otps;}
  public Map<String,Object> register(String email,String phone,String name,String password){
    if(users.findByEmailIgnoreCase(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"An account already exists for this email");
    UserAccount user=users.save(new UserAccount(email.trim().toLowerCase(),phone,name.trim(),encoder.encode(password))); return response(user);
  }
  public Map<String,Object> login(String email,String password){
    UserAccount user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect"));
    if(!encoder.matches(password,user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect"); return response(user);
  }
  public Map<String,Object> verifyOtp(String phone,String code){
    if(!devOtpBypass || code==null || code.isBlank() || !code.chars().allMatch(Character::isDigit)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid verification code");
    UserAccount user=users.findByPhone(phone).orElseGet(()->users.save(new UserAccount(phone+"@phone.rekol.local",phone,"Rekol user",null))); return response(user);
  }
  public UserAccount require(String bearer){
    if(bearer==null || !bearer.toLowerCase().startsWith("bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
    AuthSession session=sessions.findById(hash(bearer.substring(7).trim())).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session expired"));
    if(session.getExpiresAt().isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Session expired"); return session.getUser();
  }
  public void logout(String bearer){ if(bearer!=null && bearer.toLowerCase().startsWith("bearer ")) sessions.deleteById(hash(bearer.substring(7).trim())); }
  private Map<String,Object> response(UserAccount user){ String token=newToken(); sessions.save(new AuthSession(hash(token),user,Instant.now().plus(sessionHours, java.time.temporal.ChronoUnit.HOURS))); Map<String,Object> result=new LinkedHashMap<>(); result.put("access_token",token); result.put("token_type","bearer"); result.put("expires_in",sessionHours*3600); result.put("user",userMap(user)); return result; }
  public Map<String,Object> userMap(UserAccount user){ Map<String,Object> result=new LinkedHashMap<>(); result.put("id",user.getId()); result.put("email",user.getEmail()); result.put("phone",user.getPhone()); result.put("name",user.getName()); result.put("role",user.getRole()); return result; }
  private String newToken(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
  private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
