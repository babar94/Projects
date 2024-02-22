package com.gateway.response.billinquiryresponse;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)

public class AdditionalInfo implements Serializable {

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

	public AdditionalInfo(String reserveField1, String reserveField2, String reserveField3, String reserveField4,
			String reserveField5) {
		super();
		this.reserveField1 = reserveField1;
		this.reserveField2 = reserveField2;
		this.reserveField3 = reserveField3;
		this.reserveField4 = reserveField4;
		this.reserveField5 = reserveField5;
	}

	public AdditionalInfo(String reserveField1, String reserveField2, String reserveField3, String reserveField4,
			String reserveField5, String reserveField6, String reserveField7, String reserveField8,
			String reserveField9, String reserveField10) {
		super();
		this.reserveField1 = reserveField1;
		this.reserveField2 = reserveField2;
		this.reserveField3 = reserveField3;
		this.reserveField4 = reserveField4;
		this.reserveField5 = reserveField5;
		this.reserveField6 = reserveField6;
		this.reserveField7 = reserveField7;
		this.reserveField8 = reserveField8;
		this.reserveField9 = reserveField9;
		this.reserveField10 = reserveField10;
	}

}
