package com.gateway.model.mpay.response.billinquiry.pitham;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

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
public class PithmGetVoucher {

	@JacksonXmlProperty(localName = "getinquiryresult")
    private GetInquiryResult getInquiryResult;
}


