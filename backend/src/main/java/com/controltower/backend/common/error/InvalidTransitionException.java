package com.controltower.backend.common.error;

public class InvalidTransitionException extends RuntimeException {

	public InvalidTransitionException(String message) {
		super(message);
	}
}
