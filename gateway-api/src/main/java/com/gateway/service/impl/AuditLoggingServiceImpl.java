//package com.gateway.service.impl;
//
//import java.util.Date;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//import com.gateway.entity.AuditLog;
//import com.gateway.repository.AuditLogRepository;
//import com.gateway.service.AuditLoggingService;
//
//@Service
//public class AuditLoggingServiceImpl implements AuditLoggingService {
//
//	private static final Logger LOG = LoggerFactory.getLogger(AuditLoggingServiceImpl.class);
//
//	@Autowired
//	private AuditLogRepository auditLogRepository;
//
//	@Async("auditLoggingExecutor")
//	public void auditLog(String activity, String responseCode, String responseDescription, String requestParam,
//			String responseParam, Date requestDatetime, Date responseDatetime, String rrn, String billerId,
//			String billerNumber, String channel, String username) throws Exception {
//
//		AuditLog auditLog = new AuditLog();
//		LOG.info("Insertin in table (audit)");
//
//		
//		
//		auditLog.setActivity(activity);
//		auditLog.setRequestParam(requestParam);
//		auditLog.setResponseParam(responseParam);
//		auditLog.setResponseCode(responseCode);
//		auditLog.setResponseDescription(responseDescription);
//		auditLog.setRequestDatetime(requestDatetime);
//		auditLog.setResponsetDatetime(requestDatetime);
//		auditLog.setRrn(rrn);
//		auditLog.setBillerId(billerId);
//		auditLog.setBillerNumber(billerNumber);
//		auditLog.setChannel(channel);
//		auditLog.setUsername(username);
//
//		auditLogRepository.save(auditLog);
//
//		LOG.info("Inserted in table (audit)");
//
//	}
//
//}
