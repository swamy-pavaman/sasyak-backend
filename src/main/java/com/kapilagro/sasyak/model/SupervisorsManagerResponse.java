package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupervisorsManagerResponse {
    String managerName;
    String email;
}
