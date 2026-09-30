package com.rekol.auth.web;

import com.rekol.auth.model.*;
import com.rekol.auth.repo.MemoryRepository;
import com.rekol.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api/v1/memories")
public class MemoryController {
  private final AuthService auth; private final MemoryRepository memories;
  public MemoryController(AuthService auth, MemoryRepository memories){this.auth=auth;this.memories=memories;}
  public record Create(@NotBlank String content, String reason, String title, boolean privateMemory) {}
  @GetMapping public List<Map<String,Object>> list(@RequestHeader(value="Authorization",required=false) String h){
    UserAccount user=auth.require(h); return memories.findByUserOrderByCreatedAtDesc(user).stream().map(this::map).collect(Collectors.toList());
  }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestHeader(value="Authorization",required=false) String h,@Valid @RequestBody Create p){
    UserAccount user=auth.require(h); String content=p.content().trim(); String title=(p.title()==null||p.title().isBlank())?content.substring(0,Math.min(42,content.length())):p.title().trim(); return map(memories.save(new MemoryEntry(user,content,p.reason(),title,p.privateMemory())));
  }
  private Map<String,Object> map(MemoryEntry m){Map<String,Object> x=new LinkedHashMap<>();x.put("id",m.getId());x.put("content",m.getContent());x.put("reason",m.getReason());x.put("title",m.getTitle());x.put("created_at",m.getCreatedAt().toString());x.put("private_memory",m.isPrivateMemory());return x;}
}
