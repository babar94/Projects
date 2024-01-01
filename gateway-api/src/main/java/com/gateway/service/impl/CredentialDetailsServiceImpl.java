package com.gateway.service.impl;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entity.Credential;
import com.gateway.repository.CredentialDao;
import com.gateway.request.AuthenticationRequest;
import com.gateway.response.AuthenticationResponse;
import com.gateway.service.AuditLoggingService;
import com.gateway.service.CredentialDetailsService;
import com.gateway.utils.Constants;
import com.gateway.utils.Constants.ResponseDescription;
import com.gateway.utils.JwtTokenUtil;

@Service
public class CredentialDetailsServiceImpl implements CredentialDetailsService {

	private static final Logger LOG = LoggerFactory.getLogger(CredentialDetailsServiceImpl.class);

	@Autowired
	private CredentialDao credentialDao;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private AuditLoggingService auditLoggingService;

	@Value("${security.login.max-attempts}")
	private int maxAttempts;

	@Override
	public AuthenticationResponse authenticatedToken(AuthenticationRequest authenticationRequest) {
		AuthenticationResponse response = new AuthenticationResponse(null, null);
		Credential loginUser = null;
		try {
			LOG.info("CredentialDetailsServiceImpl - Calling Customer Inquiry");
			loginUser = credentialDao.findByUsernameAndChannelNameAndIsEnable(authenticationRequest.getUsername(),
					authenticationRequest.getChannel(), true);
			if (loginUser != null) {
				try {
					// add account locked logic

					if (loginUser.getRemainingCount() <= 0) {
						LOG.info("CredentialDetailsServiceImpl - Error - DisabledException");
						loginUser.setEnable(false);
						credentialDao.save(loginUser);
						response = new AuthenticationResponse(Constants.ResponseCodes.DISABLED_EXCEPTION,
								Constants.ResponseDescription.DISABLED_EXCEPTION);
						return response;
					}

					authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
							authenticationRequest.getUsername(), authenticationRequest.getPassword()));
					loginUser.setRemainingCount(maxAttempts); // Reset retry count on successful login
					credentialDao.save(loginUser);

					LOG.info("CredentialDetailsServiceImpl - Authenticating Customer");
				} catch (DisabledException e) {
					LOG.info("CredentialDetailsServiceImpl - Error - DisabledException");

					response = new AuthenticationResponse(Constants.ResponseCodes.DISABLED_EXCEPTION,
							Constants.ResponseDescription.DISABLED_EXCEPTION);
					return response;
				} catch (BadCredentialsException e) {
					LOG.info("CredentialDetailsServiceImpl - INVALID CREDENTIALS");
					// Invalid credentials logic...

					loginUser.setRemainingCount(loginUser.getRemainingCount() - 1);
					credentialDao.save(loginUser);

					response = new AuthenticationResponse(Constants.ResponseCodes.UNAUTHORISED,
							Constants.ResponseDescription.UNAUTHORISED_WRONG_CREDENTIALS);
					return response;

				}

				final String token = jwtTokenUtil.generateToken(authenticationRequest.getUsername(),
						authenticationRequest.getChannel());
				LOG.info("CredentialDetailsServiceImpl - Token Gen");
				final String expiry = jwtTokenUtil.getTokenExpiry(token);
				response = new AuthenticationResponse(Constants.ResponseCodes.OK, Constants.ResponseDescription.OK,
						expiry, token);

			} else {
				// Invalid credentials logic...

				response = new AuthenticationResponse(Constants.ResponseCodes.UNAUTHORISED,
						Constants.ResponseDescription.UNAUTHORISED_WRONG_CHANNEL);
				return response;
			}
		} catch (Exception e) {
			response = new AuthenticationResponse(Constants.ResponseCodes.UNABLE_TO_PROCESS,
					ResponseDescription.UNABLE_TO_PROCESS);

			return response;
		} finally {

			Date requestDatetime = new Date();
			try {

				ObjectMapper reqMapper = new ObjectMapper();
				String requestAsString = reqMapper.writeValueAsString(authenticationRequest);

				ObjectMapper respMapper = new ObjectMapper();
				String responseAsString = respMapper.writeValueAsString(response);

				auditLoggingService.auditLog("Authentication", response.getResponseCode(),
						response.getResponseDescription(), requestAsString, responseAsString, requestDatetime,
						requestDatetime, null, null, null, authenticationRequest.getChannel(),
						authenticationRequest.getUsername());

			} catch (Exception ex) {
				return response;
			}
		}

		return response;
	}

}
