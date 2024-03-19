package com.gateway.service.impl;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gateway.entity.TransactionParams;
import com.gateway.repository.TransactionParamsDao;
import com.gateway.service.ParamsValidatorService;

@Service
public class ParamsValidatorServiceImpl implements ParamsValidatorService {

	private static final Logger LOG = LoggerFactory.getLogger(ParamsValidatorServiceImpl.class);

	@Autowired
	private TransactionParamsDao transactionParamsDao;

	@Override
	public boolean validateRequestParams(String request) {
		LOG.info("Inside method validateRequestParams");
		boolean result = false;
		String requestParamValue = null;
		JSONObject innerObject = null;
		List<TransactionParams> transParamsList = getTransactionsParams();
		JSONObject requestObj = new JSONObject(request);
		LOG.info("REQUEST Object:",requestObj);

		for (TransactionParams transactionParam : transParamsList) {
			String parameter = transactionParam.getParamName(); // list ka parameter ka naam

			String regex = transactionParam.getRegex();
			boolean match = false;
			if (requestObj.has("txnInfo")) {
				innerObject = requestObj.getJSONObject("txnInfo");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
					if (transactionParam.isRequired()) {
						match = requestParamValue.matches(regex);
						
						if (match) {
							result = true;
							LOG.info("Regex matches");
							if (parameter.equalsIgnoreCase("tranDate")) {
								SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
								f.setLenient(false);
								try {
									f.parse(requestParamValue);
									// good
								} catch (Exception e) {
									return false;
								}
							}
							if (parameter.equalsIgnoreCase("tranTime")) {
								SimpleDateFormat f = new SimpleDateFormat("HHmmss");
								f.setLenient(false);
								try {
									f.parse(requestParamValue);
									// good
								} catch (Exception e) {
									return false;
								}
							}

						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}

				}
			}
			//second object 
			if (requestObj.has("info")) {
				innerObject = requestObj.getJSONObject("info");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
					if (transactionParam.isRequired()) {
						match = requestParamValue.matches(regex);
						if (match) {
							result = true;
							LOG.info("Regex matches");
						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}
				}
			}
			if (requestObj.has("terminalInfo")) {
				innerObject = requestObj.getJSONObject("terminalInfo");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
					if (transactionParam.isRequired()) {
						match = requestParamValue.matches(regex);
						if (match) {
							result = true;
							LOG.info("Regex matches");
						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}
				}
			}
			if (requestObj.has("additionalInfo")) {
				innerObject = requestObj.getJSONObject("additionalInfo");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
					if (transactionParam.isRequired()) {
						match = requestParamValue.matches(regex);
						if (match) {
							result = true;
							LOG.info("Regex matches");
						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}
				}
			}
			if (requestObj.has("branchInfo") && !requestObj.isNull("branchInfo")) {
				innerObject = requestObj.getJSONObject("branchInfo");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
					if (transactionParam.isRequired()) {
						match = requestParamValue.matches(regex);
						if (match) {
							result = true;
							LOG.info("Regex matches");
						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}
				}
			}

		}

		return result;
	}

	
	
	                ///////////////////////////////////////
	
	
	@Override
	public boolean validateRequestParamsSpecialCharacter(String request) {
		LOG.info("Inside method validateRequestParams");
		boolean result = false;
		String requestParamValue = null;
		JSONObject innerObject = null;
		List<TransactionParams> transParamsList = getTransactionsParams();
		JSONObject requestObj = new JSONObject(request);
		LOG.info("REQUEST Object:",requestObj);

		for (TransactionParams transactionParam : transParamsList) {
			String parameter = transactionParam.getParamName(); // list ka parameter ka naam

			String regex = transactionParam.getRegex();
			boolean match = false;
			if (requestObj.has("txnInfo")) {
				innerObject = requestObj.getJSONObject("txnInfo");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
					if (transactionParam.isRequired()) {
						
						match = requestParamValue.matches(regex);
						
						if (match) {
							result = true;
							LOG.info("Regex matches");
							if (parameter.equalsIgnoreCase("tranDate")) {
								SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
								f.setLenient(false);
								try {
									f.parse(requestParamValue);
									// good
								} catch (Exception e) {
									return false;
								}
							}
							if (parameter.equalsIgnoreCase("tranTime")) {
								SimpleDateFormat f = new SimpleDateFormat("HHmmss");
								f.setLenient(false);
								try {
									f.parse(requestParamValue);
									// good
								} catch (Exception e) {
									return false;
								}
							}

						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}

				}
			}
			//second object 
			if (requestObj.has("info")) {
				innerObject = requestObj.getJSONObject("info");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
					if (transactionParam.isRequired()) {
						match = requestParamValue.matches(regex);
						if (match) {
							result = true;
							LOG.info("Regex matches");
						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}
				}
			}
			if (requestObj.has("terminalInfo")) {
				innerObject = requestObj.getJSONObject("terminalInfo");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
					if (transactionParam.isRequired()) {
						match = requestParamValue.matches(regex);
						
						if (match) {
							result = true;
							LOG.info("Regex matches");
							
							
						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}
					
			    else if(!transactionParam.isRequired()) {
						
							Pattern pattern = Pattern.compile(regex);
					        Matcher matcher = pattern.matcher(requestParamValue);

							if (matcher.find()) {
								result = false;
								LOG.info("Regex matches");
								return result;

							} else {
								result = true;
								LOG.info("Regex Not matches");
								
								
							}
					
				}
					
					
					
				}
			}
			if (requestObj.has("additionalInfo")) {
				innerObject = requestObj.getJSONObject("additionalInfo");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
				
					
					if (transactionParam.isRequired()) {
						match = requestParamValue.matches(regex);
						if (match) {
							result = true;
							LOG.info("Regex matches");
						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}
					
					
                     else if(!transactionParam.isRequired()) {
						
							Pattern pattern = Pattern.compile(regex);
					        Matcher matcher = pattern.matcher(requestParamValue);

							if (matcher.find()) {
								result = false;
								LOG.info("Regex matches");
								return result;

								
							} else {
								result = true;
								LOG.info("Regex Not matches");
								
							}
					
				}
					
				}
			}

			if (requestObj.has("branchInfo") && !requestObj.isNull("branchInfo")) {
				innerObject = requestObj.getJSONObject("branchInfo");
				if (innerObject.has(parameter)) {
					requestParamValue = innerObject.optString(parameter); // request ka parameter ka naam
					if (transactionParam.isRequired()) {
						match = requestParamValue.matches(regex);
						if (match) {
							result = true;
							LOG.info("Regex matches");
						} else {
							result = false;
							LOG.info("Regex Not matches");
							return result;
						}
					}
					
				    else if(!transactionParam.isRequired()) {
						
						Pattern pattern = Pattern.compile(regex);
				        Matcher matcher = pattern.matcher(requestParamValue);

						if (matcher.find()) {
							result = false;
							LOG.info("Regex matches");
							return result;

						} else {
							result = true;
							LOG.info("Regex Not matches");
							
							
						}
				
			}

					
				}
			}

		}

		return result;
	}
	
	
	
	// It will return Transactions Params Lists
	public List<TransactionParams> getTransactionsParams() {

		LOG.info("Inside method getTransactionsParams from Table");

		List<TransactionParams> transParamsList = (List<TransactionParams>) transactionParamsDao.findAll();

		LOG.info("Inside method getTransactionsParams where transParamsList:", transParamsList.toString());

		return transParamsList;

	}

}
