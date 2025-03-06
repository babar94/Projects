package com.gateway.model.mpay.response.billinquiry.lesco;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LescoBillinquiryData {

	@JsonProperty("CardType")
    private String cardType;

    @JsonProperty("ReferenceNo")
    private String referenceNo;

    @JsonProperty("RU_code")
    private String ruCode;

    @JsonProperty("DueDate")
    private String dueDate;

    @JsonProperty("BillMonth")
    private String billMonth;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Address")
    private String address;

    @JsonProperty("AmountWithInDue")
    private String amountWithInDueDate;

    @JsonProperty("AmountAfterDue")
    private String amountAfterDueDate;

    @JsonProperty("CustID")
    private String custId;
	
}
