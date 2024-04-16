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
public class UomUpdateVoucher {

	@JsonProperty("response_Code")
    private String responseCode;

    @JsonProperty("identification_parameter")
    private String identification_parameter;
    
    @JsonProperty("reserved")
    private String reserved;
	
}
