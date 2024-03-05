package com.gateway.model.mpay.response.billpayment.pitham;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JacksonXmlRootElement(localName = "response")
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@ToString
public class PithamUpdateVoucherResponse implements Serializable {
	
	private static final long serialVersionUID = 8396436785949897705L;
	
	@JacksonXmlProperty(localName = "response_code")
    private String responseCode;

	@JacksonXmlProperty(localName = "response_desc")
    private String responseDesc;

	@JacksonXmlProperty(localName = "tran_ref")
    private String tranRef;
	
	@JacksonXmlProperty(localName = "pithm-updatevoucher")
    private PithmUpdateVoucher pithmUpdateVoucher;
    
}
