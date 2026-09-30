package com.rekol.auth.web;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController public class HealthController {
  @Value("${rekol.dev-otp-bypass:true}") boolean bypass;
  @GetMapping("/health") public Map<String,Object> health(){return Map.of("status","ok","database","postgresql","otp_bypass",bypass);}
}
