package com.gateway.utils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PemReader {
    public static void main(String[] args) {
        try {
            // Get the absolute path of the file
            String filePath = "src/main/resources/public_key.pem";
            
            // Read file content as a string
            String pemContent = new String(Files.readAllBytes(Paths.get(filePath)));
            String singleLinePem = pemContent.replace("\n", " ").replace("\r", " ");
            // Print the content
            System.out.println(singleLinePem);
        } catch (IOException e) {
            System.err.println("Error reading the PEM file: " + e.getMessage());
        }
    }
}

