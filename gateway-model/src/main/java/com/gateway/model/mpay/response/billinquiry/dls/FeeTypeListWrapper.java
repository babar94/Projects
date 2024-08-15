package com.gateway.model.mpay.response.billinquiry.dls;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gateway.model.mpay.response.billinquiry.BillerDetails;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;



@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FeeTypeListWrapper implements BillerDetails {

	
	@JsonProperty("fees")
	private String fees;

	@JsonProperty("feesType")
	private String feesType;

	@JsonProperty("typeDetail")
	private String typeDetail;

	@Override
	public String getItemDetail() {

		return typeDetail;
	}

	@Override
	public String getFees() {
		return fees;
	}

}
