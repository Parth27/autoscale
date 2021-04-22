package com.autoscale.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.autoscale.config.AutoScaleConfig;
import com.autoscale.config.MarkovChainConfig;

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

    public AutoScaleServer(boolean trainMode) throws IOException {
        server = new ServerSocket(AutoScaleConfig.PORT);
        this.trainMode = trainMode;
        System.out.println("Started server on port " + AutoScaleConfig.PORT);
        model = new MarkovChain();
        chain = new ArrayList<>();
        serverList = new ArrayList<>();
        clientMonitor = new ConcurrentHashMap<>();
    }

    private void loadModel(String filepath) throws IOException, ClassNotFoundException {
        try (FileInputStream fStream = new FileInputStream(filepath);
                ObjectInputStream oInputStream = new ObjectInputStream(fStream)) {
            model.probabilities = (HashMap<String, int[]>) oInputStream.readObject();
        }
    }

    private void startServers() throws IOException, InterruptedException {
        String[] script;
        for (int id : clientMap.keySet()) {
            ClientHandler client = clientMap.get(id);
            script = new String[]{ "sh", AutoScaleConfig.SERVER_START, client.ip };
            try {
                System.out.println(client.ip);
                Process p = new ProcessBuilder(script).start();
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
            threadMap.get(id).start();
        }
    }

    private void createTopic() throws IOException {
        String[] script = { "sh", AutoScaleConfig.TOPIC_SCRIPT, AutoScaleConfig.ZOOKEEPER_IP };
        try {
            Process p = new ProcessBuilder(script).start();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pingProducer() throws IOException {
        InetAddress producerIP = InetAddress.getByName(AutoScaleConfig.PRODUCER_IP);
        try (Socket producerSocket = new Socket(producerIP, AutoScaleConfig.PRODUCER_PORT)) {
            DataOutputStream dos = new DataOutputStream(producerSocket.getOutputStream());
            dos.writeUTF(String.join(",", serverList));
        }
    }

    public void initialize(int numServers) throws IOException, InterruptedException {
        String[] script = { "sh", AutoScaleConfig.INSTANCE_START };
        for (int i = 0; i < numServers; i++) {
            Process p = new ProcessBuilder(script).start();;
            BufferedReader processInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String instanceID = processInput.readLine();
            String ip = processInput.readLine();
            System.out.println("AWS EC2 Instance created with ID: "+instanceID);
            p.waitFor();
            // String ip = socket.getRemoteSocketAddress().toString().split("/")[1];
            System.out.println("IP: " + ip);

            ClientHandler client = new ClientHandler(serverID, this, ip, instanceID, server);
            Thread t = new Thread(client);

            System.out.println("Kafka server started, id = " + client.id);
            clientMap.put(serverID, client);
            threadMap.put(serverID, t);
            clientMonitor.put(serverID,true);
            serverID++;
            serverList.add(ip + ":9092");
        }
        startServers();
        createTopic();
        System.out.println("Topic Messages created...");
        // Interrupt the producer to update its server list
        pingProducer();
    }

    @Override
    public void run() {
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
                System.out.println("Num clients ready: "+(serverID - clientMonitor.size()));
                for (int id : clientMap.keySet()) {
                    clientMonitor.put(id, true);
                }
                int maxResourceVal = 0;
                for (ClientHandler client : clientMap.values()) {
                    maxResourceVal = Math.max(maxResourceVal, client.diskUsage);
                }
                System.out.println("Max Disk Usage: "+maxResourceVal);
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

    public void inference() throws IOException, InterruptedException, ClassNotFoundException {
        // During final inference
        // Scales if necessary based on markov chain prediction
        loadModel(AutoScaleConfig.MODEL_FILE);

        while (running) {
            while (clientMonitor.size() > 0) {
                // Do nothing
            }
            System.out.println("Num clients ready: "+(serverID - clientMonitor.size()));
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
            if (maxResourceVal > AutoScaleConfig.UPPER_LIMIT) {
                initialize(1);
                chain.clear();
            }
        }
    }

    public void terminateClient(int id) throws IOException, InterruptedException {
        ClientHandler client = clientMap.get(id);
        Thread t = threadMap.get(id);
        client.terminate();
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clientMap.remove(id);
        threadMap.remove(id);
    }

    class ServerStop extends Thread {
        private void terminate() throws IOException {
            System.out.println("Saving model....");
    
            try (FileOutputStream fStream = new FileOutputStream("/home/parth/autoscale/probabilities.txt");
                    ObjectOutputStream oOutputStream = new ObjectOutputStream(fStream)) {
                oOutputStream.writeObject(model.probabilities);
            }
            server.close();
        }

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
        server.initialize(AutoScaleConfig.STARTUP_SERVERS);
        server.start();
    }
}