package com.gateway.model.mpay.response.billpayment.bisekohat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BiseKohatPaymentResponse {

	@JsonProperty("response_code")
	private String responseCode;

	@JsonProperty("response_desc")
	private String response_desc;
	
	@JsonProperty("bise-kohat-billpayment")
	private BiseKohatBillPayment biseKohatBillPayment;

	
}
