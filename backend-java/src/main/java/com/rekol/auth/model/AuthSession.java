package com.rekol.auth.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
  @Id
  @Column(length = 64)
  private String id;
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;
  @Column(nullable = false)
  private Instant expiresAt;

  protected AuthSession() {}

  public AuthSession(String id, UserAccount user, Instant expiresAt) {
    this.id = id;
    this.user = user;
    this.expiresAt = expiresAt;
  }

  public String getId() { return id; }
  public UserAccount getUser() { return user; }
  public Instant getExpiresAt() { return expiresAt; }
}
