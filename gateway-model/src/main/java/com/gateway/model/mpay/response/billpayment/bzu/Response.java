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
public class Response {
	
	@JsonProperty("bzu-updatevoucher")
    private BzuUpdateVoucher bzugetVoucher;
    
    @JsonProperty("response_code")
    private String responseCode;
    
    @JsonProperty("response_desc")
    private String responseDesc;

}
