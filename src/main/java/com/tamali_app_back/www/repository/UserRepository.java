package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    long countByBusinessId(UUID businessId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT u.business.id, COUNT(u) FROM User u WHERE u.business.id IS NOT NULL GROUP BY u.business.id")
    List<Object[]> countUsersGroupByBusinessId();
}
