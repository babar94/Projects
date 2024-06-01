package com.gateway.service;

import org.apache.commons.lang3.tuple.Pair;

public interface ParamsValidatorService {

	 Pair<Boolean, String> validateRequestParams(String request);
	

}