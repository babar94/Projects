package com.gateway.model.mpay.response.billinquiry.slic;

import java.util.List;

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
public class SlicPolicyInquiry {

	@JsonProperty("policy_no")
	private String policyNo;

	@JsonProperty("policy_holder")
	private String policy_holder;

	@JsonProperty("due_date")
	private String due_date;

	@JsonProperty("message")
	private String message;

	@JsonProperty("result_wrapper")
	private List<ResultWrapper> resultWrapper;

	@JsonProperty("status")
	private String status;

}
