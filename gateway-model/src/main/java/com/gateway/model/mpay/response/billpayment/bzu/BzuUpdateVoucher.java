package com.gateway.model.mpay.response.billpayment.bzu;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BzuUpdateVoucher {
	
	@JsonProperty("message")
    private String message;

}
