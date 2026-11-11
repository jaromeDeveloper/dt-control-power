package com.controltower.backend.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DatabaseUserDetailsService implements UserDetailsService {

	private final AppUserRepository repository;

	DatabaseUserDetailsService(AppUserRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		return repository.findByUsernameIgnoreCase(username.strip())
				.map(AppUserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
	}
}
