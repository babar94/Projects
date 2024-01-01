package com.gateway.model.mpay.response.billinquiry.pta;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
//@JsonIgnoreProperties(ignoreUnknown = true)

public class Response implements Serializable {
	private static final long serialVersionUID = 8396436785949897705L;

	@JsonProperty(value = "response_code")
	private String responseCode;
	@JsonProperty(value = "response_desc")
	private String responseDesc;
	
	@JsonProperty(value = "pta-getvoucher")
	 private PtaGetVoucher ptaGetVoucher;
}
