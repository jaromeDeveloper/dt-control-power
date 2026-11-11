package com.controltower.backend.auth;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class DemoUserProvisioner implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemoUserProvisioner.class);

	private final AppUserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final Environment environment;

	DemoUserProvisioner(
			AppUserRepository repository,
			PasswordEncoder passwordEncoder,
			Environment environment) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.environment = environment;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments arguments) {
		provision(
				"admin",
				"Administrador Demo",
				UserRole.ADMIN,
				Set.of("PROD_A", "PROD_B", "PROD_C"),
				"DEMO_ADMIN_PASSWORD");
		provision(
				"operator-a",
				"Operador Producción A",
				UserRole.OPERATOR,
				Set.of("PROD_A"),
				"DEMO_OPERATOR_PASSWORD");
		provision(
				"auditor",
				"Auditor Demo",
				UserRole.AUDITOR,
				Set.of(),
				"DEMO_AUDITOR_PASSWORD");
	}

	private void provision(
			String username,
			String displayName,
			UserRole role,
			Set<String> areaCodes,
			String passwordEnvironmentVariable) {
		if (repository.findByUsernameIgnoreCase(username).isPresent()) {
			return;
		}

		String password = environment.getProperty(passwordEnvironmentVariable);
		if (password == null || password.isBlank()) {
			LOGGER.warn(
					"Demo user '{}' was not provisioned because {} is not configured",
					username,
					passwordEnvironmentVariable);
			return;
		}

		repository.save(AppUser.create(
				username,
				passwordEncoder.encode(password),
				displayName,
				role,
				areaCodes));
		LOGGER.info("Provisioned synthetic demo user '{}' with role {}", username, role);
	}
}
