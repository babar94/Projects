package com.gateway.model.mpay.response.billinquiry.vms;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class VmsGetvoucher implements Serializable {


	private static final long serialVersionUID = 1L;
	
	@JsonProperty(value = "expirydate")
	private String expiryDate;
	@JsonProperty(value = "amount")
	private String amount;
	@JsonProperty(value = "amountafterduedate")
	private String amountAfterDueDate;
	@JsonProperty(value = "billstatus")
	private String billStatus;
	@JsonProperty(value = "duedate")
	private String dueDate;
	@JsonProperty(value = "name")
	private String name;
	@JsonProperty(value = "billingmonth")
	private String billingMonth;
	@JsonProperty(value = "onebillNumber")
	private String oneBillNumber;
	@JsonProperty(value = "billingdate")
	private String billingDate;

}
