package com.gateway.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gateway.filter.ValidationFilter;
import com.gateway.repository.TransactionParamsDao;
import com.gateway.utils.ValidationUtil;

@Configuration
public class FilterConfig {

	@Bean
	public FilterRegistrationBean<ValidationFilter> validationFilterRegistration(
			TransactionParamsDao transactionParamsDao, ValidationUtil validationUtil) {
		FilterRegistrationBean<ValidationFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new ValidationFilter(transactionParamsDao, validationUtil));

		// Add URL patterns to include
		registrationBean.addUrlPatterns("/api/v1/bill/billinquiry", "/api/v1/bill/billpayment");

		// You can customize other properties of the filter registration here if needed
		registrationBean.setOrder(1); // Set filter order

		return registrationBean;
	}
}
