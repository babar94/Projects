package com.gateway.model.mpay.response.billinquiry.bppra;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.ToString;

import java.util.List;


@Data
@ToString
public class BppraVoucherResponse {

    @JsonProperty("challanData")
    private ChallanData challanData;

    @JsonProperty("challanFee")
    private List<ChallanFee> challanFee;

}

