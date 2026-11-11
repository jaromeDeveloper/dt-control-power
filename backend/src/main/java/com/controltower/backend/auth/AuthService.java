package com.controltower.backend.auth;

import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	AuthService(
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	public UserSession login(
			String username,
			String password,
			HttpServletRequest request,
			HttpServletResponse response) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(username.strip(), password));

			HttpSession existingSession = request.getSession(false);
			if (existingSession != null) {
				request.changeSessionId();
			}
			else {
				request.getSession(true);
			}

			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			securityContextRepository.saveContext(context, request, response);

			return toSession(authentication);
		}
		catch (BadCredentialsException | DisabledException exception) {
			throw new InvalidCredentialsException();
		}
	}

	public UserSession currentSession(Authentication authentication) {
		return toSession(authentication);
	}

	public void logout(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) {
		logoutHandler.logout(request, response, authentication);
	}

	private UserSession toSession(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
			throw new InvalidCredentialsException();
		}

		return new UserSession(
				principal.id(),
				principal.username(),
				principal.displayName(),
				principal.role(),
				principal.areaCodes());
	}

	public record UserSession(
			UUID id,
			String username,
			String displayName,
			UserRole role,
			Set<String> areaCodes) {
	}
}
