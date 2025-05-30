package com.top.application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReduceRequestDto {

    String name;
    String email;
    String password;
}
