package com.gateway.exception;

public class BillerNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BillerNotFoundException(String message) {
		super(message);
	}

	public BillerNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
