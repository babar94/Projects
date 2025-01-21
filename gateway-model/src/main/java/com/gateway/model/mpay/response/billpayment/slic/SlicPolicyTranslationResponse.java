package com.gateway.model.mpay.response.billpayment.slic;

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
public class SlicPolicyTranslationResponse {

	@JsonProperty("response_code")
	private String responseCode;
	
	@JsonProperty("response_desc")
	private String response_desc;
	
	@JsonProperty("slic-policytranslation")
	private SlicPolicyTranslation slicPolicyTranslation;
	
	

}
