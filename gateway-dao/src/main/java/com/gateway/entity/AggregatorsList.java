package com.gateway.entity;

import java.io.Serializable;

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
@Table(name = "nbp_pgw_aggregators")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AggregatorsList implements Serializable {

	private static final long serialVersionUID = -2516248315446279127L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "aggregator_id")
	private String aggregatorId;

	@Column(name = "aggregator_name")
	private String aggregatorName;
	
}