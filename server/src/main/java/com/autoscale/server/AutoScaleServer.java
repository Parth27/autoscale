package com.autoscale.server;

import java.util.HashMap;

import java.net.Socket;
import java.net.ServerSocket;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class AutoScaleServer {
    static HashMap<Integer, ClientHandler> clientMap = new HashMap<>();
    static HashMap<Integer, Thread> threadMap = new HashMap<>();
    int port;

    public AutoScaleServer(int port) {
        this.port = port;
    }
    public void run() throws IOException {
        int id = 0; // counter for clients
        try (ServerSocket server = new ServerSocket(port)) {
            Socket socket;
            System.out.print("Started server");
            while (id < 100) {
                socket = server.accept();
                System.out.print("New client received: "+socket);
                
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                ClientHandler client = new ClientHandler(id,dis,dos);
                Thread t = new Thread(client);

                System.out.println("Client id = "+client.id);
                clientMap.put(id, client);
                t.start();
                id++;
            }
        }
    }
    public static void terminateClient(int id) {
        ClientHandler client = clientMap.get(id);
        Thread t = threadMap.get(id);
        client.terminate();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        clientMap.remove(id);
        threadMap.remove(id);
    }

    public static void main(String[] args) throws IOException {
        AutoScaleServer server = new AutoScaleServer(1234);
        server.run();
        for (int key:clientMap.keySet()) {
            terminateClient(key);
        }
    }
}