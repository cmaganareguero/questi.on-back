package com.top.infraestructure.util;

import java.util.List;

public class SimpleCosineIndex {
    private final List<float[]> vectors;

    public SimpleCosineIndex(List<float[]> initialVectors) {
        this.vectors = new java.util.ArrayList<>(initialVectors);
    }

    public void addVector(float[] vec) {
        this.vectors.add(vec);
    }

    public boolean hasSimilar(float[] query, double threshold) {
        for (float[] hist : vectors) {
            double sim = CosineSimilarityUtil.cosineSimilarity(query, hist);
            if (sim >= threshold) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSimilar(List<float[]> vectors, float[] query, double threshold) {
        for (float[] hist : vectors) {
            double sim = CosineSimilarityUtil.cosineSimilarity(query, hist);
            if (sim >= threshold) {
                return true;
            }
        }
        return false;
    }
}