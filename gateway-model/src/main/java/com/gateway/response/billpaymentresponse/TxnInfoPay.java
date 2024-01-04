package com.gateway.response.billpaymentresponse;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@NoArgsConstructor
@Setter
@Getter
@ToString
@AllArgsConstructor
public class TxnInfoPay implements Serializable {

	private static final long serialVersionUID = 8396436785949897705L;
	private String billerId;
	private String billNumber;
	private String paymentRefno;
	//private String identificationParameter;


	
	

	
	
	
	
	
	
	

	
}
