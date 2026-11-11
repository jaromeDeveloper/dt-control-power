package com.controltower.backend.common.error;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resourceName) {
		super(resourceName + " was not found");
	}
}
