package com.gateway.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.ToString;

@ToString

@JsonInclude(value = Include.NON_NULL)
public class AuthenticationResponse implements Serializable {

	private static final long serialVersionUID = 8225334744231345997L;
	private String responseCode;
	private String responseDescription;
	private String identificationParameter;
	private String reserveField;
	private String expiry;
	private String token;

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

	public String getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	public String getResponseDescription() {
		return responseDescription;
	}

	public void setResponseDescription(String responseDescription) {
		this.responseDescription = responseDescription;
	}

	public String getIdentificationParameter() {
		return identificationParameter;
	}

	public void setIdentificationParameter(String identificationParameter) {
		this.identificationParameter = identificationParameter;
	}

	public String getReserveField() {
		return reserveField;
	}

	public void setReserveField(String reserveField) {
		this.reserveField = reserveField;
	}

	public String getExpiry() {
		return expiry;
	}

	public void setExpiry(String expiry) {
		this.expiry = expiry;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
