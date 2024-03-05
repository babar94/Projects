package com.gateway.model.mpay.response.billinquiry.pitham;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

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
public class GetInquiryResult {

    @JacksonXmlProperty(localName = "amount_ad_date")
    private String amountAdDate;

    @JacksonXmlProperty(localName = "amount_wid_date")
    private String amountWidDate;

    @JacksonXmlProperty(localName = "billingmonth")
    private String billingMonth;

    @JacksonXmlProperty(localName = "status")
    private String status;

    @JacksonXmlProperty(localName = "statusdesc")
    private String statusDesc;

    @JacksonXmlProperty(localName = "transactiondate")
    private String transactionDate;

    @JacksonXmlProperty(localName = "voucherno")
    private String voucherNo;

    @JacksonXmlProperty(localName = "due_date")
    private String dueDate;

    @JacksonXmlProperty(localName = "student_id")
    private String studentId;

    @JacksonXmlProperty(localName = "student_name")
    private String studentName;
}

