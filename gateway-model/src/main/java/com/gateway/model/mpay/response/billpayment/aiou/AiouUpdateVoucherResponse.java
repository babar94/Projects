package com.gateway.model.mpay.response.billpayment.aiou;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gateway.model.mpay.response.billpayment.Response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "response")
public class AiouUpdateVoucherResponse {

	private Response response;
}
