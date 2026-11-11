package com.controltower.backend.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AppUserRepository extends JpaRepository<AppUser, UUID> {

	Optional<AppUser> findByUsernameIgnoreCase(String username);
}
