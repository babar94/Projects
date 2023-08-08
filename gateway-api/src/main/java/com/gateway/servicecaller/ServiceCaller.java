package com.gateway.servicecaller;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.utils.URLParamEncoder;
import com.gateway.utils.UtilMethods;
import com.gateway.utils.XMLBeautifier;

@Component
public class ServiceCaller {

	@Value("${service.url.queue}")
	private String queueForwardingUrl;

	@Value("${service.url.simulator}")
	private String simulatorUrl;

	@Value("${service.url.sms}")
	private String smsUrl;

	@Value("${service.url.email}")
	private String emailUrl;

	@Value("${service.url.email.attachment}")
	private String attachmentUrl;

	@Value("${service.secret}")
	private String secret;

	@Value("${mpay.calls.simulator}")
	public String mpaySimulator;

	@Autowired
	private UtilMethods utilMethods;

	/*
	 * @Autowired private PrettyPrinter prettyPrinter;
	 */

	private static final Logger LOG = LoggerFactory.getLogger(ServiceCaller.class);

	public <T> T get(List<String> params, Class<T> type, String rrn, String userName) {
		String result = null;
		T obj = null;

		String endPoint = getEndpoint();

		try {
			
			String query = prepareQueryStringWithSignature(params);

			utilMethods.insertMpayLog("Request", new Date(), userName, rrn, endPoint + query);
			if (query == null)
				throw new Exception("Invalid Request Params");

			LOG.info("\n\n[MPAY REQUEST: " + params.get(0) + ", \nREQUEST: " + endPoint + query + "]\n\n{}\n\n");

			LOG.info("\ncall started at: " + new Date());
			long startTime = System.currentTimeMillis();
			result = Unirest.get(endPoint + query).asString().getBody();
			result = result.replace("fees_wrapper", "fees");
			long endTime = System.currentTimeMillis();
			LOG.info("\ncall finish at: " + new Date());
			long totalTime = endTime - startTime;

			if (result == null || result.toString().isEmpty()) {
				LOG.info("MPAY Response: " + "Response null: timeout");
				utilMethods.insertMpayLog("Response", new Date(), userName, rrn, "Response null: timeout");
				return null;
			}
			// LOG.info("\n\n[ MPAY RESPONSE #{} ]\n\n{}\n\n",
			// XMLBeautifier.format(result));
			utilMethods.insertMpayLog("Response", new Date(), userName, rrn, result);
			LOG.info("\n\n[ MPAY RESPONSE #{} ]\n\n{}\n\n", result);

			ObjectMapper Mapobj = new ObjectMapper();
			Mapobj.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			Mapobj.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
			obj = Mapobj.readValue(result, type);

//		    ObjectMapper mapper = new JsonMapper();
//		    JsonNode json = mapper.readTree(body);
//
//		    String email = json.get("email").asText();
//		    String password = json.get("password").asText();

			LOG.info("\n[\n%%%%%%%%%%%%%% Total ***MPAY*** Call Execution Time: '{}' %%%%%%%%%%%%%%\n]\n", totalTime);
			// obj = typecastToT(result, type);

		} catch (Exception e) {
			LOG.error("{}", e);
		}

		return obj;
	}

	 public <T> T get(List<String> params, Class<T> type, String rrn) {
	        
	    	String result = null;
	        T obj = null;
	                
	        String endPoint = getEndpoint();
	        
	        try {
	            String query = prepareQueryStringWithSignature(params);
	                
	            LOG.info("Request: Date:({}), TranRef:({}), EndPoint:({}), QueryString:({}) ", new Date(), rrn, endPoint, query);
	            if(query == null)
	                throw new Exception("Invalid Request Params");
	            
	            LOG.info("\n\n[MPAY REQUEST: " + params.get(0) + ", \nREQUEST: " + endPoint + query + "]\n\n{}\n\n");

	            LOG.info("\ncall started at: " + new Date());
	            long startTime = System.currentTimeMillis();
	            result = Unirest.get(endPoint + query).asString().getBody();
	            LOG.info(result);
	            long endTime   = System.currentTimeMillis();
	            LOG.info("\ncall finish at: " + new Date());
	            long totalTime = endTime - startTime;
	            
	            if (result == null || result.toString().isEmpty()) {
	            	LOG.info("MPAY Response: {} ", "Response null: timeout");
	                return null;
	            }
	            LOG.info("Response: Date:({}), TranRef:({}), Result:({})", new Date(), rrn, result);
	            //System.out.print("MPAY Response: " + result.getBody());
	            LOG.info("\n\n[ MPAY RESPONSE #{} ]\n\n{}\n\n", XMLBeautifier.format(result));
	            LOG.info("\n[\n%%%%%%%%%%%%%% Total ***MPAY*** Call Execution Time: '{}' %%%%%%%%%%%%%%\n]\n", totalTime);
	            obj = typecastToT(result, type);

	        } catch (Exception e) {
	            LOG.error("{}", e);
	        }

	        return obj;
	    }
	
	private String getEndpoint() {
		if (mpaySimulator == null || mpaySimulator.equals("0"))
			return queueForwardingUrl;
		else
			return simulatorUrl;
	}

	@SuppressWarnings("unchecked")
	public <T> T typecastToT(String result, Class<T> type) {
		StringReader reader = new StringReader(result);

		JAXBContext jaxbContext;
		try {
			jaxbContext = JAXBContext.newInstance(type);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			return (T) jaxbUnmarshaller.unmarshal(reader);
		} catch (JAXBException e) {
			LOG.error("{}", e);
		}

		return null;

	}

	private String prepareQueryStringWithSignature(List<String> params) {

		try {

			List<String> encoded = params.stream().map(t -> URLParamEncoder.encode(t)).collect(Collectors.toList());
			List<String> copy = new ArrayList<>(encoded);
			copy.add(secret);

			return String.format("%s/%s", URLParamEncoder.encode(String.join(",", encoded)),
					DigestUtils.sha256Hex(String.join(",", copy)));

		} catch (Exception ex) {
			LOG.error("{}", ex);
			return null;
		}

	}

}
