package com.gateway.response.billinquiryresponse;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OneLinkBillInquiryResponse implements Serializable {

	private static final long serialVersionUID = 8225334744231345997L;

	@JsonProperty(value="response_Code")
	private String responseCode;
	@JsonProperty(value="consumer_Detail")
	private String consumerDetail;
	@JsonProperty(value="bill_status")
    private String billStatus;
	@JsonProperty(value="due_date")
    private String dueDate;
	@JsonProperty(value="amount_within_dueDate")
    private String amountWithinDueDate;
	@JsonProperty(value="amount_after_dueDate")
    private String amountAfterDueDate;
	@JsonProperty(value="billing_month")
    private String billingMonth;
	@JsonProperty(value="date_paid")
    private String datePaid;
	@JsonProperty(value="amount_paid")
    private String amountPaid;
	@JsonProperty(value="tran_auth_Id")
    private String tranAuthId;
	@JsonProperty(value="reserved")
    private String reserved;
	
	@JsonProperty(value="response_description")
    private String responseDescription;

//	public OneLinkBillInquiryResponse(String responseCode, String responseDescription) {
//		super();
//		this.responseCode = responseCode;
//		this.responseDescription = responseDescription;
//	}
	
	
	
	

}
