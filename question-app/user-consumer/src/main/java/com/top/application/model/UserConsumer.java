package com.top.application.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Data
@NoArgsConstructor
@Document(collection = "User")
@Builder
@AllArgsConstructor
public class UserConsumer {
    @Id
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
