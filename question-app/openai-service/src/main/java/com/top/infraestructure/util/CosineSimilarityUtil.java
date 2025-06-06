package com.top.infraestructure.util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Utilidad para calcular la similitud coseno entre dos vectores de tipo float[].
 * La similitud coseno se define como:
 *
 *    cos(θ) = (A · B) / (||A|| * ||B||)
 *
 * donde “·” es el producto punto y ||A|| es la norma (magnitud) del vector A.
 */

@Service
@Slf4j
public class CosineSimilarityUtil {

    /**
     * Calcula la similitud coseno entre dos vectores. Ambos deben tener la misma longitud.
     *
     * @param vectorA Primer vector de floats.
     * @param vectorB Segundo vector de floats.
     * @return Un valor entre -1 y 1 (en la práctica para embeddings de texto suele ser ≥ 0).
     * @throws IllegalArgumentException Si los vectores tienen distinta longitud o son nulos.
     */
    public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null) {
            throw new IllegalArgumentException("Ninguno de los vectores puede ser null");
        }
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Los vectores deben tener la misma longitud");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += (double) vectorA[i] * vectorB[i];
            normA += (double) vectorA[i] * vectorA[i];
            normB += (double) vectorB[i] * vectorB[i];
        }

        // En caso de norma cero (vector todo 0), devolvemos similitud 0 para evitar división por 0
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

}