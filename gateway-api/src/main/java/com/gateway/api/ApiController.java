package com.gateway.api;

public abstract class ApiController {

	private static final String API_PATH = "/api/v1/";
//	private static final String ONE_API_PATH = "/api/v1/";
	public static final String AUTHENTICATE_URL = API_PATH + "authenticate";	
	public static final String BILL_URL = API_PATH + "bill";
	//public static final String ONE_BILL_URL = API_PATH ;
//	public static final String ONE_BILL_URL = API_PATH + "bill";
	public static final String CONFIGURATION_URL = API_PATH + "configuration";
	
}
