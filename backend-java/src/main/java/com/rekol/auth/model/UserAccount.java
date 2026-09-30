package com.rekol.auth.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_accounts", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_phone", columnList = "phone", unique = true)
})
public class UserAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(nullable = false, unique = true)
  private String email;
  @Column(unique = true)
  private String phone;
  @Column(nullable = false)
  private String name;
  @Column(name = "password_hash")
  private String passwordHash;
  @Column(nullable = false)
  private String role = "USER";

  protected UserAccount() {}

  public UserAccount(String email, String phone, String name, String passwordHash) {
    this.email = email;
    this.phone = phone;
    this.name = name;
    this.passwordHash = passwordHash;
  }

  public UserAccount(String email, String phone, String name, String passwordHash, String role) {
    this(email, phone, name, passwordHash);
    this.role = role == null || role.isBlank() ? "USER" : role.toUpperCase();
  }

  public UUID getId() { return id; }
  public String getEmail() { return email; }
  public String getPhone() { return phone; }
  public String getName() { return name; }
  public String getPasswordHash() { return passwordHash; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role == null || role.isBlank() ? "USER" : role.toUpperCase(); }
}
