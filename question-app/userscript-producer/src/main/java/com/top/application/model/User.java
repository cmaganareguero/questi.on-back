package com.top.application.model;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Data
@NoArgsConstructor
public class User {

    private String id;
    private String name;
    private String email;
    private String password;

    public void validate(String value) {
        //TO DO
    }

    public void validateList(List<String> favCategories) {
        if (favCategories == null || favCategories.isEmpty()) throw new NullPointerException("Las categorias proporcionadas no tienen el formato adecuado");
    }
}
