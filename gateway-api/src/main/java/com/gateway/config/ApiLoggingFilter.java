package com.gateway.config;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.gateway.utils.FilterRequestResponseUtils;
import com.gateway.utils.FilterRequestResponseUtils.BufferedRequestWrapper;
import com.gateway.utils.FilterRequestResponseUtils.BufferedResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//import kong.unirest.JsonNode;
//import kong.unirest.Unirest;

@Component
public class ApiLoggingFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(ApiLoggingFilter.class);

	@Autowired
	private FilterRequestResponseUtils filterRequestResponseUtils;

//	@Value("${base.path}")
//	private String basePath;
//
//	@Value("${bpmserver.ip}")
//	private String bpmIp;
//
//	@Value("${bpmserver.sockettimeout}")
//	private int socketTimeout;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;
//		ActivityLogs activityLogs = new ActivityLogs();

		try {

			BufferedRequestWrapper bufferedRequest = new BufferedRequestWrapper(httpServletRequest);

			BufferedResponseWrapper bufferedResponse = new BufferedResponseWrapper(httpServletResponse);
			String reqBody = bufferedRequest.getRequestBody();

			logger.info("reqBody : {}", reqBody);
			logger.info("method : {}", httpServletRequest.getMethod());
			logger.info("method : {}", httpServletRequest.getHeaderNames().toString());

	//		saveReqToDb(httpServletRequest, bufferedRequest, activityLogs);

			final StringBuilder logRequest = new StringBuilder("HTTP ").append(httpServletRequest.getMethod())
					.append(" \"").append(httpServletRequest.getServletPath()).append("\" ").append(", parameters=")
					.append(", body=").append(bufferedRequest.getRequestBody()).append(", remote_address=")
					.append(httpServletRequest.getRemoteAddr());
			logger.info(logRequest.toString());

			try {
				chain.doFilter(bufferedRequest, bufferedResponse);

			} finally {
				StringBuilder logResponse = new StringBuilder("HTTP RESPONSE ").append(bufferedResponse.getContent());
				//logger.info(logResponse.length() >= 200 ? logResponse.substring(0, 200) : logResponse.toString());
				logger.info(logResponse.toString());

			}
		//	saveResToDb(bufferedResponse, httpServletResponse, activityLogs);
			MDC.clear();

		} catch (Throwable e) {
			logger.error(e.getMessage());
			httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
		}

	}

//	public void saveReqToDb(HttpServletRequest httpServletRequest, BufferedRequestWrapper bufferedRequest,
//			ActivityLogs activityLogs) throws IOException {
//
//		String path = httpServletRequest.getServletPath();// .substring(basePath.length());
//
//		String controllerString = path;
//		String reqBody = bufferedRequest.getRequestBody();
//		String reqHeaders = filterRequestResponseUtils.getReqHeaders(httpServletRequest);
//
//		logger.info("reqHeaders: {}", reqHeaders);
//
//		String userId = bufferedRequest.getHeader("UserId");
//
//		String ipAddress = bufferedRequest.getHeader("IP-Address");
//		if (ipAddress == null || ipAddress.equals("null") || ipAddress.equals("")) {
//			logger.info("getIPAddress: {}", httpServletRequest.getRemoteAddr());
//			activityLogs.setIpAddress(httpServletRequest.getRemoteAddr());
//		} else {
//			logger.info("getIPAddress: {}", ipAddress);
//			activityLogs.setIpAddress(ipAddress);
//		}
//
//		activityLogs.setUserId(userId);
//		activityLogs.setRequestBody(reqBody);
//		activityLogs.setRequestDate(new Date());
//		activityLogs.setActivity(controllerString);
//		activityLogs.setRequestHeaders(reqHeaders);
//
//	}

//	public void saveResToDb(BufferedResponseWrapper bufferedResponse, HttpServletResponse httpServletResponse,
//			ActivityLogs activityLogs) throws IOException {
//
//		Integer status = httpServletResponse.getStatus();
//		String responseBody = bufferedResponse.getContent();
//
//		String responseHeaders = filterRequestResponseUtils.getResHeaders(httpServletResponse);
//
//		activityLogs.setResponseHeaders(responseHeaders);
//		activityLogs.setResponseCode(status.toString());
//
//		activityLogs.setResponseBody(responseBody);
//		activityLogs.setResponseDate(new Date());
//
//		Unirest.post(bpmIp + "/activitylog").header("Content-Type", "application/json").body(activityLogs)
//				.socketTimeout(socketTimeout).asJsonAsync(response -> {
//					int code = response.getStatus();
//					JsonNode body = response.getBody();
//					logger.info("code {} call", code, body.toString());
//				});
//	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

}
