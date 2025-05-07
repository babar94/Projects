//package com.gateway.utils;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.gateway.entity.ReservedFieldAttributes;
//
//import java.lang.reflect.Field;
//import java.util.LinkedHashMap;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class ReservedAttributeMapper {
//
//	private static final Logger LOG = LoggerFactory.getLogger(ReservedAttributeMapper.class);
//
//	public static LinkedHashMap<String, String> populateReservedFieldsFromResponse(
//			ReservedFieldAttributes reservedAttributes, Object inquiry) {
//
//		LinkedHashMap<String, String> reservedMap = new LinkedHashMap<>();
//		for (int i = 1; i <= 10; i++) {
//			try {
//				Field field = ReservedFieldAttributes.class.getDeclaredField("reservedField" + i);
//				field.setAccessible(true);
//				Object fieldNameObj = field.get(reservedAttributes);
//
//				String fieldName = fieldNameObj != null ? fieldNameObj.toString() : "";
//
//				String valueFromResponse = getFieldValueFromResponse(inquiry, fieldName);
//
//				LOG.info("reservedField" + i + " (" + fieldName + ") => "
//						+ (valueFromResponse != null ? valueFromResponse : ""));
//
//				reservedMap.put(field.getName(), valueFromResponse);
//
//				LOG.info("ResevedFiledValues:" + reservedMap);
//
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//
//		return reservedMap;
//	}
//
//	
//	 public static String getFieldValueFromResponse(Object responseObject, String reservedFieldName) {
//
//	        if (responseObject instanceof List<?>) {
//	            List<?> responseList = (List<?>) responseObject;
//
//	            for (Object item : responseList) {
//	                String value = getFieldValueFromSingleObject(item, reservedFieldName);
//	                if (value != null) {
//	                    return value;
//	                }
//	            }
//	        } else {
//	            return getFieldValueFromSingleObject(responseObject, reservedFieldName);
//	        }
//
//	        return null;
//	    }
//
//	    private static String getFieldValueFromSingleObject(Object object, String reservedFieldName) {
//	        for (Field field : object.getClass().getDeclaredFields()) {
//	            LOG.info("Field Name: " + field.getName());
//
//	            JsonProperty jsonProp = field.getAnnotation(JsonProperty.class);
//	            if (jsonProp != null) {
//	                LOG.info("jsonProp: " + jsonProp.value());
//
//	                if (jsonProp.value().equalsIgnoreCase(reservedFieldName)) {
//	                    field.setAccessible(true);
//	                    try {
//	                        Object value = field.get(object);
//	                        return value != null ? value.toString() : null;
//	                    } catch (IllegalAccessException e) {
//	                        e.printStackTrace();
//	                    }
//	                }
//	            }
//	        }
//	        return null;
//	    }
//	}
//	
//	
//	
//	
//	
//	
////	public static String getFieldValueFromResponse(Object responseObject, String reservedFieldName) {
////
////		for (Field field : responseObject.getClass().getDeclaredFields()) {
////			
////			LOG.info("Field Name :" + field.getName());
////
////			JsonProperty jsonProp = field.getAnnotation(JsonProperty.class);
////			LOG.info("jsonProp" + jsonProp.value());
////			if (jsonProp != null && jsonProp.value().equalsIgnoreCase(reservedFieldName)) {
////				field.setAccessible(true);
////				try {
////					Object value = field.get(responseObject);
////					return value != null ? value.toString() : null;
////				} catch (IllegalAccessException e) {
////					e.printStackTrace();
////				}
////			}
////		}
////		return null;
////	}
//
////}
