package com.gateway.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gateway.entity.Credential;
import com.gateway.repository.CredentialDao;
import com.gateway.request.AuthenticationRequest;
import com.gateway.request.ChannelConfigurationRequest;
import com.gateway.request.ConfigurationRequest;
import com.gateway.response.CredentialsListAttribute;
import com.gateway.response.GenericResponse;
import com.gateway.service.ConfigurationManagementService;

@Service
public class ConfigurationManagementServiceImpl implements ConfigurationManagementService {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManagementServiceImpl.class);

	@Autowired
	private CredentialDao credentialDao;

	@Autowired
	private PasswordEncoder bcryptEncoder;

	@Value("${security.login.max-attempts}")
	private int maxAttempts;

	@Override
	public GenericResponse<String> saveCredential(AuthenticationRequest request) {
		GenericResponse<String> response = new GenericResponse<>();
		Credential checkUserExistance = null;
		try {
			LOG.info("CredentialDetailsServiceImpl - Calling Save New Credential");
			checkUserExistance = (Credential) credentialDao.findByUsernameAndChannelName(request.getUsername(),
					request.getChannel());
			if (checkUserExistance != null) {
				response = new GenericResponse<>("01", "User Already Exist!");
			} else {
				Credential newUser = new Credential();
				newUser.setUsername(request.getUsername());
				newUser.setPassword(bcryptEncoder.encode(request.getPassword()));
				newUser.setChannelName(request.getChannel());
				newUser.setEnable(true);
				newUser.setRemainingCount(maxAttempts);
				newUser.setDescription(request.getChannel() + " Channel Created");

				credentialDao.save(newUser);
				response = new GenericResponse<>("00", "User created Succesfully!");
			}
		} catch (Exception ex) {
			LOG.info("Save Credential Error :"+ex);
		}

		return response;
	}

	@Override
	public GenericResponse<String> disableCredential(ConfigurationRequest request) {
		GenericResponse<String> response = new GenericResponse<>();
		Credential checkUserExistance = null;
		try {
			LOG.info("CredentialDetailsServiceImpl - Calling Disable any Channel");
			checkUserExistance = (Credential) credentialDao.findByUsernameAndChannelName(request.getUsername(),
					request.getChannel());
			if (checkUserExistance != null) {
				checkUserExistance.setEnable(false);
				credentialDao.save(checkUserExistance);
				response = new GenericResponse<>("00", "User Disabled Successfully!");
			} else {
				response = new GenericResponse<>("01", "User does not exist against the channel!");
			}
		} catch (Exception ex) {
			LOG.info("CredentialDetailsServiceImpl - disableCredential :"+ex);

		}

		return response;
	}

	@Override
	public GenericResponse<String> enableCredential(ConfigurationRequest request) {
		GenericResponse<String> response = new GenericResponse<>();
		Credential checkUserExistance = null;
		try {
			LOG.info("CredentialDetailsServiceImpl - Calling Disable any Channel");
			checkUserExistance = (Credential) credentialDao.findByUsernameAndChannelName(request.getUsername(),
					request.getChannel());
			if (checkUserExistance != null) {
				checkUserExistance.setEnable(true);
				checkUserExistance.setRemainingCount(maxAttempts);
				checkUserExistance.setDescription(request.getChannel() +" Channel Created");
				credentialDao.save(checkUserExistance);
				response = new GenericResponse<>("00", "User Enabled Successfully!");
			} else {
				response = new GenericResponse<>("01", "User does not exist against the channel!");
			}
		} catch (Exception ex) {
			LOG.info("CredentialDetailsServiceImpl - enableCredential :"+ex);

		}

		return response;
	}

	@Override
	public GenericResponse<String> updateCredentialPassword(AuthenticationRequest request) {
		GenericResponse<String> response = new GenericResponse<>();
		Credential checkUserExistance = null;
		try {
			LOG.info("CredentialDetailsServiceImpl - Calling Save New Credential");
			checkUserExistance = (Credential) credentialDao.findByUsernameAndChannelName(request.getUsername(),
					request.getChannel());
			if (checkUserExistance != null) {
				checkUserExistance.setPassword(bcryptEncoder.encode(request.getPassword()));
				checkUserExistance.setRemainingCount(maxAttempts);
				checkUserExistance.setDescription(request.getChannel() +" Channel Created");

				credentialDao.save(checkUserExistance);
				response = new GenericResponse<>("00", "Password changed Succesfully!");
			} else {

				response = new GenericResponse<>("01", "User does not exist against the channel!");
			}
		} catch (Exception ex) {
			LOG.info("CredentialDetailsServiceImpl - updateCredentialPassword :"+ex);

		}

		return response;
	}

	@Override
	public GenericResponse<String> updateCredentialChannel(AuthenticationRequest request) {
		GenericResponse<String> response = new GenericResponse<>();
		Credential checkUserExistance = null;
		try {
			LOG.info("CredentialDetailsServiceImpl - Calling Save New Credential");
			checkUserExistance = (Credential) credentialDao.findByUsername(request.getUsername());
			if (checkUserExistance != null) {
				checkUserExistance.setChannelName(request.getChannel());
				credentialDao.save(checkUserExistance);
				response = new GenericResponse<>("00", "Channel changed Succesfully!");
			} else {

				response = new GenericResponse<>("01", "User does not exist against the channel!");
			}
		} catch (Exception ex) {
			LOG.info("CredentialDetailsServiceImpl - updateCredentialChannel :"+ex);

		}

		return response;
	}

	@Override
	public GenericResponse<String> deleteCredential(AuthenticationRequest request) {
		GenericResponse<String> response = new GenericResponse<>();
		Credential checkUserExistance = null;
		try {
			LOG.info("CredentialDetailsServiceImpl - Calling Save New Credential");
			checkUserExistance = (Credential) credentialDao.findByUsernameAndChannelName(request.getUsername(),
					request.getChannel());
			if (checkUserExistance != null) {
				credentialDao.delete(checkUserExistance);
				response = new GenericResponse<>("00", "User Deleted Successfully!");
			} else {

				response = new GenericResponse<>("01", "User Not Found!");
			}
		} catch (Exception ex) {
			LOG.info("CredentialDetailsServiceImpl - deleteCredential :"+ex);

		}

		return response;
	}

	@Override
	public GenericResponse<List<CredentialsListAttribute>> getCredentialsList() {
		GenericResponse<List<CredentialsListAttribute>> response = new GenericResponse<>();
		List<CredentialsListAttribute> checkExistanceUsers = null;

		try {
			LOG.info("CredentialDetailsServiceImpl - Calling Save New Credential");
			checkExistanceUsers = credentialDao.findAllUsersList();
			if (checkExistanceUsers != null) {

				response = new GenericResponse<>("00", "Users found Successfully!", checkExistanceUsers);
			} else {

				response = new GenericResponse<>("01", "Users Not Found!");
			}
		} catch (Exception ex) {
			LOG.info("CredentialDetailsServiceImpl - getCredentialsList :"+ex);

		}

		return response;
	}

	@Override
	public GenericResponse<String> disableChannel(ChannelConfigurationRequest request) {
		GenericResponse<String> response = new GenericResponse<>();
		List<Credential> checkUserExistance = null;
		try {
			LOG.info("disableChannel - Calling Disable any Channel");
			checkUserExistance = credentialDao.findByChannelName(request.getChannel());
			if (checkUserExistance != null && !checkUserExistance.isEmpty()) {
				for (Credential temp : checkUserExistance) {
					temp.setEnable(false);
					credentialDao.save(temp);
				}

				response = new GenericResponse<>("00",
						"channel and its all associated users are Disabled Successfully!");
			} else {
				response = new GenericResponse<>("01", "channel does not exist!");
			}
		} catch (Exception ex) {
			LOG.info("CredentialDetailsServiceImpl - disableChannel :"+ex);

		}

		return response;
	}

	@Override
	public GenericResponse<String> enableChannel(ChannelConfigurationRequest request) {
		GenericResponse<String> response = new GenericResponse<>();
		List<Credential> checkUserExistance = null;
		try {
			LOG.info("disableChannel - Calling Enable any Channel");
			checkUserExistance = credentialDao.findByChannelName(request.getChannel());
			if (checkUserExistance != null && !checkUserExistance.isEmpty()) {
				for (Credential temp : checkUserExistance) {
					temp.setEnable(true);
					credentialDao.save(temp);
				}

				response = new GenericResponse<>("00",
						"channel and its all associated users are Enabled Successfully!");
			} else {
				response = new GenericResponse<>("01", "channel does not exist!");
			}
		} catch (Exception ex) {
			LOG.info("CredentialDetailsServiceImpl - enableChannel :"+ex);

		}
		return response;
	}
}
