package com.gateway.model.mpay.response.billinquiry;

import java.io.Serializable;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Getvoucher implements Serializable {
	private static final long serialVersionUID = 8396436785949897705L;
	public ArrayList<Fees> fees;
	private String total;
	private String address;
	private String date_of_birth;
	private String name;
	private String mobile;
	private String voucher_id;
	private String cnic;
	private String status;
	private String oneBillNumber;

}
