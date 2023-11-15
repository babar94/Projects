package com.gateway.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
//@Table(name = "nbp_pgw_payment_log")
@Table(name = "pg_payment_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PaymentLog implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2516248315446279127L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long ID;

	@Column(name = "request_Date_Time")
	private Date requestDatetime;

	@Column(name = "response_Date_Time")
	private Date responsetDatetime;

	@Column(name = "rrn")
	private String rrn;

	@Column(name = "stan")
	private String stan;

	@Column(name = "response_Code")
	private String responseCode;

	@Column(name = "response_Desc")
	private String responseDescription;

	@Column(name = "cnic")
	private String cnic;

	@Column(name = "mobile_No")
	private String mobile;

	@Column(name = "name")
	private String name;

	@Column(name = "consumer_Number")
	private String consumerNumber;

	@Column(name = "biller_Id")
	private String billerId;

	@Column(name = "amount")
	private double amount;

	@Column(name = "charges")
	private double charges;

	@Column(name = "activity")
	private String activity;

	@Column(name = "payment_Ref_No")
	private String paymentRefNo;

	@Column(name = "biller_Number")
	private String billerNumber;

	@Column(name = "transaction_Status")
	private String transactionStatus;

	@Column(name = "address")
	private String address;

	@Column(name = "transaction_Fees")
	private double transactionFees;

	@Column(name = "tax_Amount")
	private double taxAmount;

	@Column(name = "total")
	private double total;

	@Column(name = "channel")
	private String channel;

	@Column(name = "bill_Status")
	private String billStatus;

	@Column(name = "tran_Date")
	private String tranDate;

	@Column(name = "tran_Time")
	private String tranTime;

	@Column(name = "province")
	private String province;

	@Column(name = "tran_auth_id")
	private String tranAuthId;
	
	@Column(name = "retry_count", length = 4)
	private int retryCount;
	
	@Column(name = "voucher_id", length = 10)
	private String voucherId;
	
	@Column(name = "payment_channel", length = 30)
	private String paymentChannel;
	
	@Column(name = "category", length = 30)
	private String category;
	
	@Column(name = "others_discount_value", length = 8)
	private float othersDiscountValue;
	
	@Column(name = "others_discount_type", length = 30)
	private String othersDiscountType;
	
	@Column(name = "nbp_discount_value", length = 8)
	private float nbpDiscountValue;
	
	@Column(name = "nbp_discount_type", length = 25)
	private String nbpDiscountType;
	
	@Column(name = "BAFL_Fee", length = 4)
	private int BaflFee;
	
	@Column(name = "NBP_Fee", length = 4)
	private int NbpFee;
	
	@Column(name = "to_account", length = 30)
	private String toAccount;
	
	@Column(name = "from_account", length = 30)
	private String fromAccount;

}
