package com.gateway.model.mpay.response.billpayment.pta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Response {
	@JsonProperty(value = "response_code")
	private String responseCode;
	@JsonProperty(value = "response_desc")
	private String responseDesc;

}
