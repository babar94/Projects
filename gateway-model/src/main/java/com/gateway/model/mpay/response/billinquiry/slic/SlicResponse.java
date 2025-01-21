package com.gateway.model.mpay.response.billinquiry.slic;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SlicResponse {

	@JsonProperty("response_code")
	private String responseCode;
	
	@JsonProperty("slic-policyinquiry")
	private SlicPolicyInquiry slicPolicyInquiry;
	
	@JsonProperty("response_desc")
	private String response_desc;

}
