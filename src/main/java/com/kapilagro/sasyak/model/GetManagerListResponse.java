package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetManagerListResponse {
    private int manageId;
    private String managerName;

}
