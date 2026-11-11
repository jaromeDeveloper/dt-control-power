package com.controltower.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.controltower.backend.auth.AuthService.UserSession;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/csrf")
	public CsrfResponse csrf(CsrfToken token) {
		return new CsrfResponse(token.getHeaderName(), token.getToken());
	}

	@PostMapping("/login")
	public UserSession login(
			@Valid @RequestBody LoginRequest credentials,
			HttpServletRequest request,
			HttpServletResponse response) {
		return authService.login(credentials.username(), credentials.password(), request, response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) {
		authService.logout(request, response, authentication);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public UserSession me(Authentication authentication) {
		return authService.currentSession(authentication);
	}

	public record CsrfResponse(String headerName, String token) {
	}

	public record LoginRequest(
			@NotBlank @Size(max = 80) String username,
			@NotBlank @Size(max = 200) String password) {
	}
}
