package com.gateway.exception;

public class BillerAlreadyExistsException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BillerAlreadyExistsException(String message) {
		super(message);
	}
}