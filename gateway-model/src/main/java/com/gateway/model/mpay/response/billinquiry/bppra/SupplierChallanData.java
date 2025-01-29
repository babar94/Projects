package com.gateway.model.mpay.response.billinquiry.bppra;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SupplierChallanData {

	@JsonProperty("ID")
	private String id;

	@JsonProperty("NAME")
	private String name;

	@JsonProperty("ADDRESS")
	private String address;

	@JsonProperty("BANKNAME")
	private String bankName;

	@JsonProperty("ACCOUNT")
	private String account;

	@JsonProperty("CATEGORYNAME")
	private String categoryName;

	@JsonProperty("NOTE")
	private String note;

	@JsonProperty("NTN")
	private String ntn;

	@JsonProperty("SUPPLIERCONTRACTORNAME")
	private String supplierContractorName;

	@JsonProperty("NAMEOFSUPPLIER")
	private String supplierName;

	@JsonProperty("CNICOFSUPPLIER")
	private String cnicOfSupplier;

	@JsonProperty("CHALLANNUMBER")
	private String challanNumber;
	
	@JsonProperty("PROCUREMENTCATEGORY")
	private String procumentCategory;

	@JsonProperty("PAIDSTATUS")
	private boolean paidStatus;
}
