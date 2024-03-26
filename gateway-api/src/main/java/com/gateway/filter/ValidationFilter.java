package com.gateway.filter;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.gateway.entity.TransactionParams;
import com.gateway.repository.TransactionParamsDao;
import com.gateway.response.billinquiryresponse.BillInquiryResponse;
import com.gateway.response.billinquiryresponse.Info;
import com.gateway.utils.Constants;
import com.gateway.utils.ValidationUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class ValidationFilter implements Filter {

	private final TransactionParamsDao transactionParamsDao;
	private final ValidationUtil validationUtil;

	@Autowired
	public ValidationFilter(TransactionParamsDao transactionParamsDao, ValidationUtil validationUtil) {
		this.transactionParamsDao = transactionParamsDao;
		this.validationUtil = validationUtil;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper((HttpServletRequest) request);

		try {
			// Retrieve the content from the request wrapper
			byte[] content = requestWrapper.getContentAsByteArray();
			String encoding = requestWrapper.getCharacterEncoding();
			String requestBody = new String(content, encoding);
			System.out.println("Request body: " + requestBody);

			// Set the Content-Type header to application/json if not already set
			if (requestWrapper.getContentType() == null
					|| !requestWrapper.getContentType().startsWith("application/json")) {
				((ServletResponse) requestWrapper).setContentType("application/json");
			}

			// Parse the JSON string into a JSONObject
			JSONObject jsonObject = new JSONObject(requestBody);
			// Extract rrn and stan values from the info object
			JSONObject infoObject = jsonObject.getJSONObject("info");
			String rrn = infoObject.getString("rrn");
			String stan = infoObject.getString("stan");

			TransactionParams paramsDaoRrn = transactionParamsDao.findByParamName("rrn");
			TransactionParams paramsDaoStan = transactionParamsDao.findByParamName("stan");

			// RRN validation
			String regexRrn = paramsDaoRrn.getRegex();
			boolean matchRrn = rrn.matches(regexRrn);

			if (!matchRrn) {
				if (!StringUtils.isNumeric(rrn))
					rrn = "";
			}

			// STAN validation
			String regexStan = paramsDaoStan.getRegex();
			boolean matchStan = stan.matches(regexStan);

			if (!matchStan) {
				if (!StringUtils.isNumeric(stan))
					stan = "";
			}

			if (validationUtil.isNullOrEmpty(rrn) || validationUtil.isNullOrEmpty(stan)) {
				// Invalid data response
				Info info = new Info(Constants.ResponseCodes.INVALID_DATA, Constants.ResponseDescription.INVALID_DATA,
						rrn, stan);
				BillInquiryResponse billInquiryResponse = new BillInquiryResponse(info, null, null);
				response.getWriter().write(billInquiryResponse.toString()); // Convert response to string before writing
			} else {
				// Proceed with the chain if data is valid
				chain.doFilter(requestWrapper, response);
			}
		} catch (IOException e) {
			// Handle IOException if needed
			e.printStackTrace();
			throw new ServletException("Error processing request", e); // Re-throw ServletException
		}

	}
}
