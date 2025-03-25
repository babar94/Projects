package com.gateway.model.mpay.response.billinquiry.pu;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PuBillInquiryData {

	
	@JsonProperty("challan_no")
	private String challanNo;
	@JsonProperty("candidate_name")
	private String CandidateName;
	@JsonProperty("voucher_amount")
	private String VoucherAmount;
	@JsonProperty("candidate_nic")
	private String CandidateCnic;
	
}
