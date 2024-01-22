package com.gateway.model.mpay.response.billinquiry.offline;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor

public class Getvoucher implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	@JsonProperty(value = "billingdate")
	private String billingDate;
	@JsonProperty(value = "billingmonth")
	private String billingMonth;
	@JsonProperty(value = "duedate")
	private String dueDate;
	@JsonProperty(value = "amount")
	private String amount;
	@JsonProperty(value = "amountafterduedate")
	private String amountAfterDueDate;
	@JsonProperty(value = "expirydate")
	private String expiryDate;
	@JsonProperty(value = "billstatus")
	private String billStatus;
	@JsonProperty(value = "onebillNumber")
	private String oneBillNumber;

}
