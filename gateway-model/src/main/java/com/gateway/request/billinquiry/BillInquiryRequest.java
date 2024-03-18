package com.gateway.request.billinquiry;

import java.io.Serializable;

import org.springframework.lang.Nullable;

import jakarta.validation.Valid;
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
public class BillInquiryRequest implements Serializable {

	
	private static final long serialVersionUID = 8396436785949897705L;
	
	
	private InfoRequest info;
	private TxnInfoRequest txnInfo;
	private AdditionalInfoRequest additionalInfo;
	private TerminalInfoRequest terminalInfo;
	
	@Nullable
	private BranchInfo branchInfo;

}
