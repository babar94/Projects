package com.gateway.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.filter.DuplicateTransactionCheckFilter;
import com.gateway.repository.PaymentLogRepository;
import com.gateway.request.billinquiry.BillInquiryRequest;

@Configuration
public class BeanConfiguration {

	@Bean
	public BillInquiryRequest getBillInquiryRequest() {

		return new BillInquiryRequest();
	}

	@Bean
	public ObjectMapper mapper() {

		return new ObjectMapper();
	}

	@Bean
	public FilterRegistrationBean<DuplicateTransactionCheckFilter> duplicateTransactionCheckFilterRegistration(
			PaymentLogRepository paymentLogRepository, ObjectMapper objectMapper) {
		FilterRegistrationBean<DuplicateTransactionCheckFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new DuplicateTransactionCheckFilter(paymentLogRepository, objectMapper));

		registrationBean.addUrlPatterns("/api/v1/bill/billinquiry","/api/v1/bill/billpayment");

		return registrationBean;
	}

}
