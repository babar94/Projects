package com.gateway.model.mpay.response.billinquiry.dls;

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
public class DlsGetVoucher {
	
	@JsonProperty("branchCode")
	private String branchCode;

	@JsonProperty("createdAt")
	private String createdAt ;

	@JsonProperty("amount")
	private String amount;

	@JsonProperty("name")
	private String name;
	
	@JsonProperty("branchName")
	private String branchName;
	
	@JsonProperty("feeTypesList_wrapper")
	private List<FeeTypeListWrapper> feeTypesList_wrapper;
	
	@JsonProperty("id")
	private String id;
	
	@JsonProperty("cnic")
    private String cnic;
	
	@JsonProperty("mobile_number")
    private String mobile_number;
    
	@JsonProperty("email")
	private String email;
    
	@JsonProperty("status")
    private String status;
    
	@JsonProperty("updatedAt")
    private String updatedAt;

}
