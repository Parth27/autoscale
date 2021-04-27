package com.autoscale.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.autoscale.application.kafka.KafkaBroker;
import com.autoscale.application.kafka.KafkaMetaServer;
import com.autoscale.config.AutoScaleConfig;
import com.autoscale.config.MarkovChainConfig;
import com.autoscale.core.ApplicationServer;
import com.autoscale.core.MetaServer;

public class AutoScaleServer extends Thread {
    ConcurrentHashMap<Integer, ClientHandler> clientMap = new ConcurrentHashMap<>();
    List<Integer> memoryData = new ArrayList<>();
    List<Integer> diskData = new ArrayList<>();
    ConcurrentHashMap<Integer, Thread> threadMap = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    ServerSocket server;
    int serverID = 0; // counter for clients
    List<String> chain;
    MarkovChain model;
    boolean trainMode;
    List<String> serverList;
    ConcurrentHashMap<Integer, Boolean> clientMonitor;
    MetaServer metaServer;

    public AutoScaleServer(boolean trainMode) throws IOException {
        server = new ServerSocket(AutoScaleConfig.PORT);
        this.trainMode = trainMode;
        System.out.println("Started server on port " + AutoScaleConfig.PORT);
        model = new MarkovChain();
        chain = new ArrayList<>();
        serverList = new ArrayList<>();
        clientMonitor = new ConcurrentHashMap<>();
        metaServer = new KafkaMetaServer(); // Either remove or replace with your application's implementation
                                            // of MetaServer
    }

    public void startServers(int numServers) throws IOException, InterruptedException {
        String[] script = { "sh", AutoScaleConfig.INSTANCE_START };
        for (int i = 0; i < numServers; i++) {
            Process p = new ProcessBuilder(script).start();
            BufferedReader processInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String instanceID = processInput.readLine();
            String ip = processInput.readLine();
            System.out.println("AWS EC2 Instance created with ID: " + instanceID);
            p.waitFor();
            System.out.println("IP: " + ip);
            ApplicationServer applicationServer = new KafkaBroker(ip, instanceID); // Replace with your application's
                                                                                   // implementation of
                                                                                   // ApplicationServer

            ClientHandler client = new ClientHandler(serverID, this, ip, instanceID, server, applicationServer);
            Thread t = new Thread(client);

            System.out.println("Kafka server started, id = " + client.id);
            clientMap.put(serverID, client);
            threadMap.put(serverID, t);
            clientMonitor.put(serverID, true);
            serverID++;
            serverList.add(ip + ":9092");
        }
        for (int id : clientMap.keySet()) {
            ApplicationServer applicationServer = clientMap.get(id).applicationServer;
            applicationServer.initialize();
            threadMap.get(id).start();
        }
        syncServers();
        metaServer.run(serverList);
    }

    public void stopServers(int numServers) throws InterruptedException {
        for (int i = 0; i < numServers; i++) {
            int id = serverID - 1;
            terminateClient(id);
            serverList.remove(serverList.size() - 1);
            serverID--;
        }
        syncServers();
        metaServer.run(serverList);
    }

    @Override
    public void run() {
        int offset = 100 / MarkovChainConfig.NUM_BINS;
        try {
            if (!trainMode) {
                inference();
                return;
            }
            // For training the model
            while (running) {
                while (clientMonitor.size() > 0) {
                    // Do nothing
                }
                System.out.println("Num clients ready: " + (serverID - clientMonitor.size()));
                for (int id : clientMap.keySet()) {
                    clientMonitor.put(id, true);
                }
                int maxResourceVal = 0;
                for (ClientHandler client : clientMap.values()) {
                    maxResourceVal = Math.max(maxResourceVal, client.diskUsage);
                }
                System.out.println("Max Disk Usage: " + maxResourceVal * offset + "%");
                if (chain.size() == MarkovChainConfig.WINDOW) {
                    model.updateMatrix(chain, maxResourceVal);
                }
                model.updateChain(chain, maxResourceVal);
            }
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private void syncServers() throws InterruptedException {
        Thread.sleep(10*1000); // Sleep until all servers are ready
        // for (int id : clientMap.keySet()) {
        //     ApplicationServer applicationServer = clientMap.get(id).applicationServer;
        //     while (!applicationServer.isStarted) {
        //         // Wait
        //     }
        // }
    }

    private void saveModel(String filepath) throws IOException {
        try (FileOutputStream fStream = new FileOutputStream(filepath);
                ObjectOutputStream oOutputStream = new ObjectOutputStream(fStream)) {
            oOutputStream.writeObject(model.probabilities);
        }
    }

    private void loadModel(String filepath) throws IOException, ClassNotFoundException {
        try (FileInputStream fStream = new FileInputStream(filepath);
                ObjectInputStream oInputStream = new ObjectInputStream(fStream)) {
            model.probabilities = (HashMap<String, int[]>) oInputStream.readObject();
        }
    }

    private void terminate() throws IOException {
        System.out.println("Saving model....");
        saveModel("/home/parth/autoscale/probabilities.txt");
        server.close();
    }

    private void terminateClient(int id) throws InterruptedException {
        ClientHandler client = clientMap.get(id);
        Thread t = threadMap.get(id);
        client.applicationServer.terminate();
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clientMap.remove(id);
        threadMap.remove(id);
    }

    private void inference() throws IOException, InterruptedException, ClassNotFoundException {
        // During final inference
        // Scales if necessary based on markov chain prediction
        loadModel(AutoScaleConfig.MODEL_FILE);
        int offset = 100 / MarkovChainConfig.NUM_BINS;

        while (running) {
            while (clientMonitor.size() > 0) {
                // Wait
            }
            System.out.println("Num clients ready: " + (serverID - clientMonitor.size()));
            for (int id : clientMap.keySet()) {
                clientMonitor.put(id, true);
            }
            int maxResourceVal = 0;
            for (ClientHandler client : clientMap.values()) {
                maxResourceVal = Math.max(maxResourceVal, client.diskUsage);
            }
            if (chain.size() == MarkovChainConfig.WINDOW) {
                model.updateMatrix(chain, maxResourceVal);
            }
            model.updateChain(chain, maxResourceVal);
            maxResourceVal = Math.max(maxResourceVal, model.predict(chain));
            if (maxResourceVal >= (AutoScaleConfig.UPPER_LIMIT / offset)) {
                startServers(1);
                chain.clear();
            } else if (maxResourceVal < (AutoScaleConfig.LOWER_LIMIT / offset)) {
                stopServers(1);
                chain.clear();
            }
        }
    }

    class ServerStop extends Thread {
        @Override
        public void run() {
            try {
                System.out.println("Stoping servers...");
                for (int key : clientMap.keySet()) {
                    terminateClient(key);
                }
                terminate();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean trainMode = true;
        if (args.length == 1 && !args[0].equals("train")) {
            trainMode = false;
        }
        AutoScaleServer server = new AutoScaleServer(trainMode);
        Runtime.getRuntime().addShutdownHook(server.new ServerStop());
        server.startServers(AutoScaleConfig.STARTUP_SERVERS);
        server.start();
    }
}