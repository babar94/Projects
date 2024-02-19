package com.gateway.model.mpay.response.billinquiry.aiou;

import java.io.Serializable;

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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
//@JsonIgnoreProperties(ignoreUnknown = true)

public class Response implements Serializable {
	private static final long serialVersionUID = 8396436785949897705L;
	@JsonProperty("response_code")
	private String responseCode;

	@JsonProperty("response_desc")
	private String responseDesc;

	@JsonProperty("aiou-getvoucher")
	private AiouGetVoucher aiouGetVoucher;
}
