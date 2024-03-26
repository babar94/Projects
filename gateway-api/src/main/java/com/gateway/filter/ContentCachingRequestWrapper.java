//package com.gateway.filter;
//
//import java.io.BufferedReader;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//
//import jakarta.servlet.ServletInputStream;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletRequestWrapper;
//
//public class ContentCachingRequestWrapper extends HttpServletRequestWrapper {
//    private final ByteArrayOutputStream cachedContent;
//    private ServletInputStream inputStream;
//    private BufferedReader reader;
//
//    public ContentCachingRequestWrapper(HttpServletRequest request) throws IOException {
//        super(request);
//        this.cachedContent = new ByteArrayOutputStream();
//    }
//
//    @Override
//    public ServletInputStream getInputStream() throws IOException {
//        if (inputStream == null) {
//            inputStream = new ContentCachingInputStream(getRequest().getInputStream());
//        }
//        return inputStream;
//    }
//
//    @Override
//    public BufferedReader getReader() throws IOException {
//        if (reader == null) {
//            reader = new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
//        }
//        return reader;
//    }
//
//    public byte[] getContentAsByteArray() {
//        return this.cachedContent.toByteArray();
//    }
//
//    // Other methods...
//}
