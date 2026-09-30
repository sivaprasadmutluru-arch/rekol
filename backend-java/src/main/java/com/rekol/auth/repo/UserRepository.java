package com.rekol.auth.repo;
import com.rekol.auth.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<UserAccount, UUID> {
  Optional<UserAccount> findByEmailIgnoreCase(String email);
  Optional<UserAccount> findByPhone(String phone);
  long countByRoleIgnoreCase(String role);
}
