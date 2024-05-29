package com.gateway.model.mpay.response.billinquiry.dls;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@XmlRootElement(name = "response")
public class DlsGetVoucherResponse {

	private Response response;

}
