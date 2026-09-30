package com.rekol.auth.repo;
import com.rekol.auth.model.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface OtpRepository extends JpaRepository<OtpChallenge, UUID> {}
