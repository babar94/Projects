package com.gateway.model.mpay.response.billinquiry.bisekohat;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class NullStringToNullDeserializer extends JsonDeserializer<BiseKohatBillinquiryData> {
    @Override
    public BiseKohatBillinquiryData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        // Convert "null" string to actual null
        if ("null".equals(value)) {
            return null;
        }
        return ctxt.readValue(p, BiseKohatBillinquiryData.class); // Normal JSON processing
    }
}


