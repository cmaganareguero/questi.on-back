package com.top.application.enumeration;

public enum AnswerType {
    VERDADERO_FALSO("Verdadero/Falso"),
    CUATRO_RESPUESTAS("4 respuestas");

    private final String value;

    AnswerType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AnswerType fromString(String value) {
        for (AnswerType type : AnswerType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid answer type: " + value);
    }
}