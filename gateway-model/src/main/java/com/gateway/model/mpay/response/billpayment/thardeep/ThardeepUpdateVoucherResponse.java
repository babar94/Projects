package com.gateway.model.mpay.response.billpayment.thardeep;


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
public class ThardeepUpdateVoucherResponse  {

	
	@JsonProperty("response")
    private ThardeepResponse response;

	    
}
