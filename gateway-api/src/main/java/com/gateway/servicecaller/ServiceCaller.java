package com.gateway.servicecaller;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.gateway.utils.UtilMethods;
import com.gateway.utils.XMLBeautifier;

import kong.unirest.Unirest;

@Component
public class ServiceCaller {

	@Value("${service.url.queue}")
	private String queueForwardingUrl;

	@Autowired
	private UtilMethods utilMethods;

	private static final Logger LOG = LoggerFactory.getLogger(ServiceCaller.class);

	public <T> T get(List<String> params, Class<T> type, String rrn, String userName, String billername) {
		String result = null;
		T obj = null;

		String endPoint = getEndpoint();

		try {

			LOG.info("\n\n[MPAY REQUEST without signature: " + params.toString());

			String query = prepareQueryStringWithSignature(params);

			utilMethods.insertMpayLog("Request", new Date(), userName, rrn, endPoint + query, billername);
			if (query == null)
				throw new Exception("Invalid Request Params");

			LOG.info("\n\n[MPAY REQUEST: " + params.get(0) + ", \nREQUEST: " + endPoint + query + "]\n\n{}\n\n");

			LOG.info("\ncall started at: " + new Date());
			long startTime = System.currentTimeMillis();
			result = Unirest.get(endPoint + query).asString().getBody();
			result = result.replace("fees_wrapper", "fees");

			//result ="{\"response\":{\"response_code\":\"00\",\"response_desc\":\"Successfull\",\"lesco-billinquiry\":{\"data_wrapper\":[{\"AmountWithInDue\":\"32899\",\"BillMonth\":\"APR-25\",\"Address\":\"SANDA CHUNGI MAIN B RD SANDAKALAN LHR\",\"AmountAfterDue\":\"53399\",\"CustID\":\"2017669\",\"CardType\":\"6\",\"RU_Code\":\"U\",\"DueDate\":\"03032025\",\"ReferenceNo\":\"01111110000502\",\"Name\":\"AZEEM MEHMOOD MALIKMUHAMMAD AMIN MALIK\"}]}}}";
			
			long endTime = System.currentTimeMillis();
			LOG.info("\ncall finish at: " + new Date());
			long totalTime = endTime - startTime;

			if (result == null || result.toString().isEmpty()) {
				LOG.info("MPAY Response: " + "Response null: timeout");
				utilMethods.insertMpayLog("Response", new Date(), userName, rrn, "Response null: timeout", billername);
				return null;
			}
			utilMethods.insertMpayLog("Response", new Date(), userName, rrn, result, billername);
			LOG.info("\n\n[ MPAY RESPONSE #{} ]\n\n{}\n\n", result);

			boolean isJsonObject = utilMethods.isJSON(result);

			if (isJsonObject) {
				ObjectMapper Mapobj = new ObjectMapper();
				Mapobj.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
				Mapobj.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
				obj = Mapobj.readValue(result, type);
			} else {// XML
				XmlMapper xmlMapper = new XmlMapper();
				xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
				xmlMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
				obj = xmlMapper.readValue(result, type);
			}

			LOG.info("\n[\n%%%%%%%%%%%%%% Total ***MPAY*** Call Execution Time: '{}' %%%%%%%%%%%%%%\n]\n", totalTime);

		} catch (Exception e) {
			LOG.error("{}", e);
		}

		return obj;
	}

	// Muhammad Sajid added on 27-05-24
	private static void validateParams(List<String> params) {
		for (int i = 0; i < params.size(); i++) {
			if (params.get(i) == null) {
				params.set(i, "null");
			}
		}
	}

	private static String encodeParam(String param) {
		try {
			return URLEncoder.encode(param, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			// Handle encoding exception
			LOG.error("Error encoding parameter: {}", e.toString());
			return null;

		}
	}

	public <T> T get(List<String> params, Class<T> type, String rrn) {

		String result = null;
		T obj = null;

		String endPoint = getEndpoint();

		try {
			String query = prepareQueryStringWithSignature(params);

			LOG.info("Request: Date:({}), TranRef:({}), EndPoint:({}), QueryString:({}) ", new Date(), rrn, endPoint,
					query);
			if (query == null)
				throw new Exception("Invalid Request Params");

			LOG.info("\n\n[MPAY REQUEST: " + params.get(0) + ", \nREQUEST: " + endPoint + query + "]\n\n{}\n\n");

			LOG.info("\ncall started at: " + new Date());
			long startTime = System.currentTimeMillis();
			result = Unirest.get(endPoint + query).asString().getBody();
			LOG.info(result);
			long endTime = System.currentTimeMillis();
			LOG.info("\ncall finish at: " + new Date());
			long totalTime = endTime - startTime;

			if (result == null || result.toString().isEmpty()) {
				LOG.info("MPAY Response: {} ", "Response null: timeout");
				return null;
			}
			LOG.info("Response: Date:({}), TranRef:({}), Result:({})", new Date(), rrn, result);
			LOG.info("\n\n[ MPAY RESPONSE #{} ]\n\n{}\n\n", XMLBeautifier.format(result));
			LOG.info("\n[\n%%%%%%%%%%%%%% Total ***MPAY*** Call Execution Time: '{}' %%%%%%%%%%%%%%\n]\n", totalTime);
			obj = typecastToT(result, type);

		} catch (Exception e) {
			LOG.error("{}", e);
		}

		return obj;
	}

	private String getEndpoint() {
		return queueForwardingUrl;

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

	private static String prepareQueryStringWithSignature(List<String> params) {
		try {
			validateParams(params);
			List<String> encoded = params.stream().map(t -> encodeParam(t)).collect(Collectors.toList());
			List<String> copy = new ArrayList<>(encoded);
			copy.add("paysys@123");
			return String.format("%s/%s", encodeParam(String.join(",", encoded)),
					DigestUtils.sha256Hex(String.join(",", copy)));
		} catch (Exception ex) {
			LOG.error("{}", ex.toString());
			return null;
		}
	}

}
