package com.gateway.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@Table(name = "mpay_log")
@NoArgsConstructor
@ToString
public class MPAYLog implements Serializable {

	private static final long serialVersionUID = 3359174562037471207L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long Id;

	@Column(name = "stamp_date")
	private Date stampDate;

	@Column(name = "type", length = 100)
	private String type;

	@Column(name = "username", length = 100)
	private String userName;

	@Column(name = "rrn", length = 100)
	private String rrn;

	@Column(name = "req_res", columnDefinition = "text")
	private String reqRes;

}
