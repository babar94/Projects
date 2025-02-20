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
public class UpdateBiseKohatResponse {

	@JsonProperty("response")
	private BiseKohatPaymentResponse biseKohatResponse;
}
