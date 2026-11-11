package com.controltower.backend.common.error;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.controltower.backend.auth.InvalidCredentialsException;
import com.controltower.backend.observability.RequestCorrelation;

import jakarta.persistence.OptimisticLockException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail handleInvalidCredentials() {
		return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", "Invalid username or password");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		String detail = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.orElse("Request validation failed");
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		return problem(
				HttpStatus.BAD_REQUEST,
				"Invalid request",
				"The parameter '" + exception.getName() + "' has an invalid format");
	}

	@ExceptionHandler(FileValidationException.class)
	ProblemDetail handleFileValidation(FileValidationException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid file", exception.getMessage());
	}

	@ExceptionHandler(RequestValidationException.class)
	ProblemDetail handleRequestValidation(RequestValidationException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableRequest() {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The request body is invalid");
	}

	@ExceptionHandler({FileTooLargeException.class, MaxUploadSizeExceededException.class})
	ProblemDetail handleFileTooLarge() {
		return problem(HttpStatus.PAYLOAD_TOO_LARGE, "File too large", "The file exceeds the 5 MiB limit");
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
	}

	@ExceptionHandler(ForbiddenOperationException.class)
	ProblemDetail handleForbidden(ForbiddenOperationException exception) {
		return problem(HttpStatus.FORBIDDEN, "Operation not allowed", exception.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Operation not allowed", "Access denied");
	}

	@ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
	ProblemDetail handleOptimisticLocking() {
		return problem(
				HttpStatus.CONFLICT,
				"Concurrent update",
				"The record changed while the request was being processed; reload and try again");
	}

	@ExceptionHandler(InvalidTransitionException.class)
	ProblemDetail handleInvalidTransition(InvalidTransitionException exception) {
		return problem(HttpStatus.CONFLICT, "Invalid transition", exception.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataConflict() {
		return problem(HttpStatus.CONFLICT, "Request conflict", "The request conflicts with existing data");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception) {
		LOGGER.error("api.unhandled_error errorType={}", exception.getClass().getSimpleName());
		return problem(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Unexpected error",
				"The request could not be completed");
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setProperty("timestamp", Instant.now());
		problem.setProperty("requestId", RequestCorrelation.currentRequestId());
		return problem;
	}
}
