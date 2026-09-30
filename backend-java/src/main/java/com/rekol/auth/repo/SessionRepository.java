package com.rekol.auth.repo;
import com.rekol.auth.model.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SessionRepository extends JpaRepository<AuthSession, String> {}
