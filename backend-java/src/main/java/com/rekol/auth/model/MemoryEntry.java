package com.rekol.auth.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_entries", indexes = @Index(name = "idx_memory_user_created", columnList = "user_id,created_at"))
public class MemoryEntry {
  @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
  @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
  @Column(nullable = false, columnDefinition = "TEXT") private String content;
  @Column(columnDefinition = "TEXT") private String reason;
  @Column(nullable = false) private String title;
  @Column(nullable = false) private Instant createdAt;
  @Column(nullable = false) private boolean privateMemory;
  protected MemoryEntry() {}
  public MemoryEntry(UserAccount user, String content, String reason, String title, boolean privateMemory) {
    this.user=user; this.content=content; this.reason=reason; this.title=title; this.privateMemory=privateMemory; this.createdAt=Instant.now();
  }
  public UUID getId(){return id;} public UserAccount getUser(){return user;} public String getContent(){return content;} public String getReason(){return reason;} public String getTitle(){return title;} public Instant getCreatedAt(){return createdAt;} public boolean isPrivateMemory(){return privateMemory;}
}
