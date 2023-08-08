package com.gateway.config.mpay;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "mpay.external")
@Component
public class MpayHeaderValues {

	private String username;
	private String password;
	private String operand;
	private String company;
	private String columnName;
	private String criteriaValue;
	
	
}
