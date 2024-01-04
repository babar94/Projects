package com.gateway.model.mpay.response.billinquiry.pta;

import java.util.List;

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
public class DataWrapper {

	@JsonProperty("depositorName")
	private String depositorName;

	@JsonProperty("depositorContactNo")
	private String depositorContactNo;

	@JsonProperty("branchName")
	private String branchName;

	@JsonProperty("collectionInfoList_wrapper")
	private List<CollectionInfo> collectionInfoListWrapper;

	@JsonProperty("licenseeName")
	private String licenseeName;

	@JsonProperty("transactionType")
	private String transactionType;

	@JsonProperty("branchCode")
	private String branchCode;

	@JsonProperty("totalAmount")
	private String totalAmount;

	@JsonProperty("totalAmountWords")
	private String totalAmountWords;

	@JsonProperty("dateCreated")
	private String dateCreated;

	@JsonProperty("branch_Id")
	private String branchId;

	@JsonProperty("demandNote")
	private String demandNote;

	@JsonProperty("feeTypesList_wrapper")
	private List<FeeType> feeTypesListWrapper;

	@JsonProperty("id")
	private String id;

	@JsonProperty("status")
	private String status;

	private String oneBillNumber;
}
