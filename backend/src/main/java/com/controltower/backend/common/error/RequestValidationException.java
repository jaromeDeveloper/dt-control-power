package com.controltower.backend.common.error;

public class RequestValidationException extends RuntimeException {

	public RequestValidationException(String message) {
		super(message);
	}
}
