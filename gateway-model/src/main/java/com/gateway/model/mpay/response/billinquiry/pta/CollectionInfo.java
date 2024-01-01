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
public class CollectionInfo {

	@JsonProperty("demand_date")
	private String demandDate;

	@JsonProperty("amount")
	private String amount;

	@JsonProperty("bank_details")
	private String bankDetails;

	@JsonProperty("demand_type")
	private String demandType;
}
