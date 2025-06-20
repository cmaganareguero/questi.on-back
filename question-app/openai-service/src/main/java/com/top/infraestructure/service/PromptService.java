package com.top.infraestructure.service;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class PromptService {
    public String buildPromptTemplate(
            String category,
            String difficulty,
            String answerType,
            int numQuestions,
            String runId
    ) {
        if ("CUATRO_RESPUESTAS".equals(answerType)) {
            String[] templates = new String[] {
                    "/* RUN: %s */ Genera exactamente %d preguntas de opción múltiple sobre \"%s\" "
                            + "de (dificultad \"%s\"). Devuélvelas ÚNICAMENTE como un array JSON puro, "
                            + "sin ningún texto antes ni después, con este formato:\n"
                            + "[\n"
                            + "  {\"pregunta\": \"\", \"opciones\": [\"\",\"\",\"\",\"\"], \"indice_correcto\": 0},\n"
                            + "  {\"pregunta\": \"\", \"opciones\": [\"\",\"\",\"\",\"\"], \"indice_correcto\": 0},\n"
                            + "  …\n"
                            + "]",
                    "/* ID: %s */ Por favor, crea exactamente %d preguntas de opción múltiple en la categoría \"%s\" "
                            + "(dificultad \"%s\"). El resultado debe ser sólo un array JSON puro, con los objetos en este formato:\n"
                            + "[ {\"pregunta\":\"\",\"opciones\":[\"\",\"\",\"\",\"\"],\"indice_correcto\":0}, … ] "
                            + "No incluyas ningún texto ni explicación extra.",
                    "/* TAG: %s */ Necesito un array JSON puro que contenga %d preguntas de opción múltiple "
                            + "sobre \"%s\" (nivel \"%s\"). Cada elemento debe ser un objeto con los campos exactos:\n"
                            + "  \"pregunta\" (String), \"opciones\" (Array de 4 strings), \"indice_correcto\" (entero 0–3).\n"
                            + "Devuélvelo SIN texto adicional ni envoltura; sólo el array JSON cerrado con corchetes."
            };
            String tpl = templates[new Random().nextInt(templates.length)];
            return String.format(tpl, runId, numQuestions, category, difficulty);

        } else if ("VERDADERO_FALSO".equals(answerType)) {
            String[] templates = new String[] {
                    "/* RUN: %s */ Genera exactamente %d preguntas de Verdadero/Falso sobre \"%s\" "
                            + "(dificultad \"%s\"). Devuélvelas ÚNICAMENTE como un array JSON puro, "
                            + "sin ningún texto antes ni después, con este formato:\n"
                            + "[\n"
                            + "  {\"pregunta\": \"\", \"opciones\": [\"Verdadero\",\"Falso\"], \"indice_correcto\": 0},\n"
                            + "  {\"pregunta\": \"\", \"opciones\": [\"Verdadero\",\"Falso\"], \"indice_correcto\": 1},\n"
                            + "  …\n"
                            + "]",
                    "/* ID: %s */ Por favor, crea exactamente %d preguntas de Verdadero/Falso en la categoría \"%s\" "
                            + "(dificultad \"%s\"). El resultado debe ser sólo un array JSON puro, con los objetos en este formato:\n"
                            + "[ {\"pregunta\":\"\",\"opciones\":[\"Verdadero\",\"Falso\"],\"indice_correcto\":0}, … ] "
                            + "No incluyas ningún texto ni explicación extra.",
                    "/* TAG: %s */ Necesito un array JSON puro que contenga %d preguntas de Verdadero/Falso "
                            + "sobre \"%s\" (nivel \"%s\"). Cada elemento debe ser un objeto con los campos exactos:\n"
                            + "  \"pregunta\" (String), \"opciones\" (Array [\"Verdadero\",\"Falso\"]), \"indice_correcto\" (0 o 1).\n"
                            + "Devuélvelo SIN texto adicional ni envoltura; sólo el array JSON cerrado con corchetes."
            };
            String tpl = templates[new Random().nextInt(templates.length)];
            return String.format(tpl, runId, numQuestions, category, difficulty);

        } else {
            throw new IllegalArgumentException("Tipo de respuesta no soportado: " + answerType);
        }
    }
}
