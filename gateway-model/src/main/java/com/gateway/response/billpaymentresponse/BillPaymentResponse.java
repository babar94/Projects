package com.gateway.response.billpaymentresponse;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)

public class BillPaymentResponse implements Serializable {

	private static final long serialVersionUID = 8225334744231345997L;

	private InfoPay info;
	private TxnInfoPay txnInfo;
	private AdditionalInfoPay additionalInfo;
	
	
//	public BillPaymentResponse(InfoPay info, AdditionalInfoPay additionalInfo) {
//		super();
//		this.info = info;
//		this.additionalInfo = additionalInfo;
//	}


	
	
	
	

}
