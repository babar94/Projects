package com.gateway.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data

@Table(name = "pending_payments")
public class PendingPayment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int paymentId;

	private String orderId;

	private String voucherId;

	private LocalDateTime transactionDate;

	private Boolean ignoreTimer;
	
	@Column(name = "biller_id")
	private String billerId;

}