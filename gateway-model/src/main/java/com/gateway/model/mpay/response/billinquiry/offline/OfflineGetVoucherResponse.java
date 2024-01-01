package com.gateway.model.mpay.response.billinquiry.offline;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@XmlRootElement(name = "response")
public class OfflineGetVoucherResponse {

	private Response response;
}
