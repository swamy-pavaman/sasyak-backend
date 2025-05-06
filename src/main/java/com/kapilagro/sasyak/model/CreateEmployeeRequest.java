package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateEmployeeRequest {
    private String name;
    private String email;
    //private String password;
    private String phone_number;
    private String companyName;
    private String role; // EMPLOYEE, MANAGER, SUPERVISOR
    private int managerId;
}
