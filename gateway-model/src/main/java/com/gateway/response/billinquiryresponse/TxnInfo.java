package com.gateway.response.billinquiryresponse;

import java.io.Serializable;

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
public class TxnInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8396436785949897705L;
	private String billerId;
	private String billNumber;
	private String consumerName;
	private String billStatus;
	private String dueDate;
	private String amountwithinduedate;
	private String amountafterduedate;
	private String billingMonth;
	private String tranAuthId;
	private String datePaid;
	private String amountPaid;
	
	
	
}
