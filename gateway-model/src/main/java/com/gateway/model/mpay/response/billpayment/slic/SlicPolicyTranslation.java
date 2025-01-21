package com.gateway.model.mpay.response.billpayment.slic;


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
public class SlicPolicyTranslation {

	@JsonProperty("policy_no")
    private String policy_no;

    @JsonProperty("bank_trans_id")
    private String bank_trans_id;

    @JsonProperty("message")
    private String message;

    @JsonProperty("collection_type")
    private String collection_type;
    
    @JsonProperty("status")
    private String status;
	
    @JsonProperty("due_amt")
    private String due_amt;
	
    
}
