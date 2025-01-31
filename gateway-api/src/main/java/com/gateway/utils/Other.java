package com.gateway.utils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Other {
    public static void main(String[] args) {
        String filePath = "src/main/resources/public_key.pem";

        try {
            // Read file content as a string
            String pemContent = new String(Files.readAllBytes(Paths.get(filePath)));

            // Regular expression to match the PEM header and footer
            String regex = "-----BEGIN [A-Za-z0-9 ]+-----|-----END [A-Za-z0-9 ]+-----";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(pemContent);

            // Remove the header and footer
            String cleanedPemContent = matcher.replaceAll("").replaceAll("\\s+", "");

            // Print the cleaned content
            System.out.println(cleanedPemContent);
        } catch (IOException e) {
            System.err.println("Error reading the PEM file: " + e.getMessage());
        }
    }
}

