package com.gateway.model.mpay.response.billinquiry.uom;

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
	private String responseCode;

	@JsonProperty("uom-getvoucher")
	private UomGetVoucher uomgetvoucher;

	@JsonProperty("response_desc")
	private String responseDesc;

}
