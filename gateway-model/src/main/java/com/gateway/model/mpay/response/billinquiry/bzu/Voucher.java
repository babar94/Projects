package com.gateway.model.mpay.response.billinquiry.bzu;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class Voucher {
	
	@JsonProperty("voucher_no")
    private String voucherNo;

    @JsonProperty("issue_date")
    private String issueDate;

    @JsonProperty("due_date")
    private String dueDate;

    @JsonProperty("total_fees")
    private String totalFees;

    @JsonProperty("voucher_type")
    private String voucherType;

    @JsonProperty("student_name")
    private String studentName;

    @JsonProperty("student_fname")
    private String studentFname;

    @JsonProperty("scnic")
    private String scnic;

    @JsonProperty("roll_no")
    private String rollNo;

}
