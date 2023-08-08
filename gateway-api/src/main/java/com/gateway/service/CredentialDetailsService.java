package com.gateway.service;


import org.springframework.security.core.userdetails.UserDetails;

import com.gateway.request.AuthenticationRequest;
import com.gateway.response.AuthenticationResponse;



public interface CredentialDetailsService {

	public AuthenticationResponse authenticatedToken(AuthenticationRequest authenticationRequest);
	public UserDetails loadUserByUsername(String username);
	
}