package com.top.application.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class User {

    private String id;
    private String name;
    private String email;
    private String gender;
    private String birthDate;
    private String playerType;
    private String password;
    private List<String> games;

    public User(String id ,String name,String email, String gender, String birthDate, String playerType, String password, List<String> games) {
        this.id = id;
        this.name = name;
        validate(email);
        this.gender = gender;
        this.birthDate = birthDate;
        this.playerType = playerType;
        this.email = email;
        this.password = password;
        this.games = games;
    }

    public void validate(String value) {
        //TO DO
    }

    public void validateList(List<String> favCategories) {
        if (favCategories == null || favCategories.isEmpty()) throw new NullPointerException("Las categorias proporcionadas no tienen el formato adecuado");
    }
}
