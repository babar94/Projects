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
public class BzuGetVoucherResponse {
	
	@JsonProperty("response")
	private Response response;

}
