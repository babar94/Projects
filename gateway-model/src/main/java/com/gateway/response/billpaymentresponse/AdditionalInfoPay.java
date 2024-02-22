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

public class AdditionalInfoPay implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8396436785949897705L;
	private String reserveField1;
	private String reserveField2;
	private String reserveField3;
	private String reserveField4;
	private String reserveField5;
	private String reserveField6;
	private String reserveField7;
	private String reserveField8;
	private String reserveField9;
	private String reserveField10;

	public AdditionalInfoPay(String reserveField1, String reserveField2, String reserveField3, String reserveField4,
			String reserveField5) {
		super();
		this.reserveField1 = reserveField1;
		this.reserveField2 = reserveField2;
		this.reserveField3 = reserveField3;
		this.reserveField4 = reserveField4;
		this.reserveField5 = reserveField5;
	}

}
