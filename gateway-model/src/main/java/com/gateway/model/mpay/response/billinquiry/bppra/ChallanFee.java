package com.gateway.model.mpay.response.billinquiry.bppra;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class ChallanFee {

    @JsonProperty("TARIFTITLE")
    private String tariffTitle;

    @JsonProperty("AMOUNT")
    private int amount;
}

