package com.gateway.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.response.AuthenticationResponse;
import com.gateway.service.CredentialDetailsService;
import com.gateway.utils.Constants;
import com.gateway.utils.JwtTokenUtil;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

	// private static final Logger LOG =
	// LoggerFactory.getLogger(JwtRequestFilter.class);

	public static final String RRN_SESSION_KEY = "rrn";

	@Autowired
	private CredentialDetailsService jwtUserDetailsService;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		CustomHttpRequestBody wrappedRequest = null;
		boolean basicHeader = false;
		String requestTokenHeader = request.getHeader("X-Auth-Token");
		if (requestTokenHeader == null) {
			requestTokenHeader = request.getHeader("Authorization");
			if (requestTokenHeader != null)
				basicHeader = requestTokenHeader.startsWith("Basic");
		}

		if ((basicHeader && request.getRequestURI().equals("/api/v1/bill/Payments/BillInquiry"))
				|| (basicHeader && request.getRequestURI().equals("/api/v1/bill/Payments/BillPayment"))) {

			chain.doFilter(request, response);
			return;
		}

		if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")
				&& !(request.getRequestURI().equals("/api/v1/bill/Payments/BillInquiry")
						|| request.getRequestURI().equals("/api/v1/bill/Payments/BillPayment"))) {

			// JWT Token present, process JWT Authentication logic here
			String jwtToken = requestTokenHeader.substring(7);
			try {

				String username = jwtTokenUtil.getUsernameFromToken(jwtToken);

				if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
					UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

					if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
						UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
								userDetails, null, userDetails.getAuthorities());
						authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authenticationToken);
						// Added new Work forx custom Request..
						wrappedRequest = new CustomHttpRequestBody((HttpServletRequest) request);
					}
				}
			} catch (IllegalArgumentException e) {
				logger.warn("Unable to get JWT Token");
			} catch (ExpiredJwtException e) {
				logger.warn("JWT Token has expired");
			} catch (MalformedJwtException e) {
				logger.warn("Malformed JWT Token");
			}

		} else {
			// Invalid token header, handle accordingly
			logger.warn("JWT Token does not begin with Bearer String");
			log.info("RequestUri:"+request.getRequestURI());
			log.info("RequestUri:"+request.getRequestURI());
			log.info("RequestUri:"+request.getRequestURI().equalsIgnoreCase("/swagger-ui.html"));
			if (request.getRequestURI().equalsIgnoreCase("/api/v1/authenticate") || request.getRequestURI().equalsIgnoreCase("swagger-ui.html")) {
				chain.doFilter(request, response);
				return;
			}
//			AuthenticationResponse authResponse = new AuthenticationResponse(Constants.ResponseCodes.UNAUTHORISED,
//					Constants.ResponseDescription.UNAUTHORISED_WRONG_CREDENTIALS);
//			ObjectMapper objectMapper = new ObjectMapper();
//			String responseBody = objectMapper.writeValueAsString(authResponse);
//			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//			response.setContentType("application/json");
//			response.getWriter().write(responseBody);
//			response.getWriter().flush();
//			response.getWriter().close();
//			return;

		}

		if (wrappedRequest == null)
			chain.doFilter(request, response);
		else
			chain.doFilter(wrappedRequest, response);
	}

//		@Override
//		protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
//			String servletPath = request.getServletPath();
//			String requestURI = request.getRequestURI();
//	
//		log.info("servletPath " +servletPath);
//		log.info("requestURI" +requestURI);
//			// Exclude the favicon.ico request from filtering
////			if (servletPath.equals("/favicon.ico") || requestURI.endsWith("/favicon.ico")) {
////				return true;
////			}
//				return servletPath.equals("/swagger-ui.html");
////	
////			// Exclude the swagger-ui.html request from filtering
////			return servletPath.equals("/swagger-ui/index.html") || requestURI.endsWith("//swagger-ui/index.html");
////		}


		
}