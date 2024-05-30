package com.gateway.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.gateway.utils.EncryptionUtils;
import com.gateway.utils.FilterRequestResponseUtils;
import com.gateway.utils.FilterRequestResponseUtils.BufferedRequestWrapper;
import com.gateway.utils.FilterRequestResponseUtils.BufferedResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kong.unirest.json.JSONObject;

@Component
public class ApiLoggingFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(ApiLoggingFilter.class);

	@Autowired
	private FilterRequestResponseUtils filterRequestResponseUtils;


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;

		try {

			BufferedRequestWrapper bufferedRequest = new BufferedRequestWrapper(httpServletRequest);

			BufferedResponseWrapper bufferedResponse = new BufferedResponseWrapper(httpServletResponse);
			String reqBody = bufferedRequest.getRequestBody();

            String encryptedBody = encryptSensitiveData(reqBody);

			logger.info("reqBody : {}", encryptedBody);
			logger.info("method : {}", httpServletRequest.getMethod());
			logger.info("method : {}", httpServletRequest.getHeaderNames().toString());


			final StringBuilder logRequest = new StringBuilder("HTTP ").append(httpServletRequest.getMethod())
					.append(" \"").append(httpServletRequest.getServletPath()).append("\" ").append(", parameters=")
					.append(", body=").append(encryptedBody).append(", remote_address=")
					.append(httpServletRequest.getRemoteAddr());
			logger.info(logRequest.toString());

			try {
				chain.doFilter(bufferedRequest, bufferedResponse);

			} finally {
				StringBuilder logResponse = new StringBuilder("HTTP RESPONSE ").append(bufferedResponse.getContent());
				logger.info(logResponse.toString());

			}
			MDC.clear();

		} catch (Throwable e) {
			logger.error(e.getMessage());
			httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
		}

	}
	
	private String encryptSensitiveData(String data) {
        try {
            // Parse JSON and encrypt sensitive fields
            JSONObject json = new JSONObject(data);
            if (json.has("username")) {
                json.put("username", EncryptionUtils.encrypt(json.getString("username")));
            }
            if (json.has("password")) {
                json.put("password", EncryptionUtils.encrypt(json.getString("password")));
            }
            return json.toString();
        } catch (Exception e) {
            logger.error("Error encrypting data", e);
            return data; // Fallback to original data if encryption fails
        }
    }


	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

}
