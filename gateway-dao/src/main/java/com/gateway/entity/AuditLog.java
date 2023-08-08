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
@Table(name = "nbp_pgw_audit_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
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
	private Long billerId;
	
	@Column(name = "biller_Number")
	private String billerNumber;
	
	@Column(name = "channel")
	private String channel;
	
	@Column(name = "username")
	private String username;

}
