package com.gateway.model.mpay.response.billinquiry.lesco;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LescoBillInquiry {

	@JsonProperty("response_code")
	private String responseCode;

	@JsonProperty("response_desc")
	private String response_desc;

	@JsonProperty("lesco-billinquiry")
	private LescoBillinquiryData lescobillinquirydata;
	
}
