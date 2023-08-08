package com.gateway.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(value = Include.NON_NULL)
public class AuthenticationResponse implements Serializable {

	private static final long serialVersionUID = 8225334744231345997L;
	private String responseCode;
	private String responseDescription;
	private String identificationParameter;
	private String reserveField;
	private  String expiry;
	private  String token;
	
	public AuthenticationResponse(String responseCode, String responseDescription) {
		super();
		this.responseCode = responseCode;
		this.responseDescription = responseDescription;
		
	}

	public AuthenticationResponse(String responseCode, String responseDescription, String expiry, String token) {
		super();
		this.responseCode = responseCode;
		this.responseDescription = responseDescription;
		this.expiry = expiry;
		this.token = token;
	}

	public AuthenticationResponse() {
		
		
	}

	public AuthenticationResponse(String responseCode, String identificationParameter, String reserveField) {
		super();
		this.responseCode = responseCode;
		this.identificationParameter = identificationParameter;
		this.reserveField = reserveField;
	}
	
	
	
	



	
	
	

}
