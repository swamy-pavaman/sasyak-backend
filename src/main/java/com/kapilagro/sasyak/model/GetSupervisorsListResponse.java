package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetSupervisorsListResponse {
    private int supervisorId;
    private String supervisorName;

}
