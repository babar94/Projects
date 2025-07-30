package com.gateway.utils;

public class Constants {

	public static class ResponseCodes {

		public static final String OK = "00";
		public static final String UNKNOWN_ERROR = "03";
		public static final String INVALID_DATA = "04";
		public static final String SERVICE_FAIL = "05";
		public static final String BAD_TRANSACTION = "07";
		public static final String DUPLICATE_TRANSACTION = "08";
		public static final String UNABLE_TO_PROCESS = "10";
		public static final String DISABLED_EXCEPTION = "400";
		public static final String UNAUTHORISED = "401";
		public static final String INVALID_VALIDATION = "400";
		public static final String NOT_FOUND = "404";
		public static final String INTERNAL_SERVER_ERROR = "500";

	}

	public static class ResponseDescription {
		public static final String OK = "SUCCESS";
		public static final String UNKNOWN_ERROR = "Unknown Error / Bad Transaction.";
		public static final String INVALID_DATA = "Invalid Data.";
		public static final String SERVICE_FAIL = "Processing/Service Failed";
		public static final String BAD_TRANSACTION = "Unknown Error / Bad Transaction";
		public static final String DUPLICATE_TRANSACTION = "Duplicate Transaction";
		public static final String UNABLE_TO_PROCESS = "Unable to process at this time, please try again later.";
		public static final String AMMOUNT_MISMATCH = "Amount mismatch.";
		public static final String DISABLED_EXCEPTION = "Disabled Exception";
		public static final String UNAUTHORISED_WRONG_CREDENTIALS = "Unauthorized – Wrong Credentials";
		public static final String UNAUTHORISED_WRONG_CHANNEL = "Unauthorized – Wrong Username Or Channel is disabled";
		public static final String INVALID_INPUT_DATA = "Invalid input data";
		public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
		public static final String OPERATION_SUCCESSFULL = "Operation Successful";

		
	}

	

	public static class Key {

		public static final String SECRET_KEY = "dbsecretkey";
		public static final String ALGORITHM = "PBEWithMD5AndDES";

	}

}
