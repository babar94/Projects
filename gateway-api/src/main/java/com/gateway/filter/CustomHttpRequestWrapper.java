//package com.gateway.filter;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletRequestWrapper;
//
//public class CustomHttpRequestWrapper extends HttpServletRequestWrapper {
//
//	
//	 private final String modifiedUri;
//
//	    public CustomHttpRequestWrapper(HttpServletRequest request, String modifiedUri) {
//	        super(request);
//	        this.modifiedUri = modifiedUri;
//	    }
//
//	    @Override
//	    public String getRequestURI() {
//	        return modifiedUri;
//	    }
//}
