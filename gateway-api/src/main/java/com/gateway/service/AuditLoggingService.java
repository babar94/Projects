package com.gateway.service;

import java.util.Date;

public interface AuditLoggingService {

	public void auditLog(String activity, String responseCode, String responseDescription, String requestParam,
			String responseParam, Date requestDatetime, Date responseDatetime,String rrn, Long billerId, String billerNumber,String channel,String username) throws Exception;
}
