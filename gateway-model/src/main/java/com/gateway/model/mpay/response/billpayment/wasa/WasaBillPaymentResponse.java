package com.gateway.model.mpay.response.billpayment.wasa;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class WasaBillPaymentResponse {

	 @JsonProperty("response")
	 private WasaPaymentResponse wasaBillPayment;
}
