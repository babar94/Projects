package com.gateway.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.gateway.entity.Credential;
import com.gateway.repository.CredentialDao;

@Service
public class CustomCredentialsServiceImpl implements UserDetailsService {

	private static final Logger LOG = LoggerFactory.getLogger(CustomCredentialsServiceImpl.class);

	@Autowired
	private CredentialDao credentialDao;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		LOG.info("CustomCredentialsServiceImpl - Finding user in credentials Table - username:", username);
		try {
			Credential user = credentialDao.findByUsername(username);
			if (user == null) {
				LOG.info("CustomCredentialsServiceImpl - User not found with username:", username);
				throw new UsernameNotFoundException("User not found with username: " + username);
			}
			else if (user != null && user.isEnable() == false) {
				LOG.info("Token expired due to account blocked " + username);
				throw new UsernameNotFoundException("Token expired due to account blocked " + username);
			}
			LOG.info("CustomCredentialsServiceImpl - User found with username:", username);
//			return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
//					new ArrayList<>());
			return new CustomCredentialsDetailImpl(user);

		} catch (Exception ex) {
			throw new InternalAuthenticationServiceException(ex.getMessage(), ex);

		}
	}

}
