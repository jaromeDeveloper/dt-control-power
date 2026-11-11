package com.controltower.backend.common.error;

public class FileTooLargeException extends RuntimeException {

	public FileTooLargeException() {
		super("The file exceeds the 5 MiB limit");
	}
}
