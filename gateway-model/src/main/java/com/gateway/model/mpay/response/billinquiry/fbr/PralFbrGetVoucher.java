package com.gateway.model.mpay.response.billinquiry.fbr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@ToString
public class PralFbrGetVoucher {

	@JsonProperty("response_Code")
	private String responseCode;

	@JsonProperty("amount_paid")
	private String amountPaid;

	@JsonProperty("amount_within_dueDate")
	private String amountWithinDueDate;

	@JsonProperty("consumer_Detail")
	private String consumerDetail;

	@JsonProperty("date_paid")
	private String datePaid;

	@JsonProperty("reserved")
	private String reserved;

	@JsonProperty("tran_auth_id")
	private String tranAuthId;

	@JsonProperty("amount_after_dueDate")
	private String amountAfterDueDate;

	@JsonProperty("due_date")
	private String dueDate;

	@JsonProperty("billing_month")
	private String billingMonth;

	@JsonProperty("bill_status")
	private String billStatus;

	private String oneBillNumber;
}
