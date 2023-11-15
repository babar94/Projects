package com.gateway.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entity.PaymentLog;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.response.BillInquiryValidationResponse;
import com.gateway.service.impl.AuditLoggingServiceImpl;
import com.gateway.utils.Constants;

@Component
public class DuplicateTransactionCheckFilter implements Filter {

	private static final Logger LOG = LoggerFactory.getLogger(AuditLoggingServiceImpl.class);
	private final PaymentLogRepository paymentLogRepository;
	private final ObjectMapper objectMapper;

	public DuplicateTransactionCheckFilter(PaymentLogRepository paymentLogRepository, ObjectMapper objectMapper) {
		this.paymentLogRepository = paymentLogRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		try {
			LOG.info("DuplicateTransactionCheckFilter execution");

			// Assuming the request contains JSON data
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			String contentType = httpRequest.getContentType();

			if (contentType != null && contentType.contains("application/json")) {

				String requestBody = captureRequestBody(httpRequest);
				// Read JSON payload
				JsonNode jsonNode = objectMapper.readTree(requestBody);

				// Extract parameters from the JSON payload
				String rrn = jsonNode.path("info").path("rrn").asText();
				String stan = jsonNode.path("info").path("stan").asText();
				LOG.info("Request RRN: {}, Stan: {}", rrn, stan);

				// RRN validation: Check payment history

				List<PaymentLog> paymentHistory = paymentLogRepository.findByRrn(rrn);

				if (paymentHistory != null && !paymentHistory.isEmpty()) {
					BillInquiryValidationResponse validationResponse = new BillInquiryValidationResponse(
							Constants.ResponseCodes.DUPLICATE_TRANSACTION,
							Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);

					String jsonResponse = objectMapper.writeValueAsString(validationResponse);
					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					response.getWriter().write(jsonResponse);

					LOG.info("Duplicate transaction detected. Returning response: {}", jsonResponse);
				} else {
					// If not a duplicate, continue with the filter chain
					chain.doFilter(request, response);
				}
			} else {
				LOG.warn("Request does not contain JSON data. Skipping DuplicateTransactionCheckFilter.");
				chain.doFilter(request, response);
			}

		} catch (Exception e) {
			LOG.error("An error occurred in the DuplicateTransactionCheckFilter", e);
			throw new ServletException("Error in DuplicateTransactionCheckFilter", e);
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	private String captureRequestBody(HttpServletRequest request) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;

		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead;
				while ((bytesRead = bufferedReader.read(charBuffer)) != -1) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			} else {
				stringBuilder.append("");
			}
		} finally {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
		}

		return stringBuilder.toString();
	}

}
