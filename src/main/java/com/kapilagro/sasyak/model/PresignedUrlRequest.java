package com.kapilagro.sasyak.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignedUrlRequest {
    private String[] fileNames;
    private int expiryHours = 1;
    private String folder;

    // Constructors

}
