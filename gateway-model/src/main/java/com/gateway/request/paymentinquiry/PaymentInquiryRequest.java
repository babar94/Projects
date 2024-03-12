package com.gateway.request.paymentinquiry;

import java.io.Serializable;

import org.springframework.lang.Nullable;

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
public class PaymentInquiryRequest implements Serializable {

	
	private static final long serialVersionUID = 8396436785949897705L;
	private InfoPayInqRequest info;
	private TxnInfoPayInqRequest txnInfo;
	private AdditionalInfoPayInqRequest additionalInfo;
	@Nullable
	private BranchInfo branchInfo;

}
