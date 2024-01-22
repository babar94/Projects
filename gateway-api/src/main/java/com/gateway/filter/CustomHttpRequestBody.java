//package com.gateway.filter;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.util.stream.Collectors;
//
//import jakarta.servlet.ReadListener;
//import jakarta.servlet.ServletInputStream;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletRequestWrapper;
//
//public class CustomHttpRequestBody extends HttpServletRequestWrapper {
//
//	private String body;
//
//	public String getBody() {
//		return body;
//	}
//
//	public CustomHttpRequestBody(HttpServletRequest request) throws IOException {
//
//		super(request);
//		body = request.getReader().lines().collect(Collectors.joining());
//	}
//
//	@Override
//	public ServletInputStream getInputStream() throws IOException {
//		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
//		ServletInputStream servletInputStream = new ServletInputStream() {
//			public int read() throws IOException {
//				return byteArrayInputStream.read();
//			}
//
//			@Override
//			public boolean isFinished() {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public boolean isReady() {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public void setReadListener(ReadListener listener) {
//				// TODO Auto-generated method stub
//
//			}
//		};
//		return servletInputStream;
//	}
//}