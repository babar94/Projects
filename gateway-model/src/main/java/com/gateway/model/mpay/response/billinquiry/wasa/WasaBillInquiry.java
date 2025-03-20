package com.gateway.model.mpay.response.billinquiry.wasa;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class WasaBillInquiry {

	@JsonProperty("BillAmount")
	private String billAmount;
	
	@JsonProperty("ReceivedAmount")
	private String receivedAmount;
	
	@JsonProperty("BillAmountAfterDueDate")
	private String billAmountAfterDueDate;
	
	@JsonProperty("Transaction")
	private String transaction;
	
	@JsonProperty("ConsumerName")
	private String consumername;
	
	@JsonProperty("ConsumerNumber")
	private String consumerNumber;
	
	@JsonProperty("Period")
	private String period;
	
	@JsonProperty("DueDate")
	private String dueDate;
}
