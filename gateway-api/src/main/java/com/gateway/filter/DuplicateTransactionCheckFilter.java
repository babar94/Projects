//package com.gateway.filter;
//
//import java.io.IOException;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.util.ContentCachingRequestWrapper;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.gateway.entity.PaymentLog;
//import com.gateway.repository.PaymentLogRepository;
//import com.gateway.response.BillInquiryValidationResponse;
//import com.gateway.utils.Constants;
//
//import jakarta.servlet.Filter;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.FilterConfig;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.ServletRequest;
//import jakarta.servlet.ServletResponse;
//import jakarta.servlet.http.HttpServletRequest;
//
//@Component
//public class DuplicateTransactionCheckFilter implements Filter {
//
//	private static final Logger LOG = LoggerFactory.getLogger(DuplicateTransactionCheckFilter.class);
//	private final PaymentLogRepository paymentLogRepository;
//	private final ObjectMapper objectMapper;
//
//	public DuplicateTransactionCheckFilter(PaymentLogRepository paymentLogRepository, ObjectMapper objectMapper) {
//		this.paymentLogRepository = paymentLogRepository;
//		this.objectMapper = objectMapper;
//	}
//
//	@Override
//	public void init(FilterConfig filterConfig) throws ServletException {
//		// TODO Auto-generated method stub
//	}
//
//	@Override
//	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//			throws IOException, ServletException {
//
//		try {
//			LOG.info("DuplicateTransactionCheckFilter execution");
//
//			// Assuming the request contains JSON data
//			HttpServletRequest httpRequest = (HttpServletRequest) request;
//			String contentType = httpRequest.getContentType();
//
//			if (contentType != null && contentType.contains("application/json")) {
//				// Wrap the request to allow multiple reads of the request body
//				ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
//				String requestBody = new String(requestWrapper.getContentAsByteArray());
//
//				// Read JSON payload
//				JsonNode jsonNode = objectMapper.readTree(requestBody);
//
//				// Extract parameters from the JSON payload
//				String rrn = jsonNode.path("info").path("rrn").asText();
//				String stan = jsonNode.path("info").path("stan").asText();
//				LOG.info("Request RRN: {}, Stan: {}", rrn, stan);
//
//				// RRN validation: Check payment history
//				List<PaymentLog> paymentHistory = paymentLogRepository.findByRrn(rrn);
//
//				if (paymentHistory != null && !paymentHistory.isEmpty()) {
//					BillInquiryValidationResponse validationResponse = new BillInquiryValidationResponse(
//							Constants.ResponseCodes.DUPLICATE_TRANSACTION,
//							Constants.ResponseDescription.DUPLICATE_TRANSACTION, rrn, stan);
//
//					String jsonResponse = objectMapper.writeValueAsString(validationResponse);
//					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//					response.getWriter().write(jsonResponse);
//
//					LOG.info("Duplicate transaction detected. Returning response: {}", jsonResponse);
//				} else {
//					// If not a duplicate, continue with the filter chain
//					chain.doFilter(requestWrapper, response);
//				}
//			} else {
//				LOG.warn("Request does not contain JSON data. Skipping DuplicateTransactionCheckFilter.");
//				chain.doFilter(request, response);
//			}
//
//		} catch (Exception e) {
//			LOG.error("An error occurred in the DuplicateTransactionCheckFilter", e);
//			throw new ServletException("Error in DuplicateTransactionCheckFilter", e);
//		}
//	}
//
//	@Override
//	public void destroy() {
//		// TODO Auto-generated method stub
//
//	}
//
//}
