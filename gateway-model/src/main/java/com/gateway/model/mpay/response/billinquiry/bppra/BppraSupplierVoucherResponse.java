package com.gateway.model.mpay.response.billinquiry.bppra;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BppraSupplierVoucherResponse {

	@JsonProperty("challanData")
	private SupplierChallanData supplierChallanData;

	@JsonProperty("challanFee")
	private List<ChallanFee> challanFee;

}
