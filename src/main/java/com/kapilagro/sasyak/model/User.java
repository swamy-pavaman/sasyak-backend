package com.kapilagro.sasyak.model;

import lombok.*;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
@Getter
@Setter
@Builder
public class User {

    int userId;
    String name;
    String email;
    String number;
    String role;
    String password;
    String oauthProvider;
    String oAuthProviderId;



}
