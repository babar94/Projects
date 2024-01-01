package com.gateway.model.mpay.response.billinquiry.pta;

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
public class FeeType {

	@JsonProperty("fee_Amount")
	private String feeAmount;

	@JsonProperty("feeTypeName")
	private String feeTypeName;

	@JsonProperty("period_Paid")
	private String periodPaid;

}
