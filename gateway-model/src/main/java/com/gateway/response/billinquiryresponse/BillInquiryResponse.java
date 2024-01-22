package com.gateway.response.billinquiryresponse;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@NoArgsConstructor
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillInquiryResponse implements Serializable {

	private static final long serialVersionUID = 8225334744231345997L;

	private Info info;
	private TxnInfo txnInfo;
	private AdditionalInfo additionalInfo;

}
