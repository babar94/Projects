package com.gateway.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "translations", uniqueConstraints = @UniqueConstraint(columnNames = {"translationKey", "locale"}))
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String translationKey;

    private String locale;

    private String content;

    @OneToMany(mappedBy = "translation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TranslationTag> tags = new ArrayList<>();
}
