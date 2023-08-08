package com.gateway;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Test {

//	public static void main(String[] args) {
//		double dbAmount = Double.parseDouble("7200");
//		
//		double transactionFees = 50;
//		System.out.println("Transaction Fees:"+transactionFees);
//		
//		double taxPercent =  2.5;
//		
//		double dbTax = ((taxPercent/100) * transactionFees);
//		
//		System.out.println("Tax:"+dbTax);
//		
//		double dbTransactionFees =(transactionFees+dbTax);
//		System.out.println("Transaction Fees:"+dbTransactionFees);
//		
//		double dbTotal=(dbAmount + dbTransactionFees);
//		System.out.println("Total:"+dbTotal);
//
//	}
	public static void main(String[] args) {
		String date = "20220725";
		Date today=null;		
		 try {
			 today=new SimpleDateFormat("yyyyMMdd").parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  

        Calendar calendar = Calendar.getInstance();  
        calendar.setTime(today);  

        calendar.add(Calendar.MONTH, 1);  
        calendar.set(Calendar.DAY_OF_MONTH, 1);  
        calendar.add(Calendar.DATE, -1);  

        Date lastDayOfMonth = calendar.getTime();  

        DateFormat sdf = new SimpleDateFormat("yyyyMMdd");  
        System.out.println("Today            : " + sdf.format(today));  
        System.out.println("Last Day of Month: " + sdf.format(lastDayOfMonth));  
		}
		

	

	
}
