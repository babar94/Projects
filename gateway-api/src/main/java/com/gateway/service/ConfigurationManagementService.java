package com.gateway.service;


import java.util.List;

import com.gateway.request.AuthenticationRequest;
import com.gateway.request.ChannelConfigurationRequest;
import com.gateway.request.ConfigurationRequest;
import com.gateway.response.CredentialsListAttribute;
import com.gateway.response.GenericResponse;



public interface ConfigurationManagementService {

	public GenericResponse<String> saveCredential(AuthenticationRequest user);
	public GenericResponse<String> disableCredential(ConfigurationRequest user);
	public GenericResponse<String> enableCredential(ConfigurationRequest user);
	public GenericResponse<String> updateCredentialPassword(AuthenticationRequest user);
	public GenericResponse<String> updateCredentialChannel(AuthenticationRequest user);
	public GenericResponse<String> deleteCredential(AuthenticationRequest user);
	public GenericResponse<List<CredentialsListAttribute>> getCredentialsList();
	public GenericResponse<String> disableChannel(ChannelConfigurationRequest user);
	public GenericResponse<String> enableChannel(ChannelConfigurationRequest user);
}

