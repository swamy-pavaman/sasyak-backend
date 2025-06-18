package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogRequest {
    private String category;
    private String value;
    private String details;
}