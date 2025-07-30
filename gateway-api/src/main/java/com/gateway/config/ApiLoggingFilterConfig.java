//package com.gateway.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
//import org.springframework.boot.web.servlet.FilterRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.stereotype.Component;
//
//
//@Component
//@ConditionalOnExpression("${app.api.logging.enable:true}")
//public class ApiLoggingFilterConfig {
//	
//	@Autowired
//	private ApiLoggingFilter apiLoggingFilter;
//
//	@Value("${api.url-patterns}")
//	private String urlPatterns;
//	
//	@Bean
//	public FilterRegistrationBean<ApiLoggingFilter> loggingFilter() {
//	   FilterRegistrationBean<ApiLoggingFilter> registrationBean = new FilterRegistrationBean<>();
//	   registrationBean.setFilter(apiLoggingFilter);
//	   registrationBean.addUrlPatterns(urlPatterns);
//	   return registrationBean;
//	}
//
//}
