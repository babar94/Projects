package com.gateway.utils;

import org.apache.commons.lang3.Validate;

public class ParamsValidator {

	public static boolean IsValid(String... params) {

		try {
			Validate.noNullElements(params);
			
			for (String param : params) {
			    if(param.trim().isEmpty())
			        return false;
            }
			
		} catch (IllegalArgumentException e) {
			return false;
		} catch (Exception e) {
		    return false;
        }
		return true;

	}



}
