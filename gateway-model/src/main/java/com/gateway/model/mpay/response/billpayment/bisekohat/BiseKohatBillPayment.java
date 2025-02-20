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
public class BiseKohatBillPayment {

	@JsonProperty("success")
	private String success;

	@JsonProperty("data")
	private String data;

	@JsonProperty("Massage")
	private String massage;

}
