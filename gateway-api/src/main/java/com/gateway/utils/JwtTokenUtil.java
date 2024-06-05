package com.gateway.utils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtTokenUtil implements Serializable {

	private static final long serialVersionUID = -2550185165626007488L;

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.hours}")
	private long jwtHours;

	@Value("${jwt.mins}")
	private long jwtMins;

	@Value("${jwt.secs}")
	private long jwtSecs;

	// retrieve username from jwt token
	public String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}

	// retrieve expiration date from jwt token
	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	// retrieving channel
	public String getChannel(String token) {
		Claims claims = getAllClaimsFromToken(token);
		String channel = (String) claims.get("channel");
		// Map<String, Object> test = getMapFromIoJsonwebtokenClaims(claims);
		// System.out.println(test.toString());
		return channel;
	}

	private Claims getAllClaimsFromToken(String token) {

		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secret);

		return Keys.hmacShaKeyFor(keyBytes);
	}

	// check if the token has expired
	private Boolean isTokenExpired(String token) {
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}

	// generate token
	public String generateToken(String username, String channel) {
		Map<String, Object> claims = new HashMap<String, Object>();

		claims.put("channel", channel);

		return doGenerateToken(claims, username, channel);
	}

	@SuppressWarnings("deprecation")
	private String doGenerateToken(Map<String, Object> claims, String subject, String channel) {

		long JWT_TOKEN_VALIDITY = (jwtHours * 3600) + (jwtMins * 60) + jwtSecs;
		return Jwts.builder().setClaims(claims).subject(subject).issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000)).signWith(getSigningKey())

				.compact();

	}

	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = getUsernameFromToken(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	public String getTokenExpiry(String token) throws IOException {

		String[] split_string = token.split("\\.");
		String base64EncodedBody = split_string[1];
		Base64 base64Url = new Base64(true);
		String body = new String(base64Url.decode(base64EncodedBody));
		ObjectMapper mapper = new ObjectMapper();
		JsonNode actualObj = mapper.readTree(body);
		String exp = actualObj.get("exp").asText();
		return exp;

	}

	public String[] getTokenInformation(HttpServletRequest request) {
		String username = null;
		String jwtToken = null;
		String channel = null;
		String[] result = new String[2];
		String requestTokenHeader = null;
		requestTokenHeader = request.getHeader("X-Auth-Token");
		if (requestTokenHeader == null) {
			requestTokenHeader = request.getHeader("Authorization");
		}
		if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
			jwtToken = requestTokenHeader.substring(7);
			try {
				username = getUsernameFromToken(jwtToken);
				channel = getChannel(jwtToken);
				result[0] = username;
				result[1] = channel;
			} catch (IllegalArgumentException e) {
				System.out.println("Unable to get JWT Token");
			} catch (ExpiredJwtException e) {
				System.out.println("JWT Token has expired");
			}
		}
		return result;

	}

}
