package com.gateway.model.mpay.response.billinquiry.aiou;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiouGetVoucher {

	@JsonProperty("ResponseBillInquiry")
	private ResponseBillInquiry responseBillInquiry;
}
