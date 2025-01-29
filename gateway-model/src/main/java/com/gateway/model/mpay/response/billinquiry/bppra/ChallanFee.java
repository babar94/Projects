package com.gateway.model.mpay.response.billinquiry.bppra;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChallanFee {

    @JsonProperty("TARIFTITLE")
    private String tariffTitle;

    @JsonProperty("AMOUNT")
    private int amount;
}

