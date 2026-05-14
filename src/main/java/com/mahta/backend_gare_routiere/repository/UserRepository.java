package com.mahta.backend_gare_routiere.repository;

import com.mahta.backend_gare_routiere.entity.User;
import com.mahta.backend_gare_routiere.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByRole(UserRole role);

    List<User> findAllByIsActiveTrue();
}