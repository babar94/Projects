package com.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.gateway.api.ApiController;
import com.gateway.authentication.JwtAuthenticationEntryPoint;
import com.gateway.filter.JwtRequestFilter;

@Configuration
@EnableWebSecurity
@EnableWebMvc
@EnableGlobalMethodSecurity(prePostEnabled =  true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

//	@Value("${springfox.documentation.swagger.v2.path:#{null}}")
//	private String swaggerApiDocsPath;
//	@Value("${swagger.enabled:false}")
//	private Boolean swaggerEnabled;

	
	public static final String[] PUBLIC_URLS={"/api/v1/authenticate","/v3/api-docs","/v2/api-docs","/swagger-resources/**","/swagger-ui/**",
            "/webjars/**"
    };

	
	@Autowired
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Autowired
	private UserDetailsService jwtUserDetailsService;

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		// configure AuthenticationManager so that it knows from where to load
		// user for matching credentials
		// Use BCryptPasswordEncoder
		auth.userDetailsService(jwtUserDetailsService).passwordEncoder(passwordEncoder());

	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {
		// We don't need CSRF for this example
		httpSecurity.csrf().disable()
				// don't authenticate this particular request
		
				.authorizeRequests()
			//	.antMatchers("/swagger-ui.html","/v3/api-docs").permitAll()
				.antMatchers(PUBLIC_URLS).permitAll()
				.antMatchers(ApiController.BILL_URL + "/billinquiry", ApiController.BILL_URL + "/billpayment")
				.authenticated().antMatchers("/**").authenticated()
				// all other requests need to be authenticated
				.anyRequest().authenticated().and()
				// configure Basic authentication
				.httpBasic().and()
				// configure JWT authentication
				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
				// configure session management
				.exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and().sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
	}

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
	
	@Override
	public void configure(WebSecurity web) throws Exception {
	    web.ignoring().antMatchers("/swagger-ui/**", "/swagger-ui.html", "/webjars/**");
	}

	
}
