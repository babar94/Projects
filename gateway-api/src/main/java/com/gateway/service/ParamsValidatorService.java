package com.gateway.service;

public interface ParamsValidatorService {

	boolean validateRequestParams(String request);
    boolean validateRequestParamsSpecialCharacter (String request);
	

}