package com.gateway.model.mpay.response.billpayment.wasa;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gateway.model.mpay.response.billinquiry.wasa.WasaBillInquiry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class WasaPaymentResponse {

	@JsonProperty("response_code")
	private String responseCode;
	
	@JsonProperty("response_desc")
	private String responseDesc;
	
	@JsonProperty("wasa-billpayment")
	private WasaBillPayment wasaBillPayment;
	
}
