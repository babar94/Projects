package com.gateway.service.impl;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gateway.entity.ReservedFieldsMapping;
import com.gateway.repository.ReservedFieldMappingRepository;
import com.gateway.request.billpayment.BillPaymentRequest;
import com.gateway.service.ReservedFieldsValidationService;

@Service
public class ReservedFieldsValidationServiceImpl implements ReservedFieldsValidationService {

	 private static final Logger logger = LoggerFactory.getLogger(ReservedFieldsValidationServiceImpl.class);

	    @Autowired
	    private ReservedFieldMappingRepository reservedFieldMappingRepository;

	    @Override
	    public boolean validateReservedFields(BillPaymentRequest billPaymentRequest, String billerid) {

	        List<ReservedFieldsMapping> reservedFieldsMappings = reservedFieldMappingRepository
	                .findByBillerId(billerid);

	        Map<String, String> reservedFieldsMap = reservedFieldsMappings.stream()
	                .collect(Collectors.toMap(ReservedFieldsMapping::getReservedField, ReservedFieldsMapping::getRegex));

	        long matchedFieldsCount = reservedFieldsMap.entrySet().stream()
	                .filter(entry -> {
	                    String reservedField = entry.getKey();
	                    String requestValue = getRequestValue(billPaymentRequest, reservedField.trim());
	                    String regex = entry.getValue();
	                    return requestValue != null && Pattern.matches(regex, requestValue);
	                })
	                .count();

	        boolean validationPassed = matchedFieldsCount == reservedFieldsMap.size();

	        if (!validationPassed) {
	            logger.error("Validation failed. All reserved fields must match the regex pattern.");
	        } else {
	            logger.info("Validation passed. All reserved fields match the regex pattern."); 
	        }

	        return validationPassed;
	    }

	    // Helper method to get the value for a reserved field from BillPaymentRequest
	    private String getRequestValue(BillPaymentRequest billPaymentRequest, String reservedField) {
	        switch (reservedField) {
	            case "reserveField1":
	                return billPaymentRequest.getAdditionalInfo().getReserveField1();
	            case "reserveField2":
	                return billPaymentRequest.getAdditionalInfo().getReserveField2();
	            case "reserveField3":
	                return billPaymentRequest.getAdditionalInfo().getReserveField3();
	            case "reserveField4":
	                return billPaymentRequest.getAdditionalInfo().getReserveField4();
	            case "reserveField5":
	                return billPaymentRequest.getAdditionalInfo().getReserveField5();
	            case "reserveField6":
	                return billPaymentRequest.getAdditionalInfo().getReserveField6();
	            case "reserveField7":
	                return billPaymentRequest.getAdditionalInfo().getReserveField7();
	            case "reserveField8":
	                return billPaymentRequest.getAdditionalInfo().getReserveField8();
	            case "reserveField9":
	                return billPaymentRequest.getAdditionalInfo().getReserveField9();
	            case "reserveField10":
	                return billPaymentRequest.getAdditionalInfo().getReserveField10();
	            default:
	                return null;
	        }
	    }
}
