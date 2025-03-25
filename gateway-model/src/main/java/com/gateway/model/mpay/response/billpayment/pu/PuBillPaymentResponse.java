package com.gateway.model.mpay.response.billpayment.pu;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PuBillPaymentResponse {
	
	@JsonProperty("response")
	private PuBillPayment puBillPayment;

}
