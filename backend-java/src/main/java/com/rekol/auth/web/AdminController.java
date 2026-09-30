package com.rekol.auth.web;

import com.rekol.auth.model.MemoryEntry;
import com.rekol.auth.model.UserAccount;
import com.rekol.auth.repo.MemoryRepository;
import com.rekol.auth.repo.UserRepository;
import com.rekol.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

/** Live admin read model. The dashboard must never manufacture rows or metrics. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
  private final AuthService auth;
  private final UserRepository users;
  private final MemoryRepository memories;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AdminController(AuthService auth, UserRepository users, MemoryRepository memories) {
    this.auth = auth; this.users = users; this.memories = memories;
  }

  public record CreateAdmin(@Email String email, @NotBlank String name, @Size(min=8) String password) {}

  @PostMapping("/users/admin")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createAdmin(@RequestHeader(value="Authorization", required=false) String bearer,
                                         @Valid @RequestBody CreateAdmin input) {
    UserAccount requester = auth.require(bearer);
    long adminCount = users.countByRoleIgnoreCase("ADMIN");
    if (adminCount > 0 && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
    }
    if (adminCount == 0 && !"ADMIN".equalsIgnoreCase(requester.getRole())) {
      requester.setRole("ADMIN");
      users.save(requester);
    }
    if (users.findByEmailIgnoreCase(input.email()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email");
    }
    UserAccount created = users.save(new UserAccount(input.email().trim().toLowerCase(), null,
        input.name().trim(), encoder.encode(input.password()), "ADMIN"));
    return auth.userMap(created);
  }

  @GetMapping("/overview")
  public Map<String, Object> overview(@RequestHeader(value="Authorization", required=false) String bearer) {
    auth.require(bearer);
    long totalUsers = users.count();
    long totalMemories = memories.count();
    return Map.of("total_users", totalUsers, "active_users", totalUsers,
        "total_memories", totalMemories, "processed_memories", totalMemories,
        "awaiting_analysis", 0, "storage_bytes", 0);
  }

  @GetMapping("/users")
  public List<Map<String, Object>> listUsers(@RequestHeader(value="Authorization", required=false) String bearer) {
    auth.require(bearer);
    return users.findAll().stream().map(auth::userMap).collect(Collectors.toList());
  }

  @GetMapping("/memories")
  public List<Map<String, Object>> listMemories(@RequestHeader(value="Authorization", required=false) String bearer) {
    auth.require(bearer);
    return memories.findAllByOrderByCreatedAtDesc().stream().map(this::mapMemory).collect(Collectors.toList());
  }

  private Map<String, Object> mapMemory(MemoryEntry memory) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", memory.getId()); result.put("title", memory.getTitle());
    result.put("content", memory.getContent()); result.put("reason", memory.getReason());
    result.put("created_at", memory.getCreatedAt()); result.put("private_memory", memory.isPrivateMemory());
    result.put("owner", memory.getUser().getName()); result.put("owner_email", memory.getUser().getEmail());
    return result;
  }
}
