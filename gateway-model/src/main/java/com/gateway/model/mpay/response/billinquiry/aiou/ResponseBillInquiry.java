package com.gateway.model.mpay.response.billinquiry.aiou;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseBillInquiry {

	@JsonProperty("ResponseCode")
	private String responseCode;

	@JsonProperty("ValidityDate")
	private String validityDate;

	@JsonProperty("Semester")
	private String semester;

	@JsonProperty("ChallanNumber")
	private String challanNumber;

	@JsonProperty("STAN")
	private String stan;

	@JsonProperty("IssueDate")
	private String issueDate;

	@JsonProperty("AmountAfterDueDate")
	private String amountAfterDueDate;

	@JsonProperty("Name")
	private String name;

	@JsonProperty("Programme")
	private String programme;

	@JsonProperty("Date_Paid")
	private String datePaid;

	@JsonProperty("RollNumber")
	private String rollNumber;

	@JsonProperty("ContactNumber")
	private String contactNumber;

	@JsonProperty("CNIC")
	private String cnic;

	@JsonProperty("RegistrationNumber")
	private String registrationNumber;

	@JsonProperty("AmountWithinDueDate")
	private String amountWithinDueDate;

	@JsonProperty("DueDate")
	private String dueDate;

	@JsonProperty("Bill_Status")
	private String billStatus;

	@JsonProperty("FatherName")
	private String fatherName;

	@JsonProperty("Amount_Paid")
	private String amountPaid;

	private String oneBillNumber;
}
