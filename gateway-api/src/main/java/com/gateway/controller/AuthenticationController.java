package com.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.api.ApiController;
import com.gateway.request.AuthenticationRequest;
import com.gateway.response.AuthenticationResponse;
import com.gateway.service.CredentialDetailsService;

import jakarta.servlet.http.HttpServletRequest;

//@Api(tags = "Authentication Controller")
@RestController
@CrossOrigin
public class AuthenticationController extends ApiController {

	private static final Logger LOG = LoggerFactory.getLogger(AuthenticationController.class);

	@Autowired
	private CredentialDetailsService credentialDetailsService;

	//@ApiOperation(value = "Open Connect - Authenticate", notes = "Authenticate")
	@RequestMapping(value = ApiController.AUTHENTICATE_URL, method = RequestMethod.POST)
	public AuthenticationResponse createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest,
			HttpServletRequest httpRequestData) throws Exception {
			
		return credentialDetailsService.authenticatedToken(authenticationRequest);
	}

}
