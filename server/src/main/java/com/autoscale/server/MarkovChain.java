package com.autoscale.server;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.autoscale.config.MarkovChainConfig;

public class MarkovChain {
    HashMap<String, int[]> probabilities;
    float[][] matrix;

    public MarkovChain() {
        probabilities = new HashMap<>();
    }

    private int argmax(int[] arr) {
        int max = arr[0];
        int idx = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                idx = i;
            }
        }
        return idx;
    }

    public void updateChain(List<String> chain, int value) {
        if (chain.size() < MarkovChainConfig.WINDOW) {
            chain.add(String.valueOf(value));
            return;
        }
        for (int i = 1; i < MarkovChainConfig.WINDOW; i++) {
            chain.set(i - 1, chain.get(i));
        }
        chain.set(MarkovChainConfig.WINDOW - 1, String.valueOf(value));
    }

    public int predict(List<String> chain) {
        String key = String.join(",", chain);
        if (!probabilities.containsKey(key)) {
            return Integer.valueOf(chain.get(chain.size()-1));
        }
        return argmax(probabilities.get(key));
    }

    public void updateWeights(List<String> chain, int value) {
        String key = String.join(",", chain);
        probabilities.computeIfAbsent(key, s -> new int[MarkovChainConfig.NUM_BINS+1]);
        int[] counts = probabilities.get(key);
        counts[value]++;
        probabilities.put(key, counts);
    }
}
