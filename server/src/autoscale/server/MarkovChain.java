package autoscale.server;

import java.util.Arrays;
import java.util.Random;
import java.util.HashMap;

public class MarkovChain {
    final int WINDOW;
    final int NUM_BINS;
    HashMap<String, int[]> map;
    float[][] matrix;

    public MarkovChain(int window_size, int num_bins) {
        WINDOW = window_size;
        NUM_BINS = num_bins;
        map = new HashMap<>();
        matrix = new float[NUM_BINS][NUM_BINS];
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
    private void updateChain(String[] chain, int value) {
        for (int i=1; i < WINDOW; i++) {
            chain[i-1] = chain[i];
        }
        chain[WINDOW-1] = String.valueOf(value);
    }
    public void train(int[] data) {
        String[] chain = new String[WINDOW];
        for (int i = 0; i < data.length; i++) {
            if (i < WINDOW) {
                chain[i] = String.valueOf(data[i]);
                continue;
            }
            updateMatrix(chain, data[i]);
            updateChain(chain, data[i]);
        }
    }
    public int predict(String[] chain) {
        String key = String.join(",",chain);
        if (!map.containsKey(key)) {
            return 0;
        }
        return argmax(map.get(key));
    }
    private void updateMatrix(String[] chain, int value) {
        String key = String.join(",",chain);
        map.computeIfAbsent(key, s -> new int[NUM_BINS]);
        int[] counts = map.get(key);
        counts[value]++;
        map.put(key, counts);
    }

    public static void main(String[] args) {
        MarkovChain mc = new MarkovChain(2, 10);
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
        mc.train(data);
        for (String key: mc.map.keySet()) {
            System.out.println("\n"+key);
            for (int i:mc.map.get(key)) {
                System.out.print("\t"+i);
            }
        }
        String[] chain = new String[mc.WINDOW];
        for (int i=0; i < 100; i++) {
            if (i < mc.WINDOW) {
                predictions[i] = 0;
                chain[i] = String.valueOf(data[i]);
                continue;
            }
            predictions[i] = mc.predict(chain);
            mc.updateMatrix(chain, test[i]);
            mc.updateChain(chain, test[i]);
        }
        for (int i = 0; i < predictions.length; i++) {
            System.out.println(predictions[i]+"\t"+test[i]);
        }
    }
}
