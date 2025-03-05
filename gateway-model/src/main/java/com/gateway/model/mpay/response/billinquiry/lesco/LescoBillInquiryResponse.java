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
public class LescoBillInquiryResponse {
	
	@JsonProperty("response")
	private LescoBillInquiry lescoBillInquiry;

}
