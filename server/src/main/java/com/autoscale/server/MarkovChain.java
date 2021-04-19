package com.autoscale.server;

import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import com.autoscale.config.MarkovChainConfig;

public class MarkovChain {
    HashMap<String, int[]> probabilities;
    float[][] matrix;

    public MarkovChain() {
        probabilities = new HashMap<>();
        matrix = new float[MarkovChainConfig.NUM_BINS][MarkovChainConfig.NUM_BINS];
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
        for (int i=1; i < MarkovChainConfig.WINDOW; i++) {
            chain.set(i-1,chain.get(i));
        }
        chain.set(MarkovChainConfig.WINDOW-1,String.valueOf(value));
    }
    // public void train(int[] data) {
    //     String[] chain = new String[WINDOW];
    //     for (int i = 0; i < data.length; i++) {
    //         if (i < WINDOW) {
    //             chain[i] = String.valueOf(data[i]);
    //             continue;
    //         }
    //         updateMatrix(chain, data[i]);
    //         updateChain(chain, data[i]);
    //     }
    // }
    public int predict(List<String> chain) {
        String key = String.join(",",chain);
        if (!probabilities.containsKey(key)) {
            return 0;
        }
        return argmax(probabilities.get(key));
    }
    public void updateMatrix(List<String> chain, int value) {
        String key = String.join(",",chain);
        probabilities.computeIfAbsent(key, s -> new int[MarkovChainConfig.NUM_BINS]);
        int[] counts = probabilities.get(key);
        counts[value]++;
        probabilities.put(key, counts);
    }

    public static void main(String[] args) {
        MarkovChain mc = new MarkovChain();
        Random rand = new Random(0);
        Random rand2 = new Random(0);
        int[] data = new int[10000];
        int[] test = new int[100];
        int[] predictions = new int[100];
        for (int i=0; i < 10000; i++) {
            data[i] = rand.nextInt(10);
        }
        for (int i=0; i < 100; i++) {
            test[i] = rand2.nextInt(10);
        }
        // mc.train(data);
        for (String key: mc.probabilities.keySet()) {
            System.out.println("\n"+key);
            for (int i:mc.probabilities.get(key)) {
                System.out.print("\t"+i);
            }
        }
        String[] chain = new String[MarkovChainConfig.WINDOW];
        for (int i=0; i < 100; i++) {
            if (i < MarkovChainConfig.WINDOW) {
                predictions[i] = 0;
                chain[i] = String.valueOf(data[i]);
                continue;
            }
            // predictions[i] = mc.predict(chain);
            // mc.updateMatrix(chain, test[i]);
            // mc.updateChain(chain, test[i]);
        }
        for (int i = 0; i < predictions.length; i++) {
            System.out.println(predictions[i]+"\t"+test[i]);
        }
    }
}
