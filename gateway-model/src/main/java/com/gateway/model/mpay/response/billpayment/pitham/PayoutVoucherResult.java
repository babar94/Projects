package com.gateway.model.mpay.response.billpayment.pitham;

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
public class PayoutVoucherResult {

    @JacksonXmlProperty(localName = "statuscode")
    private String statusCode;

    @JacksonXmlProperty(localName = "statusdesc")
    private String statusDesc;

    
}

