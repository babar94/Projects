package com.gateway.model.mpay.response.billinquiry.bzu;

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
	
	@JsonProperty("bzu-getvoucher")
    private BzuGetVoucher bzugetVoucher;
    
    @JsonProperty("response_code")
    private String responseCode;
    
    @JsonProperty("response_desc")
    private String responseDesc;

}
