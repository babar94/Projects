package com.gateway.model.mpay.response.billinquiry.bisekohat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BiseKohatResponse {

	@JsonProperty("response_code")
	private String responseCode;

	@JsonProperty("bise-kohat-billinquiry")
	private BisekohatBillinquiry bisekohatbillinquiry;

	@JsonProperty("response_desc")
	private String response_desc;

}
