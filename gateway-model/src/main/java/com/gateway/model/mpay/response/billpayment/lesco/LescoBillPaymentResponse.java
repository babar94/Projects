package com.gateway.model.mpay.response.billpayment.lesco;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LescoBillPaymentResponse {
	
	@JsonProperty("response")
	private LescoBillPayment lescoBillPayment;

}
