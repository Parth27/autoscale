package com.autoscale.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import com.autoscale.config.AutoScaleConfig;
import com.autoscale.config.MarkovChainConfig;

public class ClientHandler implements Runnable {
    DataInputStream dis;
    DataOutputStream dos;
    final int id;
    private volatile boolean running = true;
    int diskUsage;
    int memoryUsage;
    AutoScaleServer parent;
    String ip;
    String instanceID;
    Socket socket;
    ServerSocket autoscaleServer;

    public ClientHandler(int id, AutoScaleServer parent, String ip, String instanceID, ServerSocket server) {
        this.id = id;
        this.parent = parent;
        this.ip = ip;
        this.instanceID = instanceID;
        autoscaleServer = server;
    }

    private void initialize() throws IOException {
        socket = autoscaleServer.accept();
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    public void terminate() throws IOException {
        String[] script = { "sh", AutoScaleConfig.SERVER_TERMINATE, ip, instanceID };
        new ProcessBuilder(script).start();
    }

    @Override
    public void run() {
        try {
            initialize();
            while (running) {
                String[] input = dis.readUTF().split(",");
                diskUsage = (int) Math.round(Double.parseDouble(input[0]) / MarkovChainConfig.NUM_BINS);
                memoryUsage = (int) Math.round(Double.parseDouble(input[1]) / MarkovChainConfig.NUM_BINS);
                System.out.println(input[0] + "," + input[1]);
                if (parent.clientMonitor.containsKey(id)) {
                    parent.clientMonitor.remove(id);
                }
            }
            this.dis.close();
            this.dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
