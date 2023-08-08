package com.gateway.response.billerlistresponse;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TxnInfoBiller implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8396436785949897705L;

	public ArrayList<Billers> billers;

}
