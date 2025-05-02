package com.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.gateway.authentication.JwtAuthenticationEntryPoint;
import com.gateway.filter.JwtRequestFilter;
import com.gateway.service.impl.CustomCredentialsServiceImpl;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

//	@Value("${springfox.documentation.swagger.v2.path:#{null}}")
//	private String swaggerApiDocsPath;
//	@Value("${swagger.enabled:false}")
//	private Boolean swaggerEnabled;

	public static final String[] PUBLIC_URLS = { "/api/v1/authenticate", "/v3/api-docs", "/v2/api-docs",
			"/swagger-resources/**", "/swagger-ui/**", "/webjars/**" };

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final CustomCredentialsServiceImpl customCredentialsServiceImpl;

	private final JwtRequestFilter jwtRequestFilter;
	// private final DuplicateTransactionCheckFilter
	// duplicateTransactionCheckFilter;

//	@Override
//	protected void configure(HttpSecurity httpSecurity) throws Exception {
//		// We don't need CSRF for this example
//		httpSecurity.csrf().disable().authorizeRequests().antMatchers(PUBLIC_URLS).permitAll() // Public URLs are
//																								// permitted for all
//				.anyRequest().authenticated() // All other requests require authentication
//				.and().addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class).exceptionHandling()
//				.authenticationEntryPoint(jwtAuthenticationEntryPoint).and().sessionManagement()
//				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//	}
//	@Bean
//	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//		http.csrf(AbstractHttpConfigurer::disable)
//				.authorizeHttpRequests(
//						request -> request.requestMatchers(PUBLIC_URLS).permitAll().anyRequest().authenticated())
//				.exceptionHandling(ex->ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
//				.sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//				.authenticationProvider(authenticationProvider())
//				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
//		return http.build();
//	}
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(
						request -> request.requestMatchers(PUBLIC_URLS).permitAll().anyRequest().authenticated())
				.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
				.sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

//	@Bean
//	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//		http.csrf(AbstractHttpConfigurer::disable)
//				.authorizeHttpRequests(
//						request -> request.requestMatchers(PUBLIC_URLS).permitAll().anyRequest().authenticated())
//				.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
//				.sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//				.authenticationProvider(authenticationProvider())
//				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
//				.addFilterBefore(duplicateTransactionCheckFilter, JwtRequestFilter.class); // Add your
//																							// DuplicateTransactionCheckFilter
//		return http.build();
//	}

//	private String[] anonymousEndpoints() {
//		String endpoints[] = { CustomErrorController.PATH, ApiController.AUTHENTICATE_URL, "/.well-known/**" };
//
//		if (swaggerEnabled) {
//			String[] PUBLIC_URLS = { ApiController.AUTHENTICATE_URL, "/v3/api-docs", "/v2/api-docs",
//					"/swagger-resources/**", "/swagger-ui/**", "/webjars/**"
//
//			};
//		}
//
//		return endpoints;
//	}
//	private String[] anonymousEndpoints() {
//		String endpoints[] = { CustomErrorController.PATH, ApiController.AUTHENTICATE_URL, "/.well-known/**" };
//		if (swaggerEnabled) {
//			String swaggerEndpoints[] = { "/swagger-ui.html", swaggerApiDocsPath, "/webjars/**",
//					"/swagger-resources/**" };
//			endpoints = ArrayUtils.addAll(endpoints, swaggerEndpoints);
//		}
//		return endpoints;
//	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(customCredentialsServiceImpl);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

}
