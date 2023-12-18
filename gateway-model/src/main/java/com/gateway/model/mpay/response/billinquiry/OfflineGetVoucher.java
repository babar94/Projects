package com.gateway.model.mpay.response.billinquiry;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "response")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@JsonPropertyOrder({ "response_code", "response_desc", "getvoucher" })
public class OfflineGetVoucher {
	private String response_code;
	private String response_desc;
	private Voucher getvoucher;
}
