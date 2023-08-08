package com.gateway.request.billpayment;

import java.io.Serializable;

import com.gateway.request.billinquiry.AdditionalInfoRequest;

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
public class BillPaymentRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8396436785949897705L;

	private InfoPayRequest info;
	private TxnInfoPayRequest txnInfo;
	private AdditionalInfoRequest additionalInfo;
	private TerminalInfoPayRequest terminalInfo;

}
