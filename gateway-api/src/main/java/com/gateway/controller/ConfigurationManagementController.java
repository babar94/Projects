//package com.gateway.controller;
//
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.gateway.api.ApiController;
//import com.gateway.request.AuthenticationRequest;
//import com.gateway.request.ChannelConfigurationRequest;
//import com.gateway.request.ConfigurationRequest;
//import com.gateway.response.CredentialsListAttribute;
//import com.gateway.response.GenericResponse;
//import com.gateway.service.ConfigurationManagementService;
//
//@RestController
//@RequestMapping(path = ApiController.CONFIGURATION_URL)
//public class ConfigurationManagementController extends ApiController {
//
//	private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManagementController.class);
//
//	@Autowired
//	private ConfigurationManagementService configurationManagementService;
//
//
//	@RequestMapping(path = "/createcredential", method = RequestMethod.POST)
//	public GenericResponse<String> saveUser(@RequestBody AuthenticationRequest user) throws Exception {
//		return configurationManagementService.saveCredential(user);
//	}
//	
//	@RequestMapping(path = "/disablecredential", method = RequestMethod.POST)
//	public GenericResponse<String> disableCredential(@RequestBody ConfigurationRequest user) throws Exception {
//		return configurationManagementService.disableCredential(user);
//	}
//	
//	@RequestMapping(path = "/enablecredential", method = RequestMethod.POST)
//	public GenericResponse<String> enableCredential(@RequestBody ConfigurationRequest user) throws Exception {
//		return configurationManagementService.enableCredential(user);
//	}
//	
//	@RequestMapping(path = "/updatecredentialpassword", method = RequestMethod.POST)
//	public GenericResponse<String> updateCredentialPassword(@RequestBody AuthenticationRequest user) throws Exception {
//		return configurationManagementService.updateCredentialPassword(user);
//	}
//	
//	@RequestMapping(path = "/updatecredentialchannel", method = RequestMethod.POST)
//	public GenericResponse<String> updateCredentialChannel(@RequestBody AuthenticationRequest user) throws Exception {
//		return configurationManagementService.updateCredentialChannel(user);
//	}
//	
//	@RequestMapping(path = "/deletecredential", method = RequestMethod.POST)
//	public GenericResponse<String> deleteCredential(@RequestBody AuthenticationRequest user) throws Exception {
//		return configurationManagementService.deleteCredential(user);
//	}
//	
//	@RequestMapping(path = "/getcredentialslist", method = RequestMethod.GET)
//	public GenericResponse<List<CredentialsListAttribute>> getCredentialsList() throws Exception {
//		return configurationManagementService.getCredentialsList();
//	}
//	
//	@RequestMapping(path = "/disablechannel", method = RequestMethod.POST)
//	public GenericResponse<String> disableChannel(@RequestBody ChannelConfigurationRequest user) throws Exception {
//		return configurationManagementService.disableChannel(user);
//	}
//	
//	@RequestMapping(path = "/enablechannel", method = RequestMethod.POST)
//	public GenericResponse<String> enableChannel(@RequestBody ChannelConfigurationRequest user) throws Exception {
//		return configurationManagementService.enableChannel(user);
//	}
//
//}
