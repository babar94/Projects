package com.gateway.model.mpay.response.billpayment.pta;


import com.gateway.model.mpay.response.billinquiry.pta.Response;

import jakarta.xml.bind.annotation.XmlRootElement;
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
@XmlRootElement(name = "response")
public class PtaUpdateVoucherResponse {
	private Response response;
}
