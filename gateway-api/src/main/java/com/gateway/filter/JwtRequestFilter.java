package com.gateway.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gateway.service.impl.CustomCredentialsServiceImpl;
import com.gateway.utils.JwtTokenUtil;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component

public class JwtRequestFilter extends OncePerRequestFilter {

	private static final Logger LOG = LoggerFactory.getLogger(JwtRequestFilter.class);

	public static final String RRN_SESSION_KEY = "rrn";

	@Autowired
	private CustomCredentialsServiceImpl credentialsServiceImpl;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		LOG.info("JwtRequestFilter execution");
		// CustomHttpRequestBody wrappedRequest = null;

		String requestTokenHeader = request.getHeader("X-Auth-Token");
		if (requestTokenHeader == null) {
			requestTokenHeader = request.getHeader("Authorization");
		}

		if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {

			// JWT Token present, process JWT Authentication logic here
			String jwtToken = requestTokenHeader.substring(7);
			try {

				String username = jwtTokenUtil.getUsernameFromToken(jwtToken);

				if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
					UserDetails userDetails = credentialsServiceImpl.loadUserByUsername(username);

					if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
						UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
								userDetails, null, userDetails.getAuthorities());
						authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authenticationToken);

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
		}
		chain.doFilter(request, response);
		
	}

}