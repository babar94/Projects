package com.gateway.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponseDTO {
    private String translationKey;
    private String locale;
    private String content;
    private List<String> tags;

    // Constructors, getters, setters
}

