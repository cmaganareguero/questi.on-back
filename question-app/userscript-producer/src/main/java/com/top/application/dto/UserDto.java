package com.top.application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    String id;
    String name;
    String email;
    String gender;
    String birthDate;
    String playerType;
    String password;
    List<String> games;

}
