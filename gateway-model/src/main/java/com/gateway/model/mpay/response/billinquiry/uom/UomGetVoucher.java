package com.gateway.model.mpay.response.billinquiry.uom;

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
public class UomGetVoucher {

	@JsonProperty("response_Code")
    private String responseCode;
	
	@JsonProperty("consumer_Detail")
    private String consumerDetail;
	
	@JsonProperty("bill_status")
    private String billStatus;
	
	@JsonProperty("due_date")
    private String dueDate;
	
	@JsonProperty("amount_within_dueDate")
    private String amount_Within_DueDate;
	
	@JsonProperty("amount_after_dueDate")
    private String amount_After_DueDate;
	
	@JsonProperty("billing_month")
    private String billing_Month;
	
	@JsonProperty("date_paid")
    private String date_Paid;
	
	@JsonProperty("amount_paid")
    private String amount_Paid;
	
	@JsonProperty("tran_auth_Id")
    private String tran_auth_Id;
	
	@JsonProperty("reserved")
    private String reserved;
	
}


