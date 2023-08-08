package com.gateway.response.billpaymentresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OneLinkBillPaymentResponse {

	@JsonProperty(value="response_Code")
	private String responseCode;
	
	@JsonProperty(value="response_Description")
	private String responseDescription;
	
	@JsonProperty(value="Identification_parameter")
    private String identificationParameter;
	
	@JsonProperty(value="reserved")
    private String reserved;

	public OneLinkBillPaymentResponse(String responseCode, String responseDescription) {
		super();
		this.responseCode = responseCode;
		this.responseDescription = responseDescription;
	}

	public OneLinkBillPaymentResponse(String responseCode, String identificationParameter, String reserved) {
		super();
		this.responseCode = responseCode;
		this.identificationParameter = identificationParameter;
		this.reserved = reserved;
	}
	
	
	
	
	
	
}
