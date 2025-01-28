package com.gateway.model.mpay.response.billinquiry.bppra;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ChallanData {

    @JsonProperty("ID")
    private String id;

    @JsonProperty("NAME")
    private String name;

    @JsonProperty("ADDRESS")
    private String address;

    @JsonProperty("BANKNAME")
    private String bankName;

    @JsonProperty("ACCOUNT")
    private String account;

    @JsonProperty("CATEGORYNAME")
    private String categoryName;

    @JsonProperty("NOTE")
    private String note;

    @JsonProperty("TSE")
    private String tse;

    @JsonProperty("PANAME")
    private String paName;

    @JsonProperty("TENDERTITLE")
    private String tenderTitle;

    @JsonProperty("CHALLANNUMBER")
    private String challanNumber;

    @JsonProperty("PAIDSTATUS")
    private boolean paidStatus;

  
}


