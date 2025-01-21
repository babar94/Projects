package com.gateway.model.mpay.response.billinquiry.slic;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class ResultWrapper {

    @JsonProperty("collection_type")
    private String collectionType;
    
    @JsonProperty("due_amt")
    private String dueAmt;

}
