package com.gateway.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Immutable
@Table(name = "combined_payment_log_view")
@Data
public class CombinedPaymentLogView {

	@Id
	private Long logId;

	@Column(name = "name")
	private String name;

	@Column(name = "request_date_time")
	private String requestDateTime;

	@Column(name = "biller_number")
	private String billerNumber;

	@Column(name = "bill_status")
	private String billStatus;

	@Column(name = "activity")
	private String activity;

	@Column(name = "tran_auth_id")
	private String tranAuthId;

	@Column(name = "total_amount")
	private BigDecimal totalAmount;

	@Column(name = "source")
	private String source;

	@Column(name = "biller_id")
	private String billerId;

   //Added on 18-02-2025 (Muhammad Sajid)
	@Column(name = "payment_ref_no")
	private String paymentRefNo;

	@Column(name = "tran_date")
	private String tranDate;

	@Column(name = "tran_time")
	private String tranTime;

//	@Column(name = "nbp_charges_value")
//	private double nbpChargesValue;

//	@Column(name = "others_charges_value")
//	private double othersChargesValue;
}
