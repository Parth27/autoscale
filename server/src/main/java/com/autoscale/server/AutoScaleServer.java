package com.autoscale.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.autoscale.config.AutoScaleConfig;
import com.autoscale.config.MarkovChainConfig;

public class AutoScaleServer extends Thread {
    HashMap<Integer, ClientHandler> clientMap = new HashMap<>();
    List<Integer> memoryData = new ArrayList<>();
    List<Integer> diskData = new ArrayList<>();
    HashMap<Integer, Thread> threadMap = new HashMap<>();
    private volatile boolean running = true;
    ServerSocket server;
    int serverID = 0; // counter for clients
    List<String> chain;
    MarkovChain model;
    boolean trainMode;
    List<String> serverList;

    public AutoScaleServer(boolean trainMode) throws IOException {
        server = new ServerSocket(AutoScaleConfig.PORT);
        this.trainMode = trainMode;
        System.out.println("Started server on port " + AutoScaleConfig.PORT);
        model = new MarkovChain();
        chain = new ArrayList<>();
        serverList = new ArrayList<>();
    }

    public void terminate() throws IOException {
        System.out.println("Saving model....");

        try (FileOutputStream fStream = new FileOutputStream("/home/parth/autoscale/probabilities.txt");
                ObjectOutputStream oOutputStream = new ObjectOutputStream(fStream)) {
            oOutputStream.writeObject(model.probabilities);
        }
        server.close();
    }

    private void loadModel(String filepath) throws IOException, ClassNotFoundException {
        try (FileInputStream fStream = new FileInputStream(filepath);
                ObjectInputStream oInputStream = new ObjectInputStream(fStream)) {
            model.probabilities = (HashMap<String, int[]>) oInputStream.readObject();
        }
    }

    public void initialize(int numServers) throws IOException, InterruptedException {
        String[] script = { "sh", AutoScaleConfig.STARTUP_SCRIPT };
        Socket socket;
        for (int i = 0; i < numServers; i++) {
            Process p = Runtime.getRuntime().exec(script);
            p.waitFor();
            socket = server.accept();
            String ip = socket.getRemoteSocketAddress().toString();
            System.out.println("New client received: " + socket);
            System.out.println("IP: " + ip);

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            ClientHandler client = new ClientHandler(serverID, dis, dos, this, ip);
            Thread t = new Thread(client);

            System.out.println("Kafka server started, id = " + client.id);
            clientMap.put(serverID, client);
            threadMap.put(serverID, t);
            serverID++;
            socket.close();
            serverList.add(ip + ":9092");
        }
        InetAddress producerIP = InetAddress.getByName(AutoScaleConfig.PRODUCER_IP);
        try (Socket producerSocket = new Socket(producerIP, AutoScaleConfig.PRODUCER_PORT)) {
            DataOutputStream dos = new DataOutputStream(producerSocket.getOutputStream());
            dos.writeUTF(String.join(",", serverList));
        }
        for (Thread t : threadMap.values()) {
            t.start();
        }
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
                synchronized (this) {
                    int readyClients = 0;
                    while (readyClients < serverID) {
                        wait();
                        readyClients++;
                    }
                }
                int maxResourceVal = 0;
                for (ClientHandler client : clientMap.values()) {
                    maxResourceVal = Math.max(maxResourceVal, client.diskUsage);
                }
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
            synchronized (this) {
                int readyClients = 0;
                while (readyClients < serverID) {
                    wait();
                    readyClients++;
                }
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

    public void terminateClient(int id) throws IOException {
        System.out.println("Stoping servers...");
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
        @Override
        public void run() {
            try {
                for (int key : clientMap.keySet()) {
                    terminateClient(key);
                }
                terminate();
            } catch (IOException e) {
                // Do nothing
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