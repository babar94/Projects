package com.gateway.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "pg_payment_log")
@Data
public class PgPaymentLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDateTime requestDateTime;
	private LocalDateTime responseDateTime;
	private String rrn;
	private String stan;
	private String responseCode;
	private String responseDesc;
	private String cnic;
	private String mobileNo;
	private String name;
	private String billerId;
	private Integer  amount;
	private Integer  charges;
	private String activity;
	private String transactionStatus;
	private Integer  totalAmount;
	private String billStatus;
	private Integer  retryCount;
	private String voucherId;
	private String paymentChannel;
	private String category;
	private Float othersDiscountValue;
	private String othersDiscountType;
	private Float nbpDiscountValue;
	private String nbpDiscountType;
	private Integer  baflFee;
	private Integer  nbpFee;
	private String toAccount;
	private String fromAccount;
	private String baflPaymentIndicator;

}
