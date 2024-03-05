package com.gateway.model.mpay.response.billinquiry.pitham;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JacksonXmlRootElement(localName = "response")
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PithamGetVoucherResponse  {

	
	@JacksonXmlProperty(localName = "response_code")
    private String responseCode;

	@JacksonXmlProperty(localName= "response_desc")
    private String responseDesc;

	@JacksonXmlProperty(localName= "tran_ref")
    private String tranRef;
	
	@JacksonXmlProperty(localName= "pithm-getvoucher")
    private PithmGetVoucher pithmGetVoucher;

	    
}
