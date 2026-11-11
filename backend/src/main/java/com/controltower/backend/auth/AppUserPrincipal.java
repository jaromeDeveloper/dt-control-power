package com.controltower.backend.auth;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AppUserPrincipal(
		UUID id,
		String username,
		String passwordHash,
		String displayName,
		UserRole role,
		Set<String> areaCodes,
		boolean active) implements UserDetails {

	static AppUserPrincipal from(AppUser user) {
		return new AppUserPrincipal(
				user.getId(),
				user.getUsername(),
				user.getPasswordHash(),
				user.getDisplayName(),
				user.getRole(),
				user.getAreaCodes(),
				user.isActive());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}
}
