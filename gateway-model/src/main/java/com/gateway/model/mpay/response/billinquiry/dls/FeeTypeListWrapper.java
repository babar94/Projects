package com.gateway.model.mpay.response.billinquiry.dls;

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
public class FeeTypeListWrapper {

	
	@JsonProperty("fees")
	private String fees;

	@JsonProperty("feesType")
	private String feesType;

	@JsonProperty("typeDetail")
	private String typeDetail;

	

}
