package com.controltower.backend.common.error;

public class ForbiddenOperationException extends RuntimeException {

	public ForbiddenOperationException(String message) {
		super(message);
	}
}
