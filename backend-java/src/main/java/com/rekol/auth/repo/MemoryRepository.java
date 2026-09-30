package com.rekol.auth.repo;
import com.rekol.auth.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MemoryRepository extends JpaRepository<MemoryEntry, UUID> {
  List<MemoryEntry> findByUserOrderByCreatedAtDesc(UserAccount user);
  List<MemoryEntry> findAllByOrderByCreatedAtDesc();
}
