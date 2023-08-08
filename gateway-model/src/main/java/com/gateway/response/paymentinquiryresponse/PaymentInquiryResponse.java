package com.gateway.response.paymentinquiryresponse;

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
public class PaymentInquiryResponse implements Serializable {

	private static final long serialVersionUID = 8225334744231345997L;

	private InfoPayInq info;
	private TxnInfoPayInq txnInfo;
	private AdditionalInfoPayInq additionalInfo;

}
