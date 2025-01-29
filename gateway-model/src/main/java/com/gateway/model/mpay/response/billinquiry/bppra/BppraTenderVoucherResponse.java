package com.gateway.model.mpay.response.billinquiry.bppra;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BppraTenderVoucherResponse {

	@JsonProperty("challanData")
	private TenderChallanData tenderChallanData;

	@JsonProperty("challanFee")
	private List<ChallanFee> challanFee;

}
