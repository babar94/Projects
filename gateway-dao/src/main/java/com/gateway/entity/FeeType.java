package com.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "nbp_fee_type")
@Data

public class FeeType {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "fees")
	private double fees;

	@Column(name = "fees_type")
	private String feesType;

	@Column(name = "type_detail")
	private String typeDetail;

	@Column(name = "payment_id")
	private Long paymentLogId; 
	
	@Column(name = "source_table")
	private String source;

}
