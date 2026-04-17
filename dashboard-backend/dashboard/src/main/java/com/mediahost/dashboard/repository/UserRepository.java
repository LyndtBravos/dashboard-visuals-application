package com.mediahost.dashboard.repository;

import com.mediahost.dashboard.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserId(String userId);
    boolean existsByUserId(String userId);
}