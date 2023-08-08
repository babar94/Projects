package com.gateway.authentication;

import java.io.IOException;
import java.io.Serializable;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gateway.response.AuthenticationResponse;
import com.gateway.utils.Constants;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7858869558953243875L;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {

		String requestURI = request.getRequestURI();
		AuthenticationResponse authenticationResponse = null;

		// Read the request body
		String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		JsonNode requestBodyJson = new ObjectMapper().readTree(requestBody);
		String  transactionAmount = requestBodyJson.has("transaction_amount") ? requestBodyJson.get("transaction_amount").asText() : null;

		// Retrieve the parameters from the JSON object
		
		

		log.info("CredentialDetailsServiceImpl - INVALID CREDENTIALS");

		if (requestBody != null  && transactionAmount != null ) {
			 transactionAmount = requestBodyJson.get("transaction_amount").asText();
			 String reserved = requestBodyJson.get("reserved").asText();
			authenticationResponse = new AuthenticationResponse(Constants.ResponseCodes.UNAUTHORISED, "", reserved);
		}

		else {

			authenticationResponse = new AuthenticationResponse(Constants.ResponseCodes.UNAUTHORISED,
					Constants.ResponseDescription.UNAUTHORISED_WRONG_CREDENTIALS);

		}
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Enable pretty printing

		String responseBody = objectMapper.writeValueAsString(authenticationResponse);
		response.getWriter().println(responseBody);
	}

}
