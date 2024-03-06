package com.gateway.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "payment_log", indexes = { @Index(columnList = "rrn", name = "IDX_rrn"),
		@Index(columnList = "billerNumber, billStatus", name = "IDX_billerNumber_billStatus"),

})
@Data
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

	@Column(name = "amountPaid")
	private BigDecimal amountPaid;
	
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

	// new added on 22-02-24

	@Column(name = "amount_withinduedate")
	private BigDecimal amountwithinduedate;

	@Column(name = "amount_afterduedate")
	private BigDecimal amountafterduedate;
	
	@Column(name = "duedate")
	private String duedate;
	

}
