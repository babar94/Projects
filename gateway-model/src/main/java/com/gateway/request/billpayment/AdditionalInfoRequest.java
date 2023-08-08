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
public class AdditionalInfoRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8396436785949897705L;
	private String reserveField1;
	private String reserveField2;
	private String reserveField3;
	private String reserveField4;
	private String reserveField5;

}
