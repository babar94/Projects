package com.gateway.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gateway.entity.ProvinceTransaction;
import com.gateway.response.billinquiryresponse.AdditionalInfo;
import com.gateway.response.billinquiryresponse.Info;
import com.gateway.response.billinquiryresponse.TxnInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillPaymentValidationResponse implements Serializable {

	private static final long serialVersionUID = 8225334744231345997L;

	private String responseCode;
	private String responseDesc;
	private String username;
	private String channel;
	private ProvinceTransaction provinceTransaction;
	private String rrn;
	private String stan;

	public BillPaymentValidationResponse(String responseCode, String responseDesc, String rrn, String stan) {
		super();
		this.responseCode = responseCode;
		this.responseDesc = responseDesc;
		this.rrn = rrn;
		this.stan = stan;
	}

}
