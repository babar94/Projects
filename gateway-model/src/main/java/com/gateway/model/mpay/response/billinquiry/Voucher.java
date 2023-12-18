package com.gateway.model.mpay.response.billinquiry;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@JsonPropertyOrder({ "name", "billingdate", "billingmonth", "duedate", "amount", "amountafterduedate", "expirydate" })
public class Voucher {

	private String name;
	private String billingdate;
	private String billingmonth;
	private String duedate;
	private String amount;
	private String amountafterduedate;
	private String expirydate;
	private String status;

}
