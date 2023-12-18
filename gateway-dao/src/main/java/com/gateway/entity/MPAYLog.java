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
@Table(name="api_gw_mpay_log")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MPAYLog implements Serializable {

    private static final long serialVersionUID = 3359174562037471207L;

    @Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long ID;
	
	@Column(name = "stamp_date")
	private Date stampDate;

	@Column(name="type",length=100)
	private String type;
	
	@Column(name="username",length=100)
	private String userName;
	
	@Column(name="rrn",length=100)
	private String rrn;
	
	@Column(name="req_res",columnDefinition="text")
	private String reqRes;
	
}
