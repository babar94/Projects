package com.gateway.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "api_gw_audit_log")
@Data

@NoArgsConstructor

public class AuditLog implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2516248315446279127L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long ID;

	@Column(name = "activity", length = 100)
	private String activity;

	@Column(name = "request_param", columnDefinition = "text")
	private String requestParam;

	@Column(name = "response_param", columnDefinition = "text")
	private String responseParam;

	@Column(name = "response_code")
	private String responseCode;

	@Column(name = "response_description")
	private String responseDescription;

	@Column(name = "request_datetime")
	private Date requestDatetime;

	@Column(name = "response_datetime")
	private Date responsetDatetime;

	@Column(name = "rrn")
	private String rrn;

	@Column(name = "biller_Id")
	private String billerId;

	@Column(name = "biller_Number")
	private String billerNumber;

	@Column(name = "channel")
	private String channel;

	@Column(name = "username")
	private String username;

}
