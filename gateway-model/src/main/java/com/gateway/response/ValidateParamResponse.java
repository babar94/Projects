package com.gateway.response;

import java.io.Serializable;

public class ValidateParamResponse implements Serializable {

	private static final long serialVersionUID = 8225334744231345997L;
	private  String responseCode;
	private  String description;
	public String getResponseCode() {
		return responseCode;
	}
	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public ValidateParamResponse() {
		super();
		// TODO Auto-generated constructor stub
	}
	public ValidateParamResponse(String responseCode, String description) {
		super();
		this.responseCode = responseCode;
		this.description = description;
	}
	@Override
	public String toString() {
		return "ValidateParamResponse [responseCode=" + responseCode + ", description=" + description + "]";
	}
	

	
	
}
