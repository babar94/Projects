package com.gateway.model.mpay.response.billinquiry.thardeep;

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
public class ThardeepGetVoucher {

  
   @JsonProperty("statusResponse")
   private String statusResponse;

   @JsonProperty("amount")
   private String amount;
  
   @JsonProperty("billingMonth")
   private String billingMonth;

   @JsonProperty("datePaid")
   private String datePaid;
    
   @JsonProperty("amountPaid")
   private String amountPaid;

   @JsonProperty("reserved")
   private String reserved;
   
   @JsonProperty("dueDate")
   private String dueDate;

   @JsonProperty("billStatus")
   private String billStatus;

   @JsonProperty("cnicNo")
   private String cnicNo;

   @JsonProperty("authId")
   private String authId;
   
   @JsonProperty("responseCode")
   private String responseCode;

   @JsonProperty("consumerName")
   private String consumerName;

    
}
	
	



