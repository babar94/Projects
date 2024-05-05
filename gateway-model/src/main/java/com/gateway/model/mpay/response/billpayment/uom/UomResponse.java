package com.gateway.model.mpay.response.billpayment.uom;

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
public class UomResponse {
	
	
	@JsonProperty("response_code")
    private String response_code;
  
	@JsonProperty("uom-updatevoucher")
    private UomUpdateVoucher uomUpdateVoucher;
  
	@JsonProperty("response_desc")
    private String response_desc;
  
	
}
