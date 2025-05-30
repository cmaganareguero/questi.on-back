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
    String rol;
    String photo;
    String password;
    List<String> favCategories;
}
