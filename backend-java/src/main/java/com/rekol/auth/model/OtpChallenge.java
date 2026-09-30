package com.rekol.auth.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(nullable = false)
  private String phone;
  @Column(nullable = false, length = 10)
  private String code;
  @Column(nullable = false)
  private Instant expiresAt;

  protected OtpChallenge() {}
}
