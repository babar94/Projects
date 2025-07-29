package com.gateway.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}


}
