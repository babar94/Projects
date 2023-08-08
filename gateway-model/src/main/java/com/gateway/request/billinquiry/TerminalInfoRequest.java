package com.gateway.request.billinquiry;

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
public class TerminalInfoRequest implements Serializable {
	private static final long serialVersionUID = 8396436785949897705L;
	private String terminalId;
	private String province;
	private String city;
	private String address;
	private String mobile;
	private String addInfo1;
	private String addInfo2;

}
