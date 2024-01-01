package com.gateway.model.mpay.response.billinquiry.offline;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Getvoucher implements Serializable {
	private String name;
	private String billingdate;
	private String billingmonth;
	private String duedate;
	private String amount;
	private String amountafterduedate;
	private String expirydate;
	private String billstatus;

}
