package com.gateway.request.billpayment;

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
public class TxnInfoPayRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8396436785949897705L;
	private String aggregatorId;
	private String billerId;
	private String billNumber;
	private String tranDate;
	private String tranTime;
	private String tranAuthId;
	private String tranAmount;

}
