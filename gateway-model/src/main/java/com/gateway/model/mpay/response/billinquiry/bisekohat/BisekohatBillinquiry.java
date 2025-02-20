package com.gateway.model.mpay.response.billinquiry.bisekohat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BisekohatBillinquiry {

	@JsonProperty("p_ConsumerNumber")
	private String consumerNumber;
	
	@JsonProperty("p_CustomerName")
	private String customerName;
	
	@JsonProperty("p_Amount")
	private String amount;
	
	@JsonProperty("p_Purpose")
	private String purpose;
	
	@JsonProperty("p_BillingMonth")
	private String billingMonth;
	
	@JsonProperty("p_DueDate")
	private String dueDate;
	
	@JsonProperty("p_AmountAfterDueDate")
	private String amountAfterDueDate;

}
