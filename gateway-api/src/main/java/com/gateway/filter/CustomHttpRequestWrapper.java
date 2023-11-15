package com.gateway.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class CustomHttpRequestWrapper extends HttpServletRequestWrapper {

	
	 private final String modifiedUri;

	    public CustomHttpRequestWrapper(HttpServletRequest request, String modifiedUri) {
	        super(request);
	        this.modifiedUri = modifiedUri;
	    }

	    @Override
	    public String getRequestURI() {
	        return modifiedUri;
	    }
}
